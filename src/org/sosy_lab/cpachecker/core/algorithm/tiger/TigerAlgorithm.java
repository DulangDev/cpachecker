/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.tiger.TigerAlgorithmConfiguration.CoverageCheck;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.FQLSpecificationUtil;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.FQLSpecification;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.AutomatonGoal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCase;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCaseVariable;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestGoalUtils;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.ThreeValuedAnswer;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.WorklistEntryComparator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.Wrapper;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.timeout.TimeoutCPA;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

public class TigerAlgorithm extends TigerBaseAlgorithm<AutomatonGoal> {


  private TestGoalUtils testGoalUtils;
  private FQLSpecification fqlSpecification;

  public TigerAlgorithm(
      LogManager pLogger,
      CFA pCfa,
      Configuration pConfig,
      ConfigurableProgramAnalysis pCpa,
      ShutdownNotifier pShutdownNotifier,
      @Nullable final Specification stats) throws InvalidConfigurationException {
    init(pLogger, pCfa, pConfig, pCpa, pShutdownNotifier, stats);

    pShutdownNotifier.register(this);
    testGoalUtils =
        new TestGoalUtils(
            logger,
            new Wrapper(
                pCfa,
                originalMainFunction,
                tigerConfig.shouldUseOmegaLabel()),
            pCfa,
            tigerConfig.shouldOptimizeGoalAutomata(),
            originalMainFunction);
    String preprocessFqlStmt = testGoalUtils.preprocessFQL(tigerConfig.getFqlQuery());
    fqlSpecification = FQLSpecificationUtil.getFQLSpecification(preprocessFqlStmt);
    logger.logf(Level.INFO, "FQL query: %s", fqlSpecification.toString());
  }


  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {
    logger.logf(
        Level.INFO,
        "We will not use the provided reached set since it violates the internal structure of Tiger's CPAs");
    logger.logf(Level.INFO, "We empty pReachedSet to stop complaints of an incomplete analysis");

    goalsToCover = initializeTestGoalSet();
    // testsuite = new TestSuite<>(bddUtils, goalsToCover, tigerConfig);
    String prefix = "";
    if (tigerConfig.shouldRemoveFeatureVariablePrefix()) {
      prefix = tigerConfig.getFeatureVariablePrefix();
    }
    testsuite = TestSuite.getAutomatonGoalTS(bddUtils, goalsToCover, prefix);

    boolean wasSound = true;
    if (!testGeneration(goalsToCover, pReachedSet)) {
      logger.logf(Level.WARNING, "Test generation contained unsound reachability analysis runs!");
      wasSound = false;
    }

    tsWriter.writeFinalTestSuite(testsuite);

    if (wasSound) {
      return AlgorithmStatus.SOUND_AND_PRECISE;
    } else {
      return AlgorithmStatus.UNSOUND_AND_PRECISE;
    }
  }

  private Set<AutomatonGoal> initializeTestGoalSet() {
    List<ElementaryCoveragePattern> goalPatterns;
    List<Pair<ElementaryCoveragePattern, Region>> pTestGoalPatterns = new LinkedList<>();

    goalPatterns = testGoalUtils.extractTestGoalPatterns(fqlSpecification);

    for (int i = 0; i < goalPatterns.size(); i++) {
      pTestGoalPatterns.add(Pair.of(goalPatterns.get(i), (Region) null));
    }

    int goalIndex = 1;
    Set<AutomatonGoal> goals = new HashSet<>();
    for (Pair<ElementaryCoveragePattern, Region> pair : pTestGoalPatterns) {
      AutomatonGoal lGoal =
          testGoalUtils.constructAutomatonGoal(
              goalIndex,
              pair.getFirst(),
              pair.getSecond(),
              tigerConfig.shouldUseOmegaLabel());
      logger.log(Level.INFO, lGoal.getName());
      goals.add(lGoal);
      goalIndex++;
    }

    return goals;
  }

