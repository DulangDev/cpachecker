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
package org.sosy_lab.cpachecker.cpa.harness;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.concurrent.Immutable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;

@Immutable
public class PointerState {

  private final PersistentMap<MemoryLocation, MemoryLocation> pointsToMap;

  public PointerState() {
    pointsToMap = PathCopyingPersistentTreeMap.of();
  }

  public PointerState(PersistentMap<MemoryLocation, MemoryLocation> pPointsToMap) {
    pointsToMap = pPointsToMap;
  }

  public PointerState addPointer(MemoryLocation pPointerLocation) {
    return addPointer(pPointerLocation, null);
  }

  public PointerState addPointerIfNotExists(MemoryLocation pPointerLocation) {
    boolean pointerExists = (pointsToMap.get(pPointerLocation) != null);
    if (pointerExists) {
      return this;
    }
    return addPointerIfNotExists(pPointerLocation, null);
  }

  public PointerState addPointerIfNotExists(
      MemoryLocation pPointerLocation,
      MemoryLocation pPointerTargetLocation) {
    boolean pointerExists = (pointsToMap.get(pPointerLocation) != null);
    if (pointerExists) {
      return this;
    }
    return addPointer(pPointerLocation, pPointerTargetLocation);
  }

  public PointerState
      addPointer(MemoryLocation pPointerLocation, MemoryLocation pTargetLocation) {
    PersistentMap<MemoryLocation, MemoryLocation> newPointsToMap =
        pointsToMap.putAndCopy(pPointerLocation, pTargetLocation);
    return new PointerState(newPointsToMap);
  }

  public PointerState merge(
      MemoryLocation pLocation1,
      MemoryLocation pLocation2) {

    if (pLocation1.isPrecise() && pLocation2.isPrecise()) {
      return this;
    }

    MemoryLocation mergeTarget = pLocation1.isPrecise() ? pLocation1 : pLocation2;
    MemoryLocation mergeSource = mergeTarget == pLocation1 ? pLocation2 : pLocation1;

    // TODO: vergleiche Performance von komplett neuer map mit iterator, mit stream()/map Ansatz
    Map<MemoryLocation, MemoryLocation> newPointsToMap = new HashMap<>();
    Iterator<Map.Entry<MemoryLocation, MemoryLocation>> MapIterator =
        ((PathCopyingPersistentTreeMap<MemoryLocation, MemoryLocation>) pointsToMap)
            .entryIterator();
    while (MapIterator.hasNext()) {
      MemoryLocation newTargetLocation;
      Map.Entry<MemoryLocation, MemoryLocation> mapEntry = MapIterator.next();
      MemoryLocation mapValue = mapEntry.getValue();
      if (mapValue == mergeSource) {
        newTargetLocation = mergeTarget;
      } else {
        newTargetLocation = mapValue;
      }
      newPointsToMap.put(mapEntry.getKey(), newTargetLocation);
    }
    PersistentMap<MemoryLocation, MemoryLocation> persistentMap =
        PathCopyingPersistentTreeMap.copyOf(newPointsToMap);
    PointerState newPointers = new PointerState(persistentMap);
    return newPointers;
  }



  private Map<Boolean, List<Entry<MemoryLocation, MemoryLocation>>> partitionByTargetLocation(MemoryLocation pTargetLocation) {
    return pointsToMap.entrySet().stream()
    .collect(Collectors.partitioningBy(entry->entry.getKey() == pTargetLocation));
  }

  public MemoryLocation getTarget(MemoryLocation pKeyLocation) {
    return pointsToMap.get(pKeyLocation);
  }

  public MemoryLocation getTargetFromIdentifier(String pIdentifier) {
    MemoryLocation identifierLocation = fromIdentifier(pIdentifier);
    MemoryLocation identifierTargetLocation = getTarget(identifierLocation);
    return identifierTargetLocation;
  }

  public MemoryLocation fromIdentifier(String pIdentifier) {
    MemoryLocation result;
    Optional<MemoryLocation> existingLocation =
        pointsToMap.keySet()
        .stream()
            .filter(key -> key.getIdentifier() != null)
        .filter(key -> key.getIdentifier().equals(pIdentifier))
            .filter(Objects::nonNull)
        .findFirst();
    if (existingLocation.isPresent()) {
      result = existingLocation.get();
    } else {
      result = new MemoryLocation(pIdentifier);
    }
    return result;
  }

  public MemoryLocation getValue(CExpression pOperand1) {
    // case p: CIdExpression map.get(p)
    // case *p: map.get(map.get(p)) -- assumption: p points to another pointer
    // case q->p: map.get(q).getChild(p)
    // case &q: map.keys.get(q)
    // case q.p: q.getChild(p)
    // case *&p: case p
    // case &*p: case p

    return null;
  }

  public MemoryLocation getLocationFromInitializer(CInitializer pInitializer) {
    // Assumptions: Initializer refers to an existing MemoryLocation through an arbitrarily deep
    // nesting of operators, e.g. a->b.c->d.e

    if (pInitializer instanceof CInitializerExpression) {
      CInitializerExpression initializerExpression = (CInitializerExpression) pInitializer;
      CExpression expression = initializerExpression.getExpression();
      getLocationFromExpression(expression);

    }

    // TODO Auto-generated method stub
    return null;
  }

  private MemoryLocation getLocationFromExpression(CExpression pExpression) {
    boolean isNestedFurther = true;
    CExpression curExpression = pExpression;
    MemoryLocation value;
    while (isNestedFurther) {
      if (curExpression instanceof CIdExpression) {
        CIdExpression idExpression = (CIdExpression) curExpression;
        String identifier = idExpression.getName();
        return getTargetFromIdentifier(identifier);
      } else {
        isNestedFurther = false;
      }
    }
    return new MemoryLocation();
  }

}
