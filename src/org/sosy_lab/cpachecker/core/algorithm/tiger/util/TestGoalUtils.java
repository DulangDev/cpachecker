/// *
// * CPAchecker is a tool for configurable software verification.
// * This file is part of CPAchecker.
// *
// * Copyright (C) 2007-2017 Dirk Beyer
// * All rights reserved.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// *
// *
// * CPAchecker web page:
// * http://cpachecker.sosy-lab.org
// */
// package org.sosy_lab.cpachecker.core.algorithm.tiger.util;
//
// import java.util.Iterator;
// import java.util.LinkedList;
// import java.util.logging.Level;
// import org.sosy_lab.common.log.LogManager;
// import org.sosy_lab.cpachecker.cfa.CFA;
// import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.FQLSpecificationUtil;
// import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.FQLSpecification;
// import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ElementaryCoveragePattern;
// import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.SingletonECPEdgeSet;
// import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedEdgeLabel;
// import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.InverseGuardedEdgeLabel;
// import
/// org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.ToGuardedAutomatonTranslator;
// import
/// org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.CoverageSpecificationTranslator;
// import
/// org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.IncrementalCoverageSpecificationTranslator;
// import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.AutomatonGoal;
// import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
// import org.sosy_lab.cpachecker.util.predicates.regions.Region;
//
// public class TestGoalUtils {
//
// private LogManager logger;
// private GuardedEdgeLabel mAlphaLabel;
// private InverseGuardedEdgeLabel mInverseAlphaLabel;
// private GuardedEdgeLabel mOmegaLabel;
// private boolean optimizeGoalAutomata;
// private int statistics_numberOfTestGoals;
// private CoverageSpecificationTranslator mCoverageSpecificationTranslator;
//
// // TODO add variablewhitelist from new parameter String bdpvwl.. or maybe it is not used
// //
// public TestGoalUtils(LogManager pLogger, Wrapper wrapper, CFA cfa, boolean pOptimizeGoalAutomata,
/// String originalMainFunction) {
//
// mAlphaLabel = new GuardedEdgeLabel(new SingletonECPEdgeSet(wrapper.getAlphaEdge()));
// mInverseAlphaLabel = new InverseGuardedEdgeLabel(mAlphaLabel);
// mOmegaLabel = new GuardedEdgeLabel(new SingletonECPEdgeSet(wrapper.getOmegaEdge()));
// mCoverageSpecificationTranslator = new CoverageSpecificationTranslator(
// cfa.getFunctionHead(originalMainFunction));
//
// logger = pLogger;
// optimizeGoalAutomata = pOptimizeGoalAutomata;
//
// }
//
//
// public AutomatonGoal constructAutomatonGoal(
// int pIndex,
// ElementaryCoveragePattern pGoalPattern,
// Region pPresenceCondition,
// boolean pUseOmegaLabel) {
// NondeterministicFiniteAutomaton<GuardedEdgeLabel> automaton =
// ToGuardedAutomatonTranslator.toAutomaton(pGoalPattern, mAlphaLabel, mInverseAlphaLabel,
// mOmegaLabel,
// pUseOmegaLabel);
// automaton = FQLSpecificationUtil.optimizeAutomaton(automaton, optimizeGoalAutomata);
//
// AutomatonGoal lGoal = new AutomatonGoal(pIndex, pGoalPattern, automaton, pPresenceCondition);
//
// return lGoal;
// }
//
/// *
// public Set<Goal> extractTestGoalPatterns(FQLSpecification pFqlSpecification,
// CoverageSpecificationTranslator pCoverageSpecificationTranslator) {
//
// logger.logf(Level.INFO, "Extracting test goals.");
//
//
// // TODO check for (temporarily) unsupported features
//
// // TODO enable use of infeasibility propagation
//
//
// IncrementalCoverageSpecificationTranslator lTranslator =
// new IncrementalCoverageSpecificationTranslator(
// pCoverageSpecificationTranslator.mPathPatternTranslator);
//
// int statistics_numberOfTestGoals =
// lTranslator.getNumberOfTestGoals(pFqlSpecification.getCoverageSpecification());
//
// Iterator<ElementaryCoveragePattern> lGoalIterator =
// lTranslator.translate(pFqlSpecification.getCoverageSpecification());
// LinkedList<ElementaryCoveragePattern> lGoalPatterns = new LinkedList<>();
//
// for (int lGoalIndex = 0; lGoalIndex < statistics_numberOfTestGoals; lGoalIndex++) {
// lGoalPatterns.add(lGoalIterator.next());
// }
//
// LinkedList<Pair<ElementaryCoveragePattern, Region>> pTestGoalPatterns = new LinkedList<>();
// for (int i = 0; i < lGoalPatterns.size(); i++) {
// pTestGoalPatterns.add(Pair.of(lGoalPatterns.get(i), (Region) null));
// }
//
// int goalIndex = 1;
// Set<Goal> pGoalsToCover = new LinkedHashSet<>();
// for (Pair<ElementaryCoveragePattern, Region> pair : pTestGoalPatterns) {
// Goal lGoal =
// constructGoal(goalIndex, pair.getFirst(), mAlphaLabel, mInverseAlphaLabel, mOmegaLabel,
// optimizeGoalAutomata,
// pair.getSecond());
// logger.log(Level.INFO, lGoal.getName());
// pGoalsToCover.add(lGoal);
// goalIndex++;
// }
//
// return pGoalsToCover;
// }*/
//
// public LinkedList<ElementaryCoveragePattern> extractTestGoalPatterns(FQLSpecification
/// pFqlSpecification) {
// logger.logf(Level.INFO, "Extracting test goals.");
//
//
// // TODO check for (temporarily) unsupported features
//
// // TODO enable use of infeasibility propagation
//
//
// IncrementalCoverageSpecificationTranslator lTranslator =
// new IncrementalCoverageSpecificationTranslator(
// mCoverageSpecificationTranslator.mPathPatternTranslator);
//
// statistics_numberOfTestGoals =
// lTranslator.getNumberOfTestGoals(pFqlSpecification.getCoverageSpecification());
// logger.logf(Level.INFO, "Number of test goals: %d", statistics_numberOfTestGoals);
//
// Iterator<ElementaryCoveragePattern> lGoalIterator =
// lTranslator.translate(pFqlSpecification.getCoverageSpecification());
// LinkedList<ElementaryCoveragePattern> lGoalPatterns = new LinkedList<>();
//
// for (int lGoalIndex = 0; lGoalIndex < statistics_numberOfTestGoals; lGoalIndex++) {
// lGoalPatterns.add(lGoalIterator.next());
// }
//
// return lGoalPatterns;
// }
//
// private String proprocessFQLGoal(String goal) {
// StringBuilder fql = new StringBuilder();
// if (goal.contains("<->")) {
// return GoalsToEdges(permute(goal.split("<->"), "->", 0));
// } else if (goal.contains("||")) {
// return GoalsToEdges(combine(goal.split("\\|\\|"), "<->"));
// } else {
// fql.append("(\"EDGES(ID)*\"");
// for (String singleGoal : goal.trim().split("->")) {
// fql.append(".(EDGES(@LABEL(");
// fql.append(singleGoal.trim());
// fql.append("))).\"EDGES(ID)*\"");
// }
// fql.append(")");
// }
// return fql.toString();
// }
//
// static String combine(String[] a, String combinator) {
// StringBuilder goals = new StringBuilder();
// for (int i = 0; i < a.length; i++) {
// for (int x = i + 1; x < a.length; x++) {
// goals.append(a[i]);
// goals.append(combinator);
// goals.append(a[x]);
// goals.append(",");
// }
// }
// return goals.toString().substring(0, goals.length() - 1);
// }
//
//
// static String permute(String[] a, String combinator, int k) {
//
// if (k == a.length) {
// StringBuilder goals = new StringBuilder();
// for (int i = 0; i < a.length; i++) {
// goals.append(a[i].trim());
// goals.append(combinator);
// }
// return goals.substring(0, goals.length() - combinator.length());
// } else {
// StringBuilder allGoals = new StringBuilder();
// for (int i = k; i < a.length; i++) {
// String temp = a[k];
// a[k] = a[i];
// a[i] = temp;
//
// allGoals.append(permute(a, combinator, k + 1));
// allGoals.append(",");
//
// temp = a[k];
// a[k] = a[i];
// a[i] = temp;
// }
// return allGoals.substring(0, allGoals.length() - 1);
// }
// }
//
// private String GoalsToEdges(String goalString) {
// StringBuilder fql = new StringBuilder();
// String[] goalList = goalString.split(",");
// boolean first = true;
// for (String goal : goalList) {
// if (!first) {
// fql.append("+");
// }
// fql.append(proprocessFQLGoal(goal));
// first = false;
// }
//
// return fql.toString();
// }
//
// public String preprocessFQL(String fqlString) {
// String goals = "goals:";
// if (!fqlString.trim().toLowerCase().startsWith(goals)) {
// return fqlString;
// }
// try {
// String fql = "COVER ";
// String goalListString = fqlString.trim().substring(goals.length());
// fql += GoalsToEdges(goalListString);
// return fql;
// } catch (Exception ex) {
// return fqlString;
// }
// /*-setprop tiger.fqlQuery="COVER
/// (\"EDGES(ID)*\".(EDGES(@LABEL(G1))).\"EDGES(ID)*\")+(\"EDGES(ID)*\".(EDGES(@LABEL(G2))).\"EDGES(ID)*\")+(\"EDGES(ID)*\".(EDGES(@LABEL(G3))).\"EDGES(ID)*\")+(\"EDGES(ID)*\".(EDGES(@LABEL(G4))).\"EDGES(ID)*\")+(\"EDGES(ID)*\".(EDGES(@LABEL(G5))).\"EDGES(ID)*\")+(\"EDGES(ID)*\".(EDGES(@LABEL(G6))).\"EDGES(ID)*\")+(\"EDGES(ID)*\".(EDGES(@LABEL(G7))).\"EDGES(ID)*\")*/
// }
//
// }