  // TODO add pc to log output
  // TODO add parameter LinkedList<Edges>> pInfeasibilityPropagation
  // TODO add the parameter to runreachabilityanalysis
  @SuppressWarnings("unchecked")
  private boolean testGeneration(Set<AutomatonGoal> pGoalsToCover, ReachedSet pReachedSet)
      throws CPAException, InterruptedException {
    boolean wasSound = true;
    boolean retry = false;
    int numberOfTestGoals = pGoalsToCover.size();
    do {
      if (retry) {
        // retry timed-out goals
        boolean order = true;

        if (tigerConfig.getTimeoutIncrement() > 0) {
          long oldCPUTimeLimitPerGoal = tigerConfig.getCpuTimelimitPerGoal();
          tigerConfig.increaseCpuTimelimitPerGoal(tigerConfig.getTimeoutIncrement());
          // tigerConfig.getCpuTimelimitPerGoal() += tigerConfig..getTimeoutIncrement();
          logger.logf(
              Level.INFO,
              "Incremented timeout from %d to %d seconds.",
              oldCPUTimeLimitPerGoal,
              tigerConfig.getCpuTimelimitPerGoal());

          Collection<Entry<Integer, Pair<AutomatonGoal, Region>>> set;
          if (tigerConfig.useOrder()) {
            if (tigerConfig.useInverseOrder()) {
              order = !order;
            }

            // keep original order of goals (or inverse of it)
            if (order) {
              set = new TreeSet<>(WorklistEntryComparator.ORDER_RESPECTING_COMPARATOR);
            } else {
              set = new TreeSet<>(WorklistEntryComparator.ORDER_INVERTING_COMPARATOR);
            }
            for (Entry<Integer, Pair<AutomatonGoal, Region>> entry : testsuite.getTimedOutGoals()
                .entrySet()) {
              set.add(entry);
            }
          } else {
            set = new LinkedList<>();
            for (Entry<Integer, Pair<AutomatonGoal, Region>> entry : testsuite.getTimedOutGoals()
                .entrySet()) {
              set.add(entry);
            }
          }

          pGoalsToCover.clear();
          for (Entry<Integer, Pair<AutomatonGoal, Region>> entry : set) {
            pGoalsToCover.add(entry.getValue().getFirst());
          }
          testsuite.getTimedOutGoals().clear();
        }
      }
      while (!pGoalsToCover.isEmpty()) {
        AutomatonGoal goal = pGoalsToCover.iterator().next();
        pGoalsToCover.remove(goal);

        logger
            .logf(Level.INFO, "Processing test goal %d of %d.", goal.getIndex(), numberOfTestGoals);

        ReachabilityAnalysisResult result =
            runReachabilityAnalysis(goal, goal.getIndex(), pGoalsToCover, pReachedSet);

        if (result.equals(ReachabilityAnalysisResult.UNSOUND)) {
          logger.logf(Level.WARNING, "Analysis run was unsound!");
          wasSound = false;
        }
        if (result.equals(ReachabilityAnalysisResult.TIMEDOUT)) {
          logger.log(Level.INFO, "Adding timedout Goal to testsuite!");
          testsuite.addTimedOutGoal(goal.getIndex(), goal, null);
          // break;
        }
      }

      if (testsuite.getTimedOutGoals().isEmpty()) {
        logger.logf(Level.INFO, "There were no timed out goals.");
        retry = false;
      } else {
        if (!tigerConfig.getTimeoutStrategy().equals(TimeoutStrategy.RETRY_AFTER_TIMEOUT)) {
          logger.logf(
              Level.INFO,
              "There were timed out goals but retry after timeout strategy is disabled.");
        } else {
          retry = true;
        }
      }
    } while (retry);
    return wasSound;
  }

  @SuppressWarnings("unused")
  private boolean isCovered(int goalIndex, AutomatonGoal lGoal) {
    @SuppressWarnings("unused")
    Region remainingPCforGoalCoverage = lGoal.getPresenceCondition();
    boolean isFullyCovered = false;
    for (TestCase testcase : testsuite.getTestCases()) {
      ThreeValuedAnswer isCovered = testcase.coversGoal(lGoal);
      if (isCovered.equals(ThreeValuedAnswer.UNKNOWN)) {
        logger.logf(
            Level.WARNING,
            "Coverage check for goal %d could not be performed in a precise way!",
            goalIndex);
        continue;
      } else if (isCovered.equals(ThreeValuedAnswer.REJECT)) {
        continue;
      }
    }

    return isFullyCovered;
  }

  private void writeARG(int goalIndex, ReachedSet pReachedSet) {
    try (OutputStreamWriter writer =
        new OutputStreamWriter(
            new FileOutputStream(new File("output", "ARG_goal_" + goalIndex + ".dot")),
            "UTF-8")) {
        ARGUtils.writeARGAsDot(writer, (ARGState) pReachedSet.getFirstState());
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write ARG to file");
      }
  }

