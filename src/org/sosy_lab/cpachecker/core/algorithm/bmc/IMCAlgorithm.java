// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.TargetLocationCandidateInvariant;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundCPA;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * This class provides an implementation of interpolation-based model checking algorithm, adapted
 * for program verification. The original algorithm was proposed in the paper "Interpolation and
 * SAT-based Model Checking" from K. L. McMillan. The algorithm consists of two phases: BMC phase
 * and interpolation phase. In the BMC phase, it unrolls the CFA and collects the path formula to
 * target states. If the path formula is UNSAT, it enters the interpolation phase, and computes
 * interpolants which are overapproximations of k-step reachable states. If the union of
 * interpolants grows to an inductive set of states, the property is proved. Otherwise, it returns
 * back to the BMC phase and keeps unrolling the CFA.
 */
@Options(prefix="imc")
public class IMCAlgorithm extends AbstractBMCAlgorithm implements Algorithm {

  @Option(secure = true, description = "try using interpolation to verify programs with loops")
  private boolean interpolation = true;

  @Option(secure = true, description = "toggle deriving the interpolants from suffix formulas")
  private boolean deriveInterpolantFromSuffix = true;

  @Option(secure = true, description = "toggle collecting formulas by traversing ARG")
  private boolean collectFormulasByTraversingARG = false;

  @Option(secure = true, description = "toggle checking existence of covered states in ARG")
  private boolean checkExistenceOfCoveredStates = false;

  @Option(secure = true, description = "toggle checking existence of target states in ARG")
  private boolean checkExistenceOfTargetStates = false;

  @Option(secure = true, description = "toggle sanity check of collected formulas")
  private boolean checkSanityOfFormulas = false;

  private final ConfigurableProgramAnalysis cpa;

  private final Algorithm algorithm;

  private final FormulaManagerView fmgr;
  private final PathFormulaManager pmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;

  private final CFA cfa;

