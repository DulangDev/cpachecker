/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger.util;

import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CParser.FileToParse;
import org.sosy_lab.cpachecker.cfa.CSourceOriginMapping;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.core.algorithm.tiger.TigerAlgorithm;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.util.Pair;


public class WrapperUtil {

  public static final String CPAtiger_MAIN = "__CPAtiger__main";
  public static final String CPAtiger_INPUT = "input";

  public static FileToParse getWrapperCFunction(CFunctionEntryNode pMainFunction, LogManager logger)
      throws IOException {

    StringWriter lWrapperFunction = new StringWriter();
    PrintWriter lWriter = new PrintWriter(lWrapperFunction);

    // TODO interpreter is not capable of handling initialization of global declarations
    if (pMainFunction == null) {
      logger.log(Level.SEVERE, "No Entry function given");
      throw new NullPointerException("No Entry function given");
    }

    lWriter.println(pMainFunction.getFunctionDefinition().toASTString());
    lWriter.println();
    lWriter.println("extern int __VERIFIER_nondet_int();");
    lWriter.println();
    lWriter.println("int " +  CPAtiger_INPUT + "() {");
    lWriter.println("  return __VERIFIER_nondet_int();");
    lWriter.println("}");
    lWriter.println();
    lWriter.println("void " + CPAtiger_MAIN + "()");
    lWriter.println("{");

    for (CParameterDeclaration lDeclaration : pMainFunction.getFunctionParameters()) {
      lWriter.println("  " + lDeclaration.toASTString() + ";");
    }

    for (CParameterDeclaration lDeclaration : pMainFunction.getFunctionParameters()) {
      // TODO do we need to handle lDeclaration more specifically?
      lWriter.println("  " + lDeclaration.getName() + " = " +  CPAtiger_INPUT + "();");
    }

    lWriter.println();
    lWriter.print("  " + pMainFunction.getFunctionName() + "(");

    boolean isFirst = true;

    for (CParameterDeclaration lDeclaration : pMainFunction.getFunctionParameters()) {
      if (isFirst) {
        isFirst = false;
      }
      else {
        lWriter.print(", ");
      }

      lWriter.print(lDeclaration.getName());
    }

    lWriter.println(");");
    lWriter.println("  return;");
    lWriter.println("}");
    lWriter.println();

    File f = File.createTempFile(CPAtiger_MAIN, ".c", null);
    f.deleteOnExit();

    @SuppressWarnings("resource")
    Writer writer = null;

    try {
        writer = new BufferedWriter(new OutputStreamWriter(
              new FileOutputStream(f), "utf-8"));
        writer.write(lWrapperFunction.toString());
    } catch (IOException ex) {
      // TODO report
    } finally {
       try {writer.close();} catch (Exception ex) {}
    }

    //return new FileToParse(f.getAbsolutePath(), CPAtiger_MAIN + "__");
    return new FileToParse(f.getAbsolutePath());
  }

  public static ParseResult addWrapper(
      CParser cParser,
      ParseResult tmpParseResult,
      CSourceOriginMapping sourceOriginMapping,
      LogManager logger)
      throws IOException, CParserException, InvalidConfigurationException, InterruptedException {
    // create wrapper code
    CFunctionEntryNode entryNode = (CFunctionEntryNode)tmpParseResult.getFunctions().get(TigerAlgorithm.originalMainFunction);

    List<FileToParse> tmpList = new ArrayList<>();
    tmpList.add(WrapperUtil.getWrapperCFunction(entryNode, logger));

    ParseResult wrapperParseResult = cParser.parseFile(tmpList, sourceOriginMapping);

    // TODO add checks for consistency
    NavigableMap<String, FunctionEntryNode> mergedFunctions = new TreeMap<>();
    mergedFunctions.putAll(tmpParseResult.getFunctions());
    mergedFunctions.putAll(wrapperParseResult.getFunctions());

    SortedSetMultimap<String, CFANode> mergedCFANodes = TreeMultimap.create();
    mergedCFANodes.putAll(tmpParseResult.getCFANodes());
    mergedCFANodes.putAll(wrapperParseResult.getCFANodes());

    List<Pair<ADeclaration, String>> mergedGlobalDeclarations = new ArrayList<> (tmpParseResult.getGlobalDeclarations().size() + wrapperParseResult.getGlobalDeclarations().size());
    mergedGlobalDeclarations.addAll(tmpParseResult.getGlobalDeclarations());
    mergedGlobalDeclarations.addAll(wrapperParseResult.getGlobalDeclarations());

    return new ParseResult(
        mergedFunctions,
        mergedCFANodes,
        mergedGlobalDeclarations,
        tmpParseResult.getFileNames());
  }

}
