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
 */
package org.sosy_lab.cpachecker.cpa.multigoal;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.location.WeavingType;
import org.sosy_lab.cpachecker.cpa.location.WeavingVariable;
import org.sosy_lab.cpachecker.util.Pair;

public class VariablesToWeave {
  ImmutableSet<Pair<WeavingVariable, WeavingType>> vars;

  VariablesToWeave(ImmutableSet<Pair<WeavingVariable, WeavingType>> pVars) {
    vars = pVars;
  }

  VariablesToWeave(Set<Pair<WeavingVariable, WeavingType>> pVars) {
    vars = ImmutableSet.copyOf(pVars);
  }

  public ImmutableSet<Pair<WeavingVariable, WeavingType>> getMap() {
    return vars;
  }

  @Override
  public int hashCode() {
    // TODO Auto-generated method stub
    return super.hashCode();
  }

}