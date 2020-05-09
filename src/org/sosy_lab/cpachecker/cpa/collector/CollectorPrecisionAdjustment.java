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
package org.sosy_lab.cpachecker.cpa.collector;

import com.google.common.base.Function;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CollectorPrecisionAdjustment implements PrecisionAdjustment {


  private final LogManager logger;

  private final PrecisionAdjustment delegate;

  public CollectorPrecisionAdjustment(
      final PrecisionAdjustment pDelegateAdjustment,
      LogManager clogger) {

    delegate = pDelegateAdjustment;
    this.logger = clogger;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      final AbstractState pState,
      final Precision pPrecision,
      final UnmodifiableReachedSet pStates,
      final Function<AbstractState, AbstractState> pStateProjection,
      final AbstractState pFullState
  ) throws CPAException, InterruptedException {

    assert pState instanceof CollectorState;
    ARGState wrappedState2 = (ARGState) ((CollectorState) pState).getWrappedState();
    Optional<PrecisionAdjustmentResult> delegateResult =
        delegate.prec(Objects.requireNonNull(wrappedState2), pPrecision, pStates, pStateProjection,
            pFullState);

    if (delegateResult.isPresent()) {
      PrecisionAdjustmentResult unwrappedResult = delegateResult.get();
      AbstractState state = unwrappedResult.abstractState();
      Precision precision = unwrappedResult.precision();


     // CollectorState preAdj =
     //     new CollectorState(wrappedState2, null, null, false, null, null, null, logger);
     // Collection<AbstractState> wrappedAbstract = new ArrayList<>();
      //wrappedAbstract.add(preAdj);


      PrecisionAdjustmentResult finalResult;
      if (state != wrappedState2) {
        // something changed
        CollectorState preAdjchange =
            new CollectorState(state, null, null, false, null, null, null, logger);
      //  wrappedAbstract.add(preAdjchange);


        finalResult =
            PrecisionAdjustmentResult.create(
                preAdjchange,
                precision,
                unwrappedResult.action());

      } else { // nothing changed
        finalResult =
            PrecisionAdjustmentResult.create(pState, pPrecision, unwrappedResult.action());
      }

      return Optional.of(finalResult);

    } else {
      return delegateResult;
    }
  }
}