  private ReachabilityAnalysisResult
      runReachabilityAnalysis(
          AutomatonGoal pGoal,
          int goalIndex,
          Set<AutomatonGoal> pGoalsToCover,
          ReachedSet pReachedSet)
          throws CPAException, InterruptedException {

    // build CPAs for the goal
    ARGCPA lARTCPA = buildCPAs(pGoal);
    initializeReachedSet(pReachedSet, lARTCPA);


    // TODO reuse prediccates option
    // if (reusePredicates) {
    // // initialize reused predicate precision
    // PredicateCPA predicateCPA = pArgCPA.retrieveWrappedCpa(PredicateCPA.class);
    //
    // if (predicateCPA != null) {
    // reusedPrecision = (PredicatePrecision)
    // predicateCPA.getInitialPrecision(cfa.getMainFunction(),
    // StateSpacePartition.getDefaultPartition());
    // } else {
    // logger.logf(Level.INFO, "No predicate CPA available to reuse predicates!");
    // }
    // }

    ShutdownManager algNotifier =
        ShutdownManager.createWithParent(startupConfig.getShutdownNotifier());

    // run analysis
    Algorithm algorithm = rebuildAlgorithm(algNotifier, lARTCPA, pReachedSet);
    Region presenceConditionToCover = testsuite.getRemainingPresenceCondition(pGoal);
    bddUtils.restrictBdd(presenceConditionToCover);
    if (timeoutCPA != null) {
      timeoutCPA.setWalltime(tigerConfig.getCpuTimelimitPerGoal());
    }
    Pair<Boolean, Boolean> analysisWasSound_hasTimedOut;
    do {

      analysisWasSound_hasTimedOut = runAlgorithm(algorithm, pReachedSet);

      // fully explored reachedset, therefore the last "testcase" was already added to the testsuite
      // in this case we break out of the loop, since the goal does not have more feasable goals
      if (!pReachedSet.hasWaitingState()) {
        break;
      }

      if (analysisWasSound_hasTimedOut.getSecond()) {
        // timeout, do not retry for other goals
        break;
        // return ReachabilityAnalysisResult.TIMEDOUT;
      }

      AbstractState lastState = pReachedSet.getLastState();

      if (lastState == null || !AbstractStates.isTargetState(lastState)) {
        // goals are infeasible, do not continue
        break;
      }

      logger.logf(Level.INFO, "Test goal is feasible.");
      CFAEdge criticalEdge = pGoal.getCriticalEdge();

      // For testing
      Optional<CounterexampleInfo> cexi = ((ARGState) lastState).getCounterexampleInformation();
      if (cexi.isPresent()) {
        logger.log(Level.INFO, "cexi is Present");
      }

      Region testCasePresenceCondition = bddUtils.getRegionFromWrappedBDDstate(lastState);

      if (!cexi.isPresent()/* counterexamples.isEmpty() */) {

        TestCase testcase =
            handleUnavailableCounterexample(criticalEdge, lastState, testCasePresenceCondition);
        testsuite.addTestCase(testcase, pGoal);
      } else {
        // test goal is feasible
        logger.logf(Level.INFO, "Counterexample is available.");
        CounterexampleInfo cex = cexi.get();
        if (cex.isSpurious()) {
          logger.logf(Level.WARNING, "Counterexample is spurious!");
        } else {
          // HashMap<String, Boolean> features =
          // for null goal get the presencecondition without the validProduct method
          testCasePresenceCondition = getPresenceConditionFromCex(cex);

          // Region simplifiedPresenceCondition = getPresenceConditionFromCexForGoal(cex, pGoal);
          TestCase testcase = createTestcase(cex, testCasePresenceCondition);
          // only add new Testcase and check for coverage if it does not already exist

          testsuite.addTestCase(testcase, pGoal);

          if (tigerConfig.getCoverageCheck() == CoverageCheck.SINGLE
              || tigerConfig.getCoverageCheck() == CoverageCheck.ALL) {

            // remove covered goals from goalstocover if
            // we want only one featureconfiguration per goal
            // or do not want variability at all
            // otherwise we need to keep the goals, to cover them for each possible configuration
            boolean removeGoalsToCover =
                !bddUtils.isVariabilityAware() || tigerConfig.shouldUseSingleFeatureGoalCoverage();
            Set<AutomatonGoal> goalsToCheckCoverage = new HashSet<>(pGoalsToCover);
            if (tigerConfig.getCoverageCheck() == CoverageCheck.ALL) {
              goalsToCheckCoverage.addAll(testsuite.getTestGoals());
            }
            goalsToCheckCoverage.remove(pGoal);
            checkGoalCoverage(goalsToCheckCoverage, testcase, removeGoalsToCover);
          }

        }

      }

      Region remainingPC = testsuite.getRemainingPresenceCondition(pGoal);
      bddUtils.restrictBdd(remainingPC);
    } // continue if we use features and need a testcase for each valid feature config for each goal
      // (continues till infeasability is reached)
    while ((bddUtils.isVariabilityAware() && !tigerConfig.shouldUseSingleFeatureGoalCoverage())
        && !pReachedSet.getWaitlist().isEmpty());

    // write ARG to file
    writeARG(goalIndex, pReachedSet);

    if (bddUtils.isVariabilityAware()) {
      testsuite.addInfeasibleGoal(pGoal, testsuite.getRemainingPresenceCondition(pGoal));
    } else {
      if (testsuite.getCoveringTestCases(pGoal) == null
          || testsuite.getCoveringTestCases(pGoal).isEmpty()) {
        testsuite.addInfeasibleGoal(pGoal, null);
      }
    }
    if (!bddUtils.isVariabilityAware()) {
      pGoalsToCover.removeAll(testsuite.getTestGoals());
    }

    if (analysisWasSound_hasTimedOut.getSecond() == true) {
      return ReachabilityAnalysisResult.TIMEDOUT;
    }

    if (analysisWasSound_hasTimedOut.getFirst() == true) {
      return ReachabilityAnalysisResult.SOUND;
    } else {
      return ReachabilityAnalysisResult.UNSOUND;
    }
  }


