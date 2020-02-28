/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.usage.refinement;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.core.interfaces.AdjustablePrecision;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.util.Pair;


public class RefinementResult {
  public enum RefinementStatus {
    TRUE,
    FALSE
  }
  private final Map<Class<? extends RefinementInterface>, Object> auxiliaryInfo = new HashMap<>();
  private final Pair<UsageInfo, UsageInfo> trueRace;
  // Currently only predicate one, but in general case we may add other ones
  private Collection<AdjustablePrecision> precisions;
  RefinementStatus status;

  private RefinementResult(RefinementStatus rStatus, UsageInfo firstUsage, UsageInfo secondUsage) {
    status = rStatus;
    if (firstUsage != null && secondUsage != null) {
      //True result
      Preconditions.checkArgument(status == RefinementStatus.TRUE);
      trueRace = Pair.of(firstUsage, secondUsage);
    } else {
      //Other results
      trueRace = null;
    }
    precisions = new ArrayList<>();
  }

  public void addInfo(Class<? extends RefinementInterface> caller, Object info) {
    //Now used only for transferring precision
    assert !auxiliaryInfo.containsKey(caller);
    auxiliaryInfo.put(caller, info);
  }

  public Object getInfo(Class<? extends RefinementInterface> caller) {
    return auxiliaryInfo.get(caller);
  }

  public boolean isTrue() {
    return status == RefinementStatus.TRUE;
  }

  public boolean isFalse() {
    return status == RefinementStatus.FALSE;
  }

  public static RefinementResult createTrue(ExtendedARGPath firstPath, ExtendedARGPath secondPath) {

    UsageInfo firstUsage = firstPath.getUsageInfo();
    UsageInfo secondUsage = secondPath.getUsageInfo();

    if (firstUsage == secondUsage) {
      secondUsage = secondUsage.copy();
    }
    firstUsage.setRefinedPath(firstPath.getInnerEdges());
    secondUsage.setRefinedPath(secondPath.getInnerEdges());
    return new RefinementResult(RefinementStatus.TRUE, firstUsage, secondUsage);
  }

  public static RefinementResult createTrue() {
    //Used for temporary result
    return new RefinementResult(RefinementStatus.TRUE, null, null);
  }

  public static RefinementResult createFalse() {
    return new RefinementResult(RefinementStatus.FALSE, null, null);
  }

  public Pair<UsageInfo, UsageInfo> getTrueRace() {
    Preconditions.checkArgument(status == RefinementStatus.TRUE);
    return trueRace;
  }

  public void addPrecision(AdjustablePrecision p) {
    if (!p.isEmpty()) {
      precisions.add(p);
    }
  }

  public void addPrecisions(Iterable<AdjustablePrecision> pList) {
    for (AdjustablePrecision p : pList) {
      addPrecision(p);
    }
  }

  public Collection<AdjustablePrecision> getPrecisions() {
    return ImmutableList.copyOf(precisions);
  }

  @Override
  public String toString() {
    return status.name();
  }
}
