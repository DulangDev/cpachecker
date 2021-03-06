// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;

public class BAMReducer implements Reducer {


  @Override
  public AbstractState getVariableReducedState(
      AbstractState expandedState, Block context, CFANode callNode) throws InterruptedException {
    BamState expandedBamState = (BamState)expandedState;
    return new BamState(CLangSMG.prepareForBAM(expandedBamState.getWrappedState()));
  }

  @Override
  public AbstractState
  getVariableExpandedState(AbstractState rootState, Block reducedContext, AbstractState reducedState) throws InterruptedException {
    BamState reducedBamState = (BamState)reducedState;
    CLangSMG baseState = ((BamState) rootState).getWrappedState();
    return (AbstractState)CLangSMG.expandBAM(reducedBamState.getWrappedState(), baseState);
  }

  @Override
  public Precision getVariableReducedPrecision(Precision precision, Block context) {
    return null;
  }

  @Override
  public Precision
  getVariableExpandedPrecision(Precision rootPrecision, Block rootContext, Precision reducedPrecision) {
    return null;
  }

  @Override
  public Object
  getHashCodeForState(AbstractState stateKey, Precision precisionKey) {
    return null;
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(
      AbstractState rootState,
      AbstractState entryState,
      AbstractState expandedState,
      FunctionExitNode exitLocation) {
    return null;
  }


}