  public IMCAlgorithm(
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCPA,
      Configuration pConfig,
      LogManager pLogger,
      ReachedSetFactory pReachedSetFactory,
      ShutdownManager pShutdownManager,
      CFA pCFA,
      final Specification specification,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    super(
        pAlgorithm,
        pCPA,
        pConfig,
        pLogger,
        pReachedSetFactory,
        pShutdownManager,
        pCFA,
        specification,
        new BMCStatistics(),
        false /* no invariant generator */,
        pAggregatedReachedSets);
    pConfig.inject(this);

    cpa = pCPA;
    cfa = pCFA;
    algorithm = pAlgorithm;

    @SuppressWarnings("resource")
    PredicateCPA predCpa = CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, IMCAlgorithm.class);
    solver = predCpa.getSolver();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pmgr = predCpa.getPathFormulaManager();
  }

  @Override
  public AlgorithmStatus run(final ReachedSet pReachedSet) throws CPAException, InterruptedException {
    try {
      return interpolationModelChecking(pReachedSet);
    } catch (SolverException e) {
      throw new CPAException("Solver Failure " + e.getMessage(), e);
    } finally {
      invariantGenerator.cancel();
    }
  }

  /**
   * The main method for interpolation-based model checking.
   *
   * @param pReachedSet Abstract Reachability Graph (ARG)
   *
   * @return {@code AlgorithmStatus.UNSOUND_AND_PRECISE} if an error location is reached, i.e.,
   *         unsafe; {@code AlgorithmStatus.SOUND_AND_PRECISE} if a fixed point is derived, i.e.,
   *         safe.
   */
  private AlgorithmStatus interpolationModelChecking(final ReachedSet pReachedSet)
      throws CPAException, SolverException, InterruptedException {
    if (!(cfa.getAllLoopHeads().isPresent() && cfa.getAllLoopHeads().orElseThrow().size() <= 1)) {
      throw new CPAException("Multi-loop programs are not supported yet");
    }

    logger.log(Level.FINE, "Performing interpolation-based model checking");
    do {
      int maxLoopIterations = CPAs.retrieveCPA(cpa, LoopBoundCPA.class).getMaxLoopIterations();

      shutdownNotifier.shutdownIfNecessary();
      logger.log(Level.FINE, "Unrolling with LBE, maxLoopIterations =", maxLoopIterations);
      stats.bmcPreparation.start();
      BMCHelper.unroll(logger, pReachedSet, algorithm, cpa);
      stats.bmcPreparation.stop();
      shutdownNotifier.shutdownIfNecessary();

      if (checkExistenceOfCoveredStates
          && !from(pReachedSet).transformAndConcat(e -> ((ARGState) e).getCoveredByThis())
              .isEmpty()) {
        throw new CPAException("Covered states exist in ARG, analysis result could be wrong.");
      }

      if (checkExistenceOfTargetStates && AbstractStates.getTargetStates(pReachedSet).isEmpty()) {
        logger.log(Level.WARNING, "No target states at maxLoopIterations =", maxLoopIterations);
        if (maxLoopIterations == 1) {
          adjustConditions();
          continue;
        } else {
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
      }

      logger.log(Level.FINE, "Collecting prefix, loop, and suffix formulas");
      PartitionedFormulas formulas = collectFormulas(pReachedSet, maxLoopIterations);
      logger.log(Level.ALL, "The prefix is", formulas.prefixFormula.getFormula());
      logger.log(Level.ALL, "The loop is", formulas.loopFormula);
      logger.log(Level.ALL, "The suffix is", formulas.suffixFormula);

      if (checkSanityOfFormulas && !checkSanityOfFormulas(formulas)) {
        throw new CPAException("Collected formulas are insane, analysis result could be wrong.");
      }

      BooleanFormula reachErrorFormula =
          bfmgr.or(
              bfmgr.and(
                  formulas.prefixFormula.getFormula(),
                  formulas.loopFormula,
                  formulas.suffixFormula),
              formulas.errorBeforeLoopFormula);
      if (!solver.isUnsat(reachErrorFormula)) {
        logger.log(Level.FINE, "A target state is reached by BMC");
        return AlgorithmStatus.UNSOUND_AND_PRECISE;
      } else {
        logger.log(Level.FINE, "No error is found up to maxLoopIterations = ", maxLoopIterations);
        if (pReachedSet.hasViolatedProperties()) {
          TargetLocationCandidateInvariant.INSTANCE.assumeTruth(pReachedSet);
        }
      }

      if (interpolation && maxLoopIterations > 1) {
        logger.log(Level.FINE, "Computing fixed points by interpolation");
        try (InterpolatingProverEnvironment<?> itpProver =
            solver.newProverEnvironmentWithInterpolation()) {
          if (reachFixedPointByInterpolation(itpProver, formulas)) {
            return AlgorithmStatus.SOUND_AND_PRECISE;
          }
        }
      }
    } while (adjustConditions());
    return AlgorithmStatus.UNSOUND_AND_PRECISE;
  }

  /**
   * A helper method to collect formulas needed by IMC algorithm. Three formulas are collected:
   * prefix, loop, and suffix. Prefix formula describes all paths from the initial state to the
   * first LH. Loop formula describes all paths from the first LH to the second LH. Suffix formula
   * is the conjunction of the all paths from the second LH to the second last LH and the block
   * formulas at target states. The second LH to the second last LH was stored in a formula called
   * tail formula before, in order to save the effort to compute the conjunction in every unrolling
   * iteration. However, to make the code easier to understand, the tail formula is not stored in
   * {@link PartitionedFormulas} now. Considering the conjunction is only a syntactic operation, and
   * the unrolling numbers are typically not large, this trade-off between efficiency and
   * readability should be acceptable. Two subroutines to collect formulas are available: one
   * syntactic and the other semantic. The old implementation relies on {@link CFANode} to detect
   * syntactic loops, but it turns out that syntactic LHs are not always abstraction states, and
   * hence it might not always collect block formulas from abstraction states. To solve this
   * mismatch, a new implementation which directly traverses ARG was developed, and it guarantees to
   * collect block formulas from abstraction states. The new method can be enabled by setting
   * {@code imc.collectFormulasByTraversingARG=true}.
   *
   * @param pReachedSet Abstract Reachability Graph
   *
   * @param maxLoopIterations The upper bound of unrolling times
   *
   * @throws InterruptedException On shutdown request.
   *
   */
  private PartitionedFormulas collectFormulas(final ReachedSet pReachedSet, int maxLoopIterations)
      throws InterruptedException {
    if (collectFormulasByTraversingARG) {
      return collectFormulasByTraversingARG(pReachedSet);
    } else {
      return collectFormulasBySyntacticLoop(pReachedSet, maxLoopIterations);
    }
  }

  private PartitionedFormulas
      collectFormulasByTraversingARG(final ReachedSet pReachedSet) {
    PathFormula prefixFormula =
        new PathFormula(
            bfmgr.makeFalse(),
            SSAMap.emptySSAMap(),
            PointerTargetSet.emptyPointerTargetSet(),
            0);
    BooleanFormula loopFormula = bfmgr.makeTrue();
    BooleanFormula tailFormula = bfmgr.makeTrue();
    BooleanFormula targetFormula = bfmgr.makeFalse();
    BooleanFormula errorBeforeLoopFormula = bfmgr.makeFalse();
    for (AbstractState targetStateBeforeLoop : getTargetStatesBeforeLoop(pReachedSet)) {
      errorBeforeLoopFormula =
          bfmgr.or(
              errorBeforeLoopFormula,
              getPredicateAbstractionBlockFormula(targetStateBeforeLoop).getFormula());
    }
    if (!AbstractStates.getTargetStates(pReachedSet)
        .filter(e -> !isTargetStateBeforeLoopStart(e))
        .isEmpty()) {
      List<AbstractState> targetStatesAfterLoop = getTargetStatesAfterLoop(pReachedSet);
      // Initialize prefix, loop, and tail using the first target state after the loop
      // Assumption: every target state after the loop has the same abstraction-state path to root
      List<ARGState> abstractionStates = getAbstractionStatesToRoot(targetStatesAfterLoop.get(0));
      prefixFormula = getPredicateAbstractionBlockFormula(abstractionStates.get(1));
      if (abstractionStates.size() > 3) {
        loopFormula = getPredicateAbstractionBlockFormula(abstractionStates.get(2)).getFormula();
      }
      if (abstractionStates.size() > 4) {
        for (int i = 3; i < abstractionStates.size() - 1; ++i) {
          tailFormula =
              bfmgr.and(
                  tailFormula,
                  getPredicateAbstractionBlockFormula(abstractionStates.get(i)).getFormula());
        }
      }
      // Collect target formulas from each target state
      for (AbstractState targetState : targetStatesAfterLoop) {
        targetFormula =
            bfmgr.or(targetFormula, getPredicateAbstractionBlockFormula(targetState).getFormula());
      }
    }
    return new PartitionedFormulas(
        prefixFormula,
        loopFormula,
        bfmgr.and(tailFormula, targetFormula),
        errorBeforeLoopFormula);
  }

  private static boolean isTargetStateBeforeLoopStart(AbstractState pTargetState) {
    return getAbstractionStatesToRoot(pTargetState).size() == 2;
  }

  private static List<AbstractState> getTargetStatesBeforeLoop(final ReachedSet pReachedSet) {
    return AbstractStates.getTargetStates(pReachedSet)
        .filter(IMCAlgorithm::isTargetStateBeforeLoopStart)
        .toList();
  }

  private static List<AbstractState> getTargetStatesAfterLoop(final ReachedSet pReachedSet) {
    return AbstractStates.getTargetStates(pReachedSet)
        .filter(e -> !isTargetStateBeforeLoopStart(e))
        .toList();
  }

  private static List<ARGState> getAbstractionStatesToRoot(AbstractState pTargetState) {
    return from(ARGUtils.getOnePathTo((ARGState) pTargetState).asStatesList())
        .filter(e -> PredicateAbstractState.containsAbstractionState(e))
        .toList();
  }

  private static PathFormula getPredicateAbstractionBlockFormula(AbstractState pState) {
    return PredicateAbstractState.getPredicateState(pState)
        .getAbstractionFormula()
        .getBlockFormula();
  }

  private PartitionedFormulas collectFormulasBySyntacticLoop(
      final ReachedSet pReachedSet,
      int maxLoopIterations)
      throws InterruptedException {
    PathFormula prefixFormula = getLoopHeadFormula(pReachedSet, 0);
    BooleanFormula loopFormula = bfmgr.makeTrue();
    BooleanFormula tailFormula = bfmgr.makeTrue();
    if (maxLoopIterations > 1) {
      loopFormula = getLoopHeadFormula(pReachedSet, 1).getFormula();
    }
    if (maxLoopIterations > 2) {
      for (int k = 2; k < maxLoopIterations; ++k) {
        tailFormula = bfmgr.and(tailFormula, getLoopHeadFormula(pReachedSet, k).getFormula());
      }
    }
    BooleanFormula suffixFormula =
        bfmgr.and(tailFormula, getErrorFormula(pReachedSet, maxLoopIterations - 1));
    return new PartitionedFormulas(
        prefixFormula,
        loopFormula,
        suffixFormula,
        getErrorFormula(pReachedSet, -1));
  }

  private boolean checkSanityOfFormulas(final PartitionedFormulas formulas)
      throws SolverException, InterruptedException {
    if (!bfmgr.isFalse(formulas.prefixFormula.getFormula())
        && !bfmgr.isTrue(formulas.loopFormula)
        && solver.isUnsat(bfmgr.and(formulas.prefixFormula.getFormula(), formulas.loopFormula))) {
      logger.log(Level.SEVERE, "The conjunction of prefix and loop should not be UNSAT!");
      return false;
    }
    return true;
  }

  /**
   * A helper method to get the block formula at the specified loop head location. Typically it
   * expects zero or one loop head state in ARG with the specified encountering number, because
   * multi-loop programs are excluded in the beginning. In this case, it returns a false block
   * formula if there is no loop head, or the block formula at the unique loop head. However, an
   * exception is caused by the pattern "{@code ERROR: goto ERROR;}". Under this situation, it
   * returns the disjunction of the block formulas to each loop head state.
   *
   * @param pReachedSet Abstract Reachability Graph
   *
   * @param numEncounterLoopHead The encounter times of the loop head location
   *
   * @return The {@code PathFormula} at the specified loop head location if the loop head is unique.
   *
   * @throws InterruptedException On shutdown request.
   *
   */
  private PathFormula getLoopHeadFormula(final ReachedSet pReachedSet, int numEncounterLoopHead)
      throws InterruptedException {
    List<AbstractState> loopHeads =
        getLoopHeadEncounterState(getLoopStart(pReachedSet), numEncounterLoopHead).toList();
    PathFormula formulaToLoopHeads =
        new PathFormula(
            bfmgr.makeFalse(),
            SSAMap.emptySSAMap(),
            PointerTargetSet.emptyPointerTargetSet(),
            0);
    for (AbstractState loopHeadState : loopHeads) {
      formulaToLoopHeads =
          pmgr.makeOr(formulaToLoopHeads, getPredicateAbstractionBlockFormula(loopHeadState));
    }
    return formulaToLoopHeads;
  }

  private static boolean isLoopStart(final AbstractState pState) {
    return AbstractStates.extractStateByType(pState, LocationState.class)
        .getLocationNode()
        .isLoopStart();
  }

  private static FluentIterable<AbstractState> getLoopStart(final ReachedSet pReachedSet) {
    return from(pReachedSet).filter(IMCAlgorithm::isLoopStart);
  }

  private static boolean
      isLoopHeadEncounterTime(final AbstractState pState, int numEncounterLoopHead) {
    return AbstractStates.extractStateByType(pState, LoopBoundState.class).getDeepestIteration()
        - 1 == numEncounterLoopHead;
  }

  private static FluentIterable<AbstractState> getLoopHeadEncounterState(
      final FluentIterable<AbstractState> pFluentIterable,
      final int numEncounterLoopHead) {
    return pFluentIterable.filter(e -> isLoopHeadEncounterTime(e, numEncounterLoopHead));
  }

  /**
   * A helper method to get the block formula at the specified error locations.
   *
   * @param pReachedSet Abstract Reachability Graph
   *
   * @param numEncounterLoopHead The times to encounter LH before reaching the error
   *
   * @return A {@code BooleanFormula} of the disjunction of block formulas at every error location
   *         if they exist; {@code False} if there is no error location with the specified encounter
   *         times.
   *
   */
  private BooleanFormula getErrorFormula(final ReachedSet pReachedSet, int numEncounterLoopHead) {
    return getLoopHeadEncounterState(
        AbstractStates.getTargetStates(pReachedSet),
        numEncounterLoopHead).transform(es -> getPredicateAbstractionBlockFormula(es).getFormula())
            .stream()
            .collect(bfmgr.toDisjunction());
  }

  /**
   * A helper method to derive an interpolant. It computes C=itp(A,B) or C'=!itp(B,A).
   *
   * @param itpProver SMT solver stack
   *
   * @param pFormulaA Formula A (prefix and loop)
   *
   * @param pFormulaB Formula B (suffix)
   *
   * @return A {@code BooleanFormula} interpolant
   *
   * @throws InterruptedException On shutdown request.
   *
   */
  private <T> BooleanFormula getInterpolantFrom(
      InterpolatingProverEnvironment<T> itpProver,
      final List<T> pFormulaA,
      final List<T> pFormulaB)
      throws SolverException, InterruptedException {
    if (deriveInterpolantFromSuffix) {
      logger.log(Level.FINE, "Deriving the interpolant from suffix (formula B) and negate it");
      return bfmgr.not(itpProver.getInterpolant(pFormulaB));
    } else {
      logger.log(Level.FINE, "Deriving the interpolant from prefix and loop (formula A)");
      return itpProver.getInterpolant(pFormulaA);
    }
  }

  /**
   * The method to iteratively compute fixed points by interpolation.
   *
   * @param itpProver the prover with interpolation enabled
   *
   * @return {@code true} if a fixed point is reached, i.e., property is proved; {@code false} if
   *         the current over-approximation is unsafe.
   *
   * @throws InterruptedException On shutdown request.
   *
   */
  private <T> boolean reachFixedPointByInterpolation(
      InterpolatingProverEnvironment<T> itpProver,
      final PartitionedFormulas formulas)
      throws InterruptedException, SolverException {
    BooleanFormula prefixBooleanFormula = formulas.prefixFormula.getFormula();
    SSAMap prefixSsaMap = formulas.prefixFormula.getSsa();
    logger.log(Level.ALL, "The SSA map is", prefixSsaMap);
    BooleanFormula currentImage = bfmgr.makeFalse();
    currentImage = bfmgr.or(currentImage, prefixBooleanFormula);

    List<T> formulaA = new ArrayList<>();
    List<T> formulaB = new ArrayList<>();
    formulaB.add(itpProver.push(formulas.suffixFormula));
    formulaA.add(itpProver.push(formulas.loopFormula));
    formulaA.add(itpProver.push(prefixBooleanFormula));

    while (itpProver.isUnsat()) {
      logger.log(Level.ALL, "The current image is", currentImage);
      BooleanFormula interpolant = getInterpolantFrom(itpProver, formulaA, formulaB);
      logger.log(Level.ALL, "The interpolant is", interpolant);
      interpolant = fmgr.instantiate(fmgr.uninstantiate(interpolant), prefixSsaMap);
      logger.log(Level.ALL, "After changing SSA", interpolant);
      if (solver.implies(interpolant, currentImage)) {
        logger.log(Level.INFO, "The current image reaches a fixed point");
        return true;
      }
      currentImage = bfmgr.or(currentImage, interpolant);
      itpProver.pop();
      formulaA.remove(formulaA.size() - 1);
      formulaA.add(itpProver.push(interpolant));
    }
    logger.log(Level.FINE, "The overapproximation is unsafe, going back to BMC phase");
    return false;
  }

  @Override
  protected CandidateGenerator getCandidateInvariants() {
    throw new AssertionError(
        "Class IMCAlgorithm does not support this function. It should not be called.");
  }

  /**
   * This class wraps three formulas used in IMC algorithm in order to avoid long parameter lists.
   * In addition, it also stores the disjunction of block formulas for the target states before the
   * loop. Therefore, a BMC query is the disjunction of 1) the conjunction of prefix, loop, and
   * suffix and 2) the errors before the loop. Note that prefixFormula is a {@link PathFormula} as
   * we need its {@link SSAMap} to update the SSA indices of derived interpolants.
   */
  private static class PartitionedFormulas {

    private final PathFormula prefixFormula;
    private final BooleanFormula loopFormula;
    private final BooleanFormula suffixFormula;
    private final BooleanFormula errorBeforeLoopFormula;

    public PartitionedFormulas(
        PathFormula pPrefixFormula,
        BooleanFormula pLoopFormula,
        BooleanFormula pSuffixFormula,
        BooleanFormula pErrorBeforeLoopFormula) {
      prefixFormula = pPrefixFormula;
      loopFormula = pLoopFormula;
      suffixFormula = pSuffixFormula;
      errorBeforeLoopFormula = pErrorBeforeLoopFormula;
    }
  }
}
