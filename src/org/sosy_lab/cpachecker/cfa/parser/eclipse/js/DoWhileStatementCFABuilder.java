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

import static org.sosy_lab.cpachecker.cfa.model.js.JSAssumeEdge.assume;

import org.eclipse.wst.jsdt.core.dom.DoStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

@SuppressWarnings("ResultOfMethodCallIgnored")
class DoWhileStatementCFABuilder implements DoWhileStatementAppendable {

  @Override
  public void append(final JavaScriptCFABuilder pBuilder, final DoStatement pNode) {
    final LoopScopeImpl loopScope = new LoopScopeImpl(pBuilder.getScope());
    final JavaScriptCFABuilder loopBuilder = pBuilder.copyWith(loopScope);
    final CFANode entryNode = loopBuilder.getExitNode();
    final CFANode exitNode = loopBuilder.createNode();
    loopScope.setLoopExitNode(exitNode);
    // loop start node comes after first run
    final CFANode loopStartNode = loopBuilder.createNode();
    loopStartNode.setLoopStart();
    loopScope.setLoopStartNode(loopStartNode);
    loopScope.setLoopContinueNode(loopStartNode);
    loopBuilder.append(pNode.getBody());
    loopBuilder.appendEdge(
        loopStartNode, DummyEdge.withDescription("check do-while loop condition"));
    final JSExpression condition = loopBuilder.append(pNode.getExpression());
    final JavaScriptCFABuilder loopEdgeBuilder = loopBuilder.copy();
    loopEdgeBuilder.appendEdge(entryNode, assume(condition, true));
    loopBuilder.appendEdge(exitNode, assume(condition, false));
    loopBuilder.appendTo(pBuilder.getBuilder());
  }
}
