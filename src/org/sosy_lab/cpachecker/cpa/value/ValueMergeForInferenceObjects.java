/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value;

import org.sosy_lab.cpachecker.core.defaults.EmptyInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;


public class ValueMergeForInferenceObjects implements MergeOperator {

  public final boolean abstractionMerge;

  public ValueMergeForInferenceObjects(boolean pFlag) {
    abstractionMerge = pFlag;
  }

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision) throws CPAException, InterruptedException {
    if (pState1 == EmptyInferenceObject.getInstance() || pState2 == EmptyInferenceObject.getInstance()) {
      return pState2;
    }

    ValueInferenceObject object1 = (ValueInferenceObject) pState1;
    ValueInferenceObject object2 = (ValueInferenceObject) pState2;

    if (abstractionMerge) {
      ValueAnalysisState state1 = object1.getSource();
      ValueAnalysisState state2 = object2.getSource();
      if (!state1.equals(state2)) {
        return pState2;
      }
    }
    ValueInferenceObject result = object1.merge(object2);

    if (result.equals(object2)) {
      return pState2;
    } else {
      return result;
    }
  }

}
