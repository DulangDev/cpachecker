/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula;

import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.util.predicates.smt.FloatingPointFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FloatingPointFormula;

class JSNumberFormulaManager {
  private final boolean useNaN;
  private final BooleanFormulaManager bfmgr;
  private final FloatingPointFormulaManagerView fpfmgr;
  final FloatingPointFormula NaN;
  final FloatingPointFormula NEGATIVE_INFINITY;
  final FloatingPointFormula ZERO;
  final FloatingPointFormula NEGATIVE_ZERO;
  final FloatingPointFormula ONE;

  JSNumberFormulaManager(
      final boolean pUseNaN,
      final BooleanFormulaManager pBfmgr,
      final FloatingPointFormulaManagerView pFpfmgr) {
    useNaN = pUseNaN;
    bfmgr = pBfmgr;
    fpfmgr = pFpfmgr;
    NaN = fpfmgr.makeNaN(Types.NUMBER_TYPE);
    NEGATIVE_INFINITY = fpfmgr.makeMinusInfinity(Types.NUMBER_TYPE);
    ZERO = makeNumber(0);
    NEGATIVE_ZERO = fpfmgr.negate(ZERO);
    ONE = makeNumber(1);
  }

  @Nonnull
  FloatingPointFormula makeNumber(final double pValue) {
    return fpfmgr.makeNumber(pValue, Types.NUMBER_TYPE);
  }

  BooleanFormula isNaN(final FloatingPointFormula pNumber) {
    return useNaN ? fpfmgr.isNaN(pNumber) : bfmgr.makeFalse();
  }

  BooleanFormula isInfinity(final FloatingPointFormula pNumber) {
    // TODO use another option or rename useNaN to be not misleading
    return useNaN ? fpfmgr.isInfinity(pNumber) : bfmgr.makeFalse();
  }

  BooleanFormula isNegative(final FloatingPointFormula pNumber) {
    return fpfmgr.isNegative(pNumber);
  }

  BooleanFormula isNegativeZero(final FloatingPointFormula pNumber) {
    // TODO use another option or rename useNaN to be not misleading
    return useNaN
        ? bfmgr.and(fpfmgr.isZero(pNumber), fpfmgr.isNegative(pNumber))
        : bfmgr.makeFalse();
  }
}
