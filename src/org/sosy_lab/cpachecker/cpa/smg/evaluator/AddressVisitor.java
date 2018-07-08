/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.smg.evaluator;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.TypeUtils;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * This visitor is used to determine the address of an expression, mainly lValues.
 * It is used to prevent
 * code replication in other visitors who need this kind of functionality,
 * which is why its abstract.
 */
abstract class AddressVisitor extends DefaultCExpressionVisitor<List<SMGAddressAndState>, CPATransferException>
    implements CRightHandSideVisitor<List<SMGAddressAndState>, CPATransferException> {

  final SMGExpressionEvaluator smgExpressionEvaluator;
  private final CFAEdge cfaEdge;
  private final SMGState initialSmgState;

  AddressVisitor(
      SMGExpressionEvaluator pSmgExpressionEvaluator, CFAEdge pEdge, SMGState pSmgState) {
    smgExpressionEvaluator = Preconditions.checkNotNull(pSmgExpressionEvaluator);
    cfaEdge = pEdge;
    initialSmgState = pSmgState;
  }

  @Override
  protected List<SMGAddressAndState> visitDefault(CExpression pExp) {
    return Collections.singletonList(SMGAddressAndState.of(getInitialSmgState()));
  }

  List<SMGAddressAndState> visitDefault(@SuppressWarnings("unused") CRightHandSide rhs) {
    return Collections.singletonList(SMGAddressAndState.of(getInitialSmgState()));
  }

  @Override
  public List<SMGAddressAndState> visit(CIdExpression variableName) throws CPATransferException {

    SMGState state = getInitialSmgState();
    SMGObject object = state.getHeap().getObjectForVisibleVariable(variableName.getName());
    if (object != null) {
      state.addElementToCurrentChain(object);
    }

    if (object == null && variableName.getDeclaration() != null) {
      CSimpleDeclaration dcl = variableName.getDeclaration();
      if (dcl instanceof CVariableDeclaration) {
        CVariableDeclaration varDcl = (CVariableDeclaration) dcl;

        if (varDcl.isGlobal()) {
          object = state.addGlobalVariable(smgExpressionEvaluator.getBitSizeof(getCfaEdge(), varDcl.getType(), state),
              varDcl.getName());
        } else {
          Optional<SMGObject> addedLocalVariable =
              state.addLocalVariable(
                  smgExpressionEvaluator.getBitSizeof(getCfaEdge(), varDcl.getType(), state),
                  varDcl.getName());
          if (addedLocalVariable.isPresent()) {
            object = addedLocalVariable.get();
          } else {
            return Collections.singletonList(SMGAddressAndState.of(state, SMGAddress.UNKNOWN));
          }
        }
      }
    }

    return Collections.singletonList(
        SMGAddressAndState.of(state, SMGAddress.valueOf(object, SMGZeroValue.INSTANCE)));
  }

  @Override
  public List<SMGAddressAndState> visit(CArraySubscriptExpression exp) throws CPATransferException {
    return smgExpressionEvaluator.evaluateArraySubscriptAddress(getInitialSmgState(), getCfaEdge(), exp);
  }

  @Override
  public List<SMGAddressAndState> visit(CFieldReference pE) throws CPATransferException {
    return smgExpressionEvaluator.getAddressOfField(getInitialSmgState(), getCfaEdge(), pE);
  }

  @Override
  public List<SMGAddressAndState> visit(CPointerExpression pointerExpression)
      throws CPATransferException {

    /*
     * The address of a pointer expression (*x) is defined as the evaluation
     * of the pointer x. This is consistent with the meaning of a pointer
     * expression in the left hand side of an assignment *x = ...
     */
    CExpression operand = pointerExpression.getOperand();

    assert TypeUtils.getRealExpressionType(operand) instanceof CPointerType
        || TypeUtils.getRealExpressionType(operand) instanceof CArrayType;

    return asAddressAndStateList(
        smgExpressionEvaluator.evaluateAddress(getInitialSmgState(), getCfaEdge(), operand));
  }

  final CFAEdge getCfaEdge() {
    return cfaEdge;
  }

  final SMGState getInitialSmgState() {
    return initialSmgState;
  }

  static List<SMGAddressAndState> asAddressAndStateList(List<SMGAddressValueAndState> lst) {
    List<SMGAddressAndState> result = new ArrayList<>();
    for (SMGAddressValueAndState addressValueAndState : lst) {
      SMGAddressValue addressValue = addressValueAndState.getObject();
      result.add(
          SMGAddressAndState.of(
              addressValueAndState.getSmgState(),
              addressValue.isUnknown()
                  ? SMGAddress.getUnknownInstance()
                  : addressValue.getAddress()));
    }
    return result;
  }
}