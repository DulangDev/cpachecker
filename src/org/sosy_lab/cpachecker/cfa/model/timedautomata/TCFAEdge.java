/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.model.timedautomata;

import com.google.common.base.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableCondition;
import org.sosy_lab.cpachecker.cfa.model.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class TCFAEdge extends AbstractCFAEdge {

  private final Set<CIdExpression> variablesToReset;
  private final Optional<TaVariableCondition> guard;
  private final Optional<String> action;

  private static final long serialVersionUID = 5472749446453717391L;

  public TCFAEdge(
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      Optional<TaVariableCondition> pGuard,
      Set<CIdExpression> pResetStatements,
      Optional<String> pAction) {
    super("", pFileLocation, pPredecessor, pSuccessor);
    variablesToReset = pResetStatements;
    guard = pGuard;
    action = pAction;
  }

  public Optional<String> getAction() {
    return action;
  }

  public Set<CIdExpression> getVariablesToReset() {
    return variablesToReset;
  }

  public Optional<TaVariableCondition> getGuard() {
    return guard;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.TimedAutomatonEdge;
  }

  @Override
  public String getCode() {
    return "";
  }
}
