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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.js;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.wst.jsdt.core.dom.ObjectLiteral;
import org.eclipse.wst.jsdt.core.dom.ObjectLiteralField;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSObjectLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSObjectLiteralField;

class ObjectLiteralCFABuilder implements ObjectLiteralAppendable {

  @SuppressWarnings("unchecked")
  @Override
  public JSExpression append(
      final JavaScriptCFABuilder pBuilder, final ObjectLiteral pObjectLiteral) {
    final List<ObjectLiteralField> fields = pObjectLiteral.fields();
    final List<JSObjectLiteralField> fieldInitializers =
        Streams.zip(
                fields.stream()
                    .map(
                        field ->
                            // TODO identifier should to be parsed (JavaScript string literal)
                            // In the meantime, quotes are simply removed (escape sequence, etc. are
                            // not handled yet)
                            ((SimpleName) field.getFieldName())
                                .getIdentifier()
                                .replace("\"", "")
                                .replace("'", "")),
                pBuilder
                    .append(
                        fields.stream()
                            .map(ObjectLiteralField::getInitializer)
                            .collect(Collectors.toList()))
                    .stream(),
                (final String fieldName, final JSExpression initializer) ->
                    new JSObjectLiteralField(fieldName, initializer))
            .collect(Collectors.toList());
    return new JSObjectLiteralExpression(
        pBuilder.getFileLocation(pObjectLiteral), fieldInitializers);
  }
}
