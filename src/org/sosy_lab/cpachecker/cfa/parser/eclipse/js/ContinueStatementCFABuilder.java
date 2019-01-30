/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.js;

import org.eclipse.wst.jsdt.core.dom.ContinueStatement;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.js.JSContinueEdge;

@SuppressWarnings("ResultOfMethodCallIgnored")
class ContinueStatementCFABuilder implements ContinueStatementAppendable {

  @Override
  public void append(
      final JavaScriptCFABuilder pBuilder, final ContinueStatement pContinueStatement) {
    final LoopScope loopScope = findLoopScope(pBuilder, pContinueStatement);
    assert loopScope != null : "ContinueStatement has to be in a loop statement";
    pBuilder.appendJumpExitEdge(
        loopScope.getLoopContinueNode(),
        (final CFANode pPredecessor, final CFANode pSuccessor) ->
            new JSContinueEdge(
                pContinueStatement.toString(),
                pBuilder.getFileLocation(pContinueStatement),
                pPredecessor,
                pSuccessor));
  }

  private LoopScope findLoopScope(
      final JavaScriptCFABuilder pBuilder, final ContinueStatement pContinueStatement) {
    if (pContinueStatement.getLabel() == null) {
      return pBuilder.getScope().getScope(LoopScope.class);
    }
    // find loop scope that is closest child of the labeled scope with the same label name
    final String labelName = pContinueStatement.getLabel().getIdentifier();
    LoopScope lastLoopScope = pBuilder.getScope().getScope(LoopScope.class);
    assert lastLoopScope != null : "ContinueStatement has to be in a loop statement";
    boolean foundLabeledStatementScope = false;
    for (Scope current = lastLoopScope.getParentScope();
        current != null;
        current = current.getParentScope()) {
      if (current instanceof LoopScope) {
        lastLoopScope = (LoopScope) current;
      } else if (current instanceof LabeledStatementScope) {
        if (((LabeledStatementScope) current).getLabelName().equals(labelName)) {
          foundLabeledStatementScope = true;
          break;
        }
      }
    }
    assert foundLabeledStatementScope
        : "label \""
            + labelName
            + "\" not found in "
            + pBuilder.getFileLocation(pContinueStatement);
    return lastLoopScope;
  }
}