  // @Override
  // public Region getPresenceConditionFromCexForGoal(CounterexampleInfo cex, AutomatonGoal pGoal) {
  //
  // NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton = pGoal.getAutomaton();
  // Set<NondeterministicFiniteAutomaton.State> lCurrentStates = new HashSet<>();
  // Set<NondeterministicFiniteAutomaton.State> lNextStates = new HashSet<>();
  //
  // lCurrentStates.add(lAutomaton.getInitialState());
  // Function<CFAEdge, Boolean> isFinalEdgeForGoal = cfaEdge -> {
  // for (NondeterministicFiniteAutomaton.State lCurrentState : lCurrentStates) {
  // // Automaton accepts as soon as it sees a final state (implicit self-loop)
  // if (lAutomaton.getFinalStates().contains(lCurrentState)) {
  // return true;
  // }
  //
  // for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOutgoingEdge : lAutomaton
  // .getOutgoingEdges(lCurrentState)) {
  // GuardedEdgeLabel lLabel = lOutgoingEdge.getLabel();
  //
  // if (!lLabel.hasGuards() && lLabel.contains(cfaEdge)) {
  // lNextStates.add(lOutgoingEdge.getTarget());
  // if (lAutomaton.getFinalStates().contains(lOutgoingEdge.getTarget())) {
  // return true;
  // }
  // }
  // }
  // }
  //
  // lCurrentStates.addAll(lNextStates);
  // return false;
  // };

  //
  //
  // return getPresenceConditionFromCexUpToEdge(cex, isFinalEdgeForGoal);
  // }

  private CPAFactory buildAutomataFactory(Automaton goalAutomaton) {
    CPAFactory automataFactory = ControlAutomatonCPA.factory();
    automataFactory
        .setConfiguration(Configuration.copyWithNewPrefix(config, goalAutomaton.getName()));
    automataFactory.setLogger(logger.withComponentName(goalAutomaton.getName()));
    automataFactory.set(cfa, CFA.class);
    automataFactory.set(goalAutomaton, Automaton.class);
    return automataFactory;
  }

  private List<ConfigurableProgramAnalysis> buildComponentAnalyses(CPAFactory automataFactory)
      throws CPAException {
    List<ConfigurableProgramAnalysis> lAutomatonCPAs = new ArrayList<>(1);// (2);
    try {
      lAutomatonCPAs.add(automataFactory.createInstance());
    } catch (InvalidConfigurationException e1) {
      throw new CPAException("Invalid automata!", e1);
    }

    List<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<>();
    lComponentAnalyses.addAll(lAutomatonCPAs);

    if (cpa instanceof CompositeCPA) {
      CompositeCPA compositeCPA = (CompositeCPA) cpa;
      lComponentAnalyses.addAll(compositeCPA.getWrappedCPAs());
    } else if (cpa instanceof ARGCPA) {
      lComponentAnalyses.addAll(((ARGCPA) cpa).getWrappedCPAs());
    } else {
      lComponentAnalyses.add(cpa);
    }
    return lComponentAnalyses;
  }

  private ARGCPA buildARGCPA(
      List<ConfigurableProgramAnalysis> lComponentAnalyses,
      Specification goalAutomatonSpecification) {
    ARGCPA lARTCPA;
    try {

      // create timeout CPA
      CPAFactory toFactory = TimeoutCPA.factory();
      toFactory.setConfiguration(startupConfig.getConfig());
      toFactory.setLogger(logger);


      // create composite CPA
      CPAFactory lCPAFactory = CompositeCPA.factory();
      lCPAFactory.setChildren(lComponentAnalyses);
      lCPAFactory.setConfiguration(startupConfig.getConfig());
      lCPAFactory.setLogger(logger);
      lCPAFactory.set(cfa, CFA.class);


      ConfigurableProgramAnalysis lCPA = lCPAFactory.createInstance();

      // create ART CPA
      CPAFactory lARTCPAFactory = ARGCPA.factory();
      lARTCPAFactory.set(cfa, CFA.class);
      lARTCPAFactory.setChild(lCPA);
      lARTCPAFactory.setConfiguration(startupConfig.getConfig());
      lARTCPAFactory.setLogger(logger);
      lARTCPAFactory.set(goalAutomatonSpecification, Specification.class);

      lARTCPA = (ARGCPA) lARTCPAFactory.createInstance();
    } catch (InvalidConfigurationException | CPAException e) {
      throw new RuntimeException(e);
    }
    return lARTCPA;
  }

  private ARGCPA buildCPAs(AutomatonGoal pGoal)// LinkedList<Goal> pGoalsToCover)
      throws CPAException {
    Automaton goalAutomaton = pGoal.createControlAutomaton();
    Specification goalAutomatonSpecification =
        Specification.fromAutomata(Lists.newArrayList(goalAutomaton));

    CPAFactory automataFactory = buildAutomataFactory(goalAutomaton);
    List<ConfigurableProgramAnalysis> lComponentAnalyses =
        buildComponentAnalyses(automataFactory);
    return buildARGCPA(lComponentAnalyses, goalAutomatonSpecification);

  }





  private TestCase handleUnavailableCounterexample(
      CFAEdge criticalEdge,
      AbstractState lastState,
      Region pPresenceCondition) {

    logger.logf(Level.INFO, "Counterexample is not available.");

    LinkedList<CFAEdge> trace = new LinkedList<>();

    // Try to reconstruct a trace in the ARG and shrink it
    ARGState argState = AbstractStates.extractStateByType(lastState, ARGState.class);
    Collection<ARGState> parents;
    parents = argState.getParents();

    while (!parents.isEmpty()) {

      ARGState parent = null;

      for (ARGState tmp_parent : parents) {
        parent = tmp_parent;
        break; // we just choose some parent
      }

      CFAEdge edge = parent.getEdgeToChild(argState);
      trace.addFirst(edge);

      // TODO Alex?
      if (edge.equals(criticalEdge)) {
        logger.logf(
            Level.INFO,
            "*********************** extract abstract state ***********************");
      }

      argState = parent;
      parents = argState.getParents();
    }

    List<TestCaseVariable> inputValues = new ArrayList<>();
    List<TestCaseVariable> outputValues = new ArrayList<>();

    TestCase result =
        new TestCase(
            nextTCID(),
            inputValues,
            outputValues,
            trace,
            pPresenceCondition,
            bddUtils);
    return result;
  }

  @Override
  public void shutdownRequested(String pArg0) {
    for (AutomatonGoal goal : goalsToCover) {
      if (!(testsuite.isGoalCovered(goal)
          || testsuite.isInfeasible(goal)
          || testsuite.isGoalTimedOut(goal))) {
        testsuite.addTimedOutGoal(goal.getIndex(), goal, null);
      }
    }
    tsWriter.writeFinalTestSuite(testsuite);
  }
}
