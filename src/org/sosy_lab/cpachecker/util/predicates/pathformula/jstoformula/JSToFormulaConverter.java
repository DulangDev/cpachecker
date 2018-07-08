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
package org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.JSToFormulaTypeUtils.areEqualWithMatchingPointerArray;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.JSToFormulaTypeUtils.getRealFieldOwner;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSAssignment;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSInitializers;
import org.sosy_lab.cpachecker.cfa.ast.js.JSLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.js.JSRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.js.JSRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.js.JSStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.js.JSAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.js.JSDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.js.JSStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.cfa.types.js.JSAnyType;
import org.sosy_lab.cpachecker.cfa.types.js.JSType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedJSCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMapMerger.MergeResult;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder.DummyPointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FloatingPointFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FunctionFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassificationBuilder;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FloatingPointRoundingMode;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

/**
 * Class containing all the code that converts C code into a formula.
 */
public class JSToFormulaConverter {

  // list of functions that are pure (no side-effects from the perspective of this analysis)
  static final ImmutableSet<String> PURE_EXTERNAL_FUNCTIONS =
      ImmutableSet.of(
          "abort",
          "exit",
          "__assert_fail",
          "__VERIFIER_error",
          "free",
          "kfree",
          "fprintf",
          "printf",
          "puts",
          "printk",
          "sprintf",
          "swprintf",
          "strcasecmp",
          "strchr",
          "strcmp",
          "strlen",
          "strncmp",
          "strrchr",
          "strstr");

  // set of functions that may not appear in the source code
  // the value of the map entry is the explanation for the user
  static final ImmutableMap<String, String> UNSUPPORTED_FUNCTIONS =
      ImmutableMap.of("fesetround", "floating-point rounding modes");

  //names for special variables needed to deal with functions
  @Deprecated
  private static final String RETURN_VARIABLE_NAME =
      VariableClassificationBuilder.FUNCTION_RETURN_VARIABLE;
  public static final String PARAM_VARIABLE_NAME = "__param__";

  private static final ImmutableSet<String> SAFE_VAR_ARG_FUNCTIONS = ImmutableSet.of(
      "printf", "printk"
      );

  private static final CharMatcher ILLEGAL_VARNAME_CHARACTERS = CharMatcher.anyOf("|\\");

  private final Map<String, Formula> stringLitToFormula = new HashMap<>();
  final TypedValues typedValues;
  final TypeTags typeTags;
  final TypedValueManager tvmgr;
  private int nextStringLitIndex = 0;

  final FormulaEncodingOptions options;
  protected final MachineModel machineModel;
  private final Optional<VariableClassification> variableClassification;
  final JSToFormulaTypeHandler typeHandler;

  protected final FormulaManagerView fmgr;
  protected final BooleanFormulaManagerView bfmgr;
  private final IntegerFormulaManagerView nfmgr;
  private final BitvectorFormulaManagerView efmgr;
  final FunctionFormulaManagerView ffmgr;
  protected final LogManagerWithoutDuplicates logger;
  protected final ShutdownNotifier shutdownNotifier;

  protected final AnalysisDirection direction;

  // Index that is used to read from variables that were not assigned yet
  private static final int VARIABLE_UNINITIALIZED = 1;

  // Index to be used for first assignment to a variable (must be higher than VARIABLE_UNINITIALIZED!)
  private static final int VARIABLE_FIRST_ASSIGNMENT = 2;

  private final FunctionDeclaration<?> stringUfDecl;

  protected final Set<CVariableDeclaration> globalDeclarations = new HashSet<>();
  FloatingPointFormulaManagerView fpfmgr;

  public JSToFormulaConverter(FormulaEncodingOptions pOptions, FormulaManagerView pFmgr,
                              MachineModel pMachineModel, Optional<VariableClassification> pVariableClassification,
                              LogManager pLogger, ShutdownNotifier pShutdownNotifier,
                              JSToFormulaTypeHandler pTypeHandler, AnalysisDirection pDirection) {

    this.fmgr = pFmgr;
    this.options = pOptions;
    this.machineModel = pMachineModel;
    this.variableClassification = pVariableClassification;
    this.typeHandler = pTypeHandler;

    this.bfmgr = pFmgr.getBooleanFormulaManager();
    this.nfmgr = pFmgr.getIntegerFormulaManager(); // NumeralMgr is only used for String-Literals, so Int or Real does not matter, however Princess only supports Int.
    this.efmgr = pFmgr.getBitvectorFormulaManager();
    this.ffmgr = pFmgr.getFunctionFormulaManager();
    this.fpfmgr = pFmgr.getFloatingPointFormulaManager();
    this.logger = new LogManagerWithoutDuplicates(pLogger);
    this.shutdownNotifier = pShutdownNotifier;

    this.direction = pDirection;

    stringUfDecl = ffmgr.declareUF(
            "__string__", typeHandler.getPointerType(), FormulaType.IntegerType);

    typedValues = new TypedValues(ffmgr);
    typeTags = new TypeTags(nfmgr);
    tvmgr = new TypedValueManager(typedValues, typeTags);
  }

  void logfOnce(Level level, CFAEdge edge, String msg, Object... args) {
    if (logger.wouldBeLogged(level)) {
      logger.logfOnce(level, "%s: %s: %s",
          edge.getFileLocation(),
          String.format(msg, args),
          edge.getDescription());
    }
  }

  /**
   * Returns the size in bits of the given type. Always use this method instead of
   * machineModel.getSizeOf, because this method can handle dereference-types.
   *
   * @param pType the type to calculate the size of.
   * @return the size in bits of the given type.
   */
  protected int getBitSizeof(CType pType) {
    return typeHandler.getBitSizeof(pType);
  }

  /**
   * Returns the size in bytes of the given type.
   * Always use this method instead of machineModel.getSizeOf,
   * because this method can handle dereference-types.
   * @param pType the type to calculate the size of.
   * @return the size in bytes of the given type.
   */
  protected int getSizeof(CType pType) {
    return typeHandler.getSizeof(pType);
  }

  protected boolean isRelevantField(final CCompositeType compositeType,
                          final String fieldName) {
    return !variableClassification.isPresent() ||
           !options.ignoreIrrelevantVariables() ||
           !options.ignoreIrrelevantFields() ||
           variableClassification.get().getRelevantFields().containsEntry(compositeType, fieldName);
  }

  protected boolean isRelevantLeftHandSide(final CLeftHandSide lhs) {
    if (!options.trackFunctionPointers() && CTypes.isFunctionPointer(lhs.getExpressionType())) {
      return false;
    }

    if (options.useHavocAbstraction()) {
      if (!lhs.accept(new IsRelevantWithHavocAbstractionVisitor(this))) {
        return false;
      }
    }

    if (options.ignoreIrrelevantVariables() && variableClassification.isPresent()) {
      return lhs.accept(new IsRelevantLhsVisitor(this));
    } else {
      return true;
    }
  }

  protected final boolean isRelevantVariable(final CSimpleDeclaration var) {
    if (options.useHavocAbstraction()) {
      if (var instanceof CVariableDeclaration) {
        CVariableDeclaration vDecl = (CVariableDeclaration) var;
        if (vDecl.isGlobal()) {
          return false;
        } else if (vDecl.getType() instanceof CPointerType) {
          return false;
        }
      }
    }
    if (options.ignoreIrrelevantVariables() && variableClassification.isPresent()) {
      boolean isRelevantVariable =
          var.getName().equals(RETURN_VARIABLE_NAME)
              || variableClassification
                  .get()
                  .getRelevantVariables()
                  .contains(var.getQualifiedName());
      if (options.overflowVariablesAreRelevant()) {
        isRelevantVariable |=
            variableClassification.get().getIntOverflowVars().contains(var.getQualifiedName());
      }
      return isRelevantVariable;
    }
    return true;
  }

  public final FormulaType<?> getFormulaTypeFromCType(JSType type) {
    return Types.VARIABLE_TYPE;
  }

  public final FormulaType<?> getFormulaTypeFromCType(CType type) {
    if (type instanceof CSimpleType) {
      CSimpleType simpleType = (CSimpleType) type;
      switch (simpleType.getType()) {
        case FLOAT:
          return FormulaType.getSinglePrecisionFloatingPointType();
        case DOUBLE:
          return FormulaType.getDoublePrecisionFloatingPointType();
        default:
          break;
      }
    }

    int bitSize = typeHandler.getBitSizeof(type);

    return FormulaType.getBitvectorTypeWithSize(bitSize);
  }

  /**
   * This method produces a String representation of an arbitrary expression
   * that can be used as a variable name in a formula.
   * The name is not globally unique.
   *
   * @param e the expression which should be named
   * @return the name of the expression
   */
  static String exprToVarNameUnscoped(AAstNode e) {
    return ILLEGAL_VARNAME_CHARACTERS.replaceFrom(
        CharMatcher.whitespace().removeFrom(e.toASTString()), '_');
  }

  /**
   * This method produces a String representation of an arbitrary expression
   * that can be used as a variable name in a formula.
   * The name is local to the given function.
   *
   * @param e the expression which should be named
   * @return the name of the expression
   */
  static String exprToVarName(AAstNode e, String function) {
    return (function + "::" + exprToVarNameUnscoped(e)).intern().intern();
  }

  /** Produces a fresh new SSA index for an assignment and updates the SSA map. */
  protected int makeFreshIndex(String name, JSType type, SSAMapBuilder ssa) {
    int idx = getFreshIndex(name, type, ssa);
    ssa.setIndex(name, type, idx);
    return idx;
  }

  /**
   * Produces a fresh new SSA index for an assignment,
   * but does _not_ update the SSA map.
   * Usually you should use {@link #makeFreshIndex(String, JSType, SSAMapBuilder)}
   * instead, because using variables with indices that are not stored in the SSAMap
   * is not a good idea (c.f. the comment inside getIndex()).
   * If you use this method, you need to make sure to update the SSAMap correctly.
   */
  protected int getFreshIndex(String name, JSType type, SSAMapBuilder ssa) {
//    checkSsaSavedType(name, type, ssa.getType(name));
    int idx = ssa.getFreshIndex(name);
    if (idx <= 0) {
      idx = VARIABLE_FIRST_ASSIGNMENT;
    }
    return idx;
  }

  /**
   * This method returns the index of the given variable in the ssa map, if there
   * is none, it creates one with the value 1.
   *
   * @return the index of the variable
   */
  protected int getIndex(String name, JSType type, SSAMapBuilder ssa) {
//    checkSsaSavedType(name, type, ssa.getType(name));
    int idx = ssa.getIndex(name);
    if (idx <= 0) {
      logger.log(Level.ALL, "WARNING: Auto-instantiating variable:", name);
      idx = VARIABLE_UNINITIALIZED;

      // It is important to store the index in the variable here.
      // If getIndex() was called with a specific name,
      // this means that name@idx will appear in formulas.
      // Thus we need to make sure that calls to FormulaManagerView.instantiate()
      // will also add indices for this name,
      // which it does exactly if the name is in the SSAMap.
      ssa.setIndex(name, type, idx);
    }

    return idx;
  }

  protected void checkSsaSavedType(String name, CType type, CType t) {

    // Check if types match

    // Assert when a variable already exists, that it has the same type
    // TODO: Un-comment when parser and code-base is stable enough
//    Variable t;
//    assert
//         (t = ssa.getType(name)) == null
//      || CTypeUtils.equals(t, type)
//      : "Saving variables with mutliple types is not possible!";
    if (t != null && !areEqualWithMatchingPointerArray(t, type)) {

      if (!getFormulaTypeFromCType(t).equals(getFormulaTypeFromCType(type))) {
        throw new UnsupportedOperationException(
            "Variable " + name + " used with types of different sizes! " +
                "(Type1: " + t + ", Type2: " + type + ")");
      } else {
        logger.logf(Level.FINEST, "Variable %s was found with multiple types!"
                + " (Type1: %s, Type2: %s)", name, t, type);
      }
    }
  }

  /**
   * Create the necessary equivalence terms for adjusting the SSA indices
   * of a given symbol (of any type) from oldIndex to newIndex.
   *
   * @param variableName The name of the variable for which the index is adjusted.
   * @param variableType The type of the variable.
   * @param oldIndex The previous SSA index.
   * @param newIndex The new SSA index.
   * @param pts The previous PointerTargetSet.
   * @throws InterruptedException If execution is interrupted.
   */
  public BooleanFormula makeSsaUpdateTerm(
      final String variableName,
      final CType variableType,
      final int oldIndex,
      final int newIndex,
      final PointerTargetSet pts)
      throws InterruptedException {
    checkArgument(oldIndex > 0 && newIndex > oldIndex);

    final FormulaType<?> variableFormulaType = getFormulaTypeFromCType(variableType);
    final Formula oldVariable = fmgr.makeVariable(variableFormulaType, variableName, oldIndex);
    final Formula newVariable = fmgr.makeVariable(variableFormulaType, variableName, newIndex);

    return fmgr.assignment(newVariable, oldVariable);
  }

  /**
   * Create a formula for a given variable, which is assumed to be constant.
   * This method does not handle scoping!
   */
  protected Formula makeConstant(String name, CType type) {
    return fmgr.makeVariableWithoutSSAIndex(getFormulaTypeFromCType(type), name);
  }

  /**
   * Create a formula for a given variable. This method does not handle scoping and the
   * NON_DET_VARIABLE!
   *
   * <p>This method does not update the index of the variable.
   */
  protected IntegerFormula makeVariable(String name, JSType type, SSAMapBuilder ssa) {
    int useIndex = getIndex(name, type, ssa);
    return fmgr.makeVariable(Types.VARIABLE_TYPE, name, useIndex);
  }

  /**
   * Takes a variable name and its type and create the corresponding formula out of it. The
   * <code>pContextSSA</code> is used to supply this method with the necessary {@link SSAMap}
   * and (if necessary) the {@link PointerTargetSet} can be supplied via <code>pContextPTS</code>.
   *
   * @param pContextSSA the SSAMap indices from which the variable should be created
   * @param pContextPTS the PointerTargetSet which should be used for formula generation
   * @param pVarName the name of the variable
   * @param pType the type of the variable
   * @param forcePointerDereference (only used in CToFormulaConverterWithPointerAliasing)
   * @return the created formula
   */
  public Formula makeFormulaForVariable(
      SSAMap pContextSSA,
      PointerTargetSet pContextPTS,
      String pVarName,
      JSType pType,
      boolean forcePointerDereference) {
//    Preconditions.checkArgument(!(pType instanceof CEnumType));

    SSAMapBuilder ssa = pContextSSA.builder();
    Formula formula = makeVariable(pVarName, pType, ssa);

    if (!ssa.build().equals(pContextSSA)) {
      throw new IllegalArgumentException(
          "we cannot apply the SSAMap changes to the point where the"
              + " information would be needed possible problems: uninitialized variables could be"
              + " in more formulas which get conjuncted and then we get unsatisfiable formulas as a result");
    }

    return formula;
  }

  /**
   * Create a formula for a given variable with a fresh index for the left-hand
   * side of an assignment.
   * This method does not handle scoping and the NON_DET_VARIABLE!
   */
  protected Formula makeFreshVariable(String name, JSType type, SSAMapBuilder ssa) {
    int useIndex;

    if (direction == AnalysisDirection.BACKWARD) {
      useIndex = getIndex(name, type, ssa);
    } else {
      useIndex = makeFreshIndex(name, type, ssa);
    }

    Formula result = fmgr.makeVariable(this.getFormulaTypeFromCType(type), name, useIndex);

    if (direction == AnalysisDirection.BACKWARD) {
      makeFreshIndex(name, type, ssa);
    }

    return result;
  }

  private IntegerFormula makeFreshVariable(final String name, final SSAMapBuilder ssa) {
    int useIndex;

    if (direction == AnalysisDirection.BACKWARD) {
      useIndex = getIndex(name, JSAnyType.ANY, ssa);
    } else {
      useIndex = makeFreshIndex(name, JSAnyType.ANY, ssa);
    }

    IntegerFormula result = fmgr.makeVariable(Types.VARIABLE_TYPE, name, useIndex);

    if (direction == AnalysisDirection.BACKWARD) {
      makeFreshIndex(name, JSAnyType.ANY, ssa);
    }

    return result;
  }

  protected Formula makeNondet(
      final String name, final JSType type, final SSAMapBuilder ssa, final Constraints
      constraints) {
    Formula newVariable = makeFreshVariable(name, type, ssa);
//    if (options.addRangeConstraintsForNondet()) {
//      addRangeConstraint(newVariable, type, constraints);
//    }
    return newVariable;
  }

  Formula makeStringLiteral(String literal) {
    Formula result = stringLitToFormula.get(literal);

    if (result == null) {
      // generate a new string literal. We generate a new UIf
      int n = nextStringLitIndex++;
      result = ffmgr.callUF(
          stringUfDecl, nfmgr.makeNumber(n));
      stringLitToFormula.put(literal, result);
    }

    return result;
  }

  /**
   * Create a formula that reinterprets the raw bit values as a different type Returns {@code null}
   * if this is not implemented for the given types. Both types need to have the same size
   */
  protected @Nullable Formula makeValueReinterpretation(
      final CType pFromType, final CType pToType, Formula formula) {
    CType fromType = handlePointerAndEnumAsInt(pFromType);
    CType toType = handlePointerAndEnumAsInt(pToType);

    FormulaType<?> fromFormulaType = getFormulaTypeFromCType(fromType);
    FormulaType<?> toFormulaType = getFormulaTypeFromCType(toType);

    if (fromFormulaType.isBitvectorType() && toFormulaType.isFloatingPointType()) {
      int sourceSize = ((BitvectorType) fromFormulaType).getSize();
      int targetSize = ((FloatingPointType) toFormulaType).getTotalSize();

      if (sourceSize > targetSize) {
        formula =
            fmgr.getBitvectorFormulaManager()
                .extract((BitvectorFormula) formula, targetSize - 1, 0, false);
      } else if (sourceSize < targetSize) {
        return null; // TODO extend with nondet bits
      }

      verify(
          fmgr.getFormulaType(formula).equals(FormulaType.getBitvectorTypeWithSize(targetSize)),
          "Unexpected result type for %s",
          formula);

      return fpfmgr.fromIeeeBitvector(
          (BitvectorFormula) formula, (FloatingPointType) toFormulaType);

    } else if (fromFormulaType.isFloatingPointType() && toFormulaType.isBitvectorType()) {
      int sourceSize = ((FloatingPointType) fromFormulaType).getTotalSize();
      int targetSize = ((BitvectorType) toFormulaType).getSize();

      formula = fpfmgr.toIeeeBitvector((FloatingPointFormula) formula);

      if (sourceSize > targetSize) {
        formula =
            fmgr.getBitvectorFormulaManager()
                .extract((BitvectorFormula) formula, targetSize - 1, 0, false);
      } else if (sourceSize < targetSize) {
        return null; // TODO extend with nondet bits
      }

      verify(
          fmgr.getFormulaType(formula).equals(toFormulaType),
          "Unexpected result type %s for %s",
          fmgr.getFormulaType(formula),
          formula);

      return formula;

    } else {
      return null; // TODO use UF
    }
  }

  /**
   * Used for implicit and explicit type casts between CTypes. Optionally, overflows can be replaced
   * with UFs.
   *
   * @param pFromType the origin Type of the expression.
   * @param pToType the type to cast into.
   * @param formula the formula of the expression.
   * @return the new formula after the cast.
   */
  protected Formula makeCast(
      final CType pFromType,
      final CType pToType,
      Formula formula,
      Constraints constraints,
      CFAEdge edge)
      throws UnrecognizedCCodeException {
    Formula result = makeCast0(pFromType, pToType, formula, edge);

    if (options.encodeOverflowsWithUFs()) {
      // handles arithmetic overflows like  "x+y>MAX"  or  "x-y<MIN"  .
      // and also type-based overflows like  "char c = (int)i;"  or  "unsigned int j = (int)i;"  .
      result = encodeOverflowsWithUF(result, pToType, constraints);
    }

    return result;
  }

  /** Replace the formula with a matching ITE-structure
   *  that returns an UF (with additional constraints), if the formula causes an overflow,
   *  else the formula itself.
   *  Example:  ITE( MIN_INT <= X <= MAX_INT, X, UF(X) )  */
  private Formula encodeOverflowsWithUF(final Formula value, CType type, final Constraints constraints) {
    type = type.getCanonicalType();
    if (type instanceof CSimpleType && ((CSimpleType)type).getType().isIntegerType()) {
      final CSimpleType sType = (CSimpleType)type;
      final FormulaType<Formula> numberType = fmgr.getFormulaType(value);
      final boolean signed = machineModel.isSigned(sType);

      final Formula lowerBound =
          fmgr.makeNumber(numberType, machineModel.getMinimalIntegerValue(sType));
      final Formula upperBound =
          fmgr.makeNumber(numberType, machineModel.getMaximalIntegerValue(sType));

      BooleanFormula range = fmgr.makeRangeConstraint(value, lowerBound, upperBound, signed);

      // simplify constant formulas like "1<=2" and return the value directly.
      // benefit: divide_by_constant works without UFs
      try {
        range = fmgr.simplify(range);
      } catch (InterruptedException pE) {
        throw propagateInterruptedException(pE);
      }
      if (bfmgr.isTrue(range)) {
        return value;
      }

      // UF-string-format copied from ReplaceBitvectorWithNumeralAndFunctionTheory.getUFDecl
      final String ufName =
          String.format(
              "_%s%s(%d)_",
              "overflow",
              (signed ? "Signed" : "Unsigned"),
              machineModel.getSizeofInBits(sType));
      final Formula overflowUF = ffmgr.declareAndCallUF(ufName, numberType, value);
      addRangeConstraint(overflowUF, type, constraints);

      // TODO improvement:
      // Add special handling for a constant number of overflows (N=1 or N=2).
      // This would allow to catch overflows from ADD and SUBTRACT.

      // if (value in [MIN,MAX])   then return (value)  else return UF(value)
      return bfmgr.ifThenElse(range, value, overflowUF);

    } else {
      return value;
    }
  }

  /**
   * Add constraint for the interval of possible values, This method should only be used for a
   * previously declared variable, otherwise the SSA-index is invalid. Example: MIN_INT <= X <=
   * MAX_INT
   */
  private void addRangeConstraint(final Formula variable, CType type, Constraints constraints) {
    type = type.getCanonicalType();
    if (type instanceof CSimpleType && ((CSimpleType)type).getType().isIntegerType()) {
      final CSimpleType sType = (CSimpleType)type;
      final FormulaType<Formula> numberType = fmgr.getFormulaType(variable);
      final boolean signed = machineModel.isSigned(sType);
      final Formula lowerBound =
          fmgr.makeNumber(numberType, machineModel.getMinimalIntegerValue(sType));
      final Formula upperBound =
          fmgr.makeNumber(numberType, machineModel.getMaximalIntegerValue(sType));
      constraints.addConstraint(fmgr.makeRangeConstraint(variable, lowerBound, upperBound, signed));
    }
  }

  /**
   * Used for implicit and explicit type casts between CTypes.
   * @param pFromType the origin Type of the expression.
   * @param pToType the type to cast into.
   * @param formula the formula of the expression.
   * @return the new formula after the cast.
   */
  private Formula makeCast0(final CType pFromType, final CType pToType,
      Formula formula, CFAEdge edge) throws UnrecognizedCCodeException {
    // UNDEFINED: Casting a numeric value into a value that can't be represented by the target type (either directly or via static_cast)

    CType fromType = pFromType.getCanonicalType();
    CType toType = pToType.getCanonicalType();

    if (fromType.equals(toType)) {
      return formula; // No cast required;
    }

    if (fromType instanceof CFunctionType) {
      // references to functions can be seen as function pointers
      fromType = new CPointerType(false, false, fromType);
    }

    fromType = handlePointerAndEnumAsInt(fromType);
    toType = handlePointerAndEnumAsInt(toType);

    if (isSimple(fromType) && isSimple(toType)) {
      return makeSimpleCast(fromType, toType, formula);
    }

    if (fromType instanceof CPointerType ||
        toType instanceof CPointerType) {
      // Ignore casts between Pointer and right sized types
      if (getFormulaTypeFromCType(toType).equals(getFormulaTypeFromCType(fromType))) {
        return formula;
      }
    }

    if (getBitSizeof(fromType) == getBitSizeof(toType)) {
      // We can most likely just ignore this cast
      logger.logfOnce(Level.WARNING, "Ignoring cast from %s to %s.", fromType, toType);
      return formula;
    } else {
      throw new UnrecognizedCCodeException("Cast from " + pFromType + " to " + pToType + " not supported!", edge);
    }
  }

  private CType handlePointerAndEnumAsInt(CType pType) {
    if (pType instanceof CBitFieldType) {
      CBitFieldType type = (CBitFieldType) pType;
      CType innerType = type.getType();
      CType normalizedInnerType = handlePointerAndEnumAsInt(innerType);
      if (innerType == normalizedInnerType) {
        return pType;
      }
      return new CBitFieldType(normalizedInnerType, type.getBitFieldSize());
    }
    if (pType instanceof CPointerType) {
      return machineModel.getPointerEquivalentSimpleType();
    }
    if (pType instanceof CEnumType
        || (pType instanceof CElaboratedType && ((CElaboratedType) pType).getKind() == ComplexTypeKind.ENUM)) {
      return CNumericTypes.INT;
    }
    return pType;
  }

  protected CExpression makeCastFromArrayToPointerIfNecessary(CExpression exp, CType targetType) {
    if (exp.getExpressionType().getCanonicalType() instanceof CArrayType) {
      targetType = targetType.getCanonicalType();
      if (targetType instanceof CPointerType || targetType instanceof CSimpleType) {
        return makeCastFromArrayToPointer(exp);
      }
    }
    return exp;
  }

  private static CExpression makeCastFromArrayToPointer(CExpression arrayExpression) {
    // array-to-pointer conversion
    CArrayType arrayType = (CArrayType)arrayExpression.getExpressionType().getCanonicalType();
    CPointerType pointerType = new CPointerType(arrayType.isConst(),
        arrayType.isVolatile(), arrayType.getType());

    return new CUnaryExpression(arrayExpression.getFileLocation(), pointerType,
        arrayExpression, UnaryOperator.AMPER);
  }

  /**
   * Handles casts between simple types.
   * When the fromType is a signed type a bit-extension will be done,
   * on any other case it will be filled with 0 bits.
   */
  private Formula makeSimpleCast(CType pFromCType, CType pToCType, Formula pFormula) {
    checkSimpleCastArgument(pFromCType);
    checkSimpleCastArgument(pToCType);
    Predicate<CType> isSigned = t -> {
      if (t instanceof CSimpleType) {
        return machineModel.isSigned((CSimpleType) t);
      }
      if (t instanceof CBitFieldType) {
        CBitFieldType bitFieldType = (CBitFieldType) t;
        if (bitFieldType.getType() instanceof CSimpleType) {
          return machineModel.isSigned(((CSimpleType) bitFieldType.getType()));
        }
      }
      throw new AssertionError("Not a simple type: " + t);
    };

    final FormulaType<?> fromType = getFormulaTypeFromCType(pFromCType);
    final FormulaType<?> toType = getFormulaTypeFromCType(pToCType);

    final Formula ret;
    if (fromType.equals(toType)) {
      ret = pFormula;

    } else if (fromType.isBitvectorType() && toType.isBitvectorType()) {
      int toSize = ((BitvectorType)toType).getSize();
      int fromSize = ((BitvectorType) fromType).getSize();

      // Cf. C-Standard 6.3.1.2 (1)
      if (pToCType.getCanonicalType().equals(CNumericTypes.BOOL)) {
        Formula zeroFromSize = efmgr.makeBitvector(fromSize, 0l);
        Formula zeroToSize = efmgr.makeBitvector(toSize, 0l);
        Formula oneToSize = efmgr.makeBitvector(toSize, 1l);

        ret = bfmgr.ifThenElse(fmgr.makeEqual(zeroFromSize, pFormula), zeroToSize, oneToSize);
      } else {
        if (fromSize > toSize) {
          ret = fmgr.makeExtract(pFormula, toSize - 1, 0, isSigned.test(pFromCType));

        } else if (fromSize < toSize) {
          ret = fmgr.makeExtend(pFormula, (toSize - fromSize), isSigned.test(pFromCType));

        } else {
          ret = pFormula;
        }
      }
    } else if (fromType.isFloatingPointType()) {
      if (toType.isFloatingPointType()) {
        ret = fpfmgr.castTo((FloatingPointFormula) pFormula, toType);
      } else {
        // Cf. C-Standard 6.3.1.4 (1).
        ret =
            fpfmgr.castTo(
                (FloatingPointFormula) pFormula, toType, FloatingPointRoundingMode.TOWARD_ZERO);
      }

    } else if (toType.isFloatingPointType()) {
      ret = fpfmgr.castFrom(pFormula, isSigned.test(pFromCType), (FloatingPointType) toType);

    } else {
      throw new IllegalArgumentException("Cast from " + pFromCType + " to " + pToCType
          + " needs theory conversion between " + fromType + " and " + toType);
    }

    assert fmgr.getFormulaType(ret).equals(toType) : "types do not match: " + fmgr.getFormulaType(ret) + " vs " + toType;
    return ret;
  }

  private void checkSimpleCastArgument(CType pType) {
    if (!isSimple(pType)) {
      throw new IllegalArgumentException("Cannot make a simple cast from or to " + pType);
    }
  }

  private boolean isSimple(CType pType) {
    if (pType instanceof CSimpleType) {
      return true;
    }
    if (pType instanceof CBitFieldType) {
      CBitFieldType type = (CBitFieldType) pType;
      if (type.getType() instanceof CSimpleType) {
        return true;
      }
    }
    return false;
  }

  /**
   * If the given expression is a integer literal, and the given type is a floating-point type,
   * convert the literal into a floating-point literal. Otherwise return the expression unchanged.
   */
  protected CExpression convertLiteralToFloatIfNecessary(
      final CExpression pExp, final CType targetType) {
    if (!isFloatingPointType(targetType)) {
      return pExp;
    }
    CExpression e = pExp;

    boolean negative = false;
    if (e instanceof CUnaryExpression
        && ((CUnaryExpression)e).getOperator() == UnaryOperator.MINUS) {
      e = ((CUnaryExpression)e).getOperand();
      negative = true;
    }

    if (e instanceof CIntegerLiteralExpression) {
      NumericValue intValue = new NumericValue(((CIntegerLiteralExpression)e).getValue());
      if (negative) {
        intValue = intValue.negate();
      }
      Value floatValue = AbstractExpressionValueVisitor.castCValue(
          intValue, targetType, machineModel,
          logger, e.getFileLocation());
      return new CFloatLiteralExpression(e.getFileLocation(), targetType,
          floatValue.asNumericValue().bigDecimalValue());
    }

    return pExp;
  }

  private static boolean isFloatingPointType(final CType pType) {
    if (pType instanceof CSimpleType) {
      return ((CSimpleType)pType).getType().isFloatingPointType();
    }
    return false;
  }


//  @Override
  public PathFormula makeAnd(PathFormula oldFormula,
      CFAEdge edge, ErrorConditions errorConditions)
      throws UnrecognizedCCodeException, UnrecognizedCFAEdgeException, InterruptedException,
             UnrecognizedJSCodeException {

    String function = (edge.getPredecessor() != null)
                          ? edge.getPredecessor().getFunctionName() : null;

    SSAMapBuilder ssa = oldFormula.getSsa().builder();
    Constraints constraints = new Constraints(bfmgr);
    PointerTargetSetBuilder pts = createPointerTargetSetBuilder(oldFormula.getPointerTargetSet());

    // param-constraints must be added _before_ handling the edge (some lines below),
    // because this edge could write a global value.
    if (edge.getPredecessor() instanceof CFunctionEntryNode) {
      final CFunctionEntryNode entryNode = (CFunctionEntryNode) edge.getPredecessor();
      addParameterConstraints(edge, function, ssa, pts, constraints, errorConditions, entryNode);
      addGlobalAssignmentConstraints(edge, function, ssa, pts, constraints, errorConditions, PARAM_VARIABLE_NAME, false);

      if (entryNode.getNumEnteringEdges() == 0) {
        handleEntryFunctionParameters(entryNode, ssa, constraints);
      }
    }

    // handle the edge
    BooleanFormula edgeFormula = createFormulaForEdge(edge, function, ssa, pts, constraints, errorConditions);

    // result-constraints must be added _after_ handling the edge (some lines above),
    // because this edge could write a global value.
    if (edge.getSuccessor() instanceof FunctionExitNode) {
      addGlobalAssignmentConstraints(edge, function, ssa, pts, constraints, errorConditions, RETURN_VARIABLE_NAME, true);
    }

    edgeFormula = bfmgr.and(edgeFormula, constraints.get());

    SSAMap newSsa = ssa.build();
    PointerTargetSet newPts = pts.build();

    if (bfmgr.isTrue(edgeFormula)
        && (newSsa == oldFormula.getSsa())
        && newPts.equals(oldFormula.getPointerTargetSet())) {
      // formula is just "true" and rest is equal
      // i.e. no writes to SSAMap, no branching and length should stay the same
      return oldFormula;
    }

    BooleanFormula newFormula = bfmgr.and(oldFormula.getFormula(), edgeFormula);
    int newLength = oldFormula.getLength() + 1;
    return new PathFormula(newFormula, newSsa, newPts, newLength);
  }

  /**
   * Ensure parameters of entry function are added to the SSAMap.
   * Otherwise they would be missing and (un)instantiate would not work correctly,
   * leading to a wrong analysis if their value is relevant.
   * TODO: This would be also necessary when the analysis starts in the middle of a CFA.
   *
   * Also add range constraints for these non-deterministic parameters to strengthen analysis.
   */
  private void handleEntryFunctionParameters(
      final CFunctionEntryNode entryNode, final SSAMapBuilder ssa, final Constraints constraints) {
    for (CParameterDeclaration param : entryNode.getFunctionDefinition().getParameters()) {
      // has side-effect of adding to SSAMap!
//      final Formula var =
//          makeFreshVariable(
//              param.getQualifiedName(), CTypes.adjustFunctionOrArrayType(param.getType()), ssa);
//
//      if (options.addRangeConstraintsForNondet()) {
//        addRangeConstraint(var, param.getType(), constraints);
//      }
    }
  }

  /**
   * Create and add constraints about parameters: param1=tmp_param1, param2=tmp_param2, ...
   * The tmp-variables are also used before the function-entry as "argument-constraints".
   *
   * This function is usually only relevant with options.useParameterVariables().
   */
  private void addParameterConstraints(final CFAEdge edge, final String function,
                                       final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
                                       final Constraints constraints, final ErrorConditions errorConditions,
                                       final CFunctionEntryNode entryNode)
          throws UnrecognizedCCodeException, InterruptedException {

    if (options.useParameterVariables()) {
      for (CParameterDeclaration formalParam : entryNode.getFunctionParameters()) {

        // create expressions for each formal param: "f::x" --> "f::x__param__"
        CParameterDeclaration tmpParameterExpression = new CParameterDeclaration(
                formalParam.getFileLocation(), formalParam.getType(), formalParam.getName() + PARAM_VARIABLE_NAME);
        tmpParameterExpression.setQualifiedName(formalParam.getQualifiedName() + PARAM_VARIABLE_NAME);

        CIdExpression lhs = new CIdExpression(formalParam.getFileLocation(), formalParam);
        CIdExpression rhs = new CIdExpression(formalParam.getFileLocation(), tmpParameterExpression);

        // add assignment to constraints: "f::x" = "f::x__param__"
//        BooleanFormula eq = makeAssignment(lhs, rhs, edge, function, ssa, pts, constraints, errorConditions);
//        constraints.addConstraint(eq);
        throw new RuntimeException("Not implemented");
      }
    }
  }

  /** this function is only executed, if the option useParameterVariablesForGlobals is used,
   * otherwise it does nothing.
   * create and add constraints about a global variable: tmp_1_f==global1, tmp_2_f==global2, ...
   * @param tmpAsLHS if tmpAsLHS:  tmp_result1_f := global1
   *                 else          global1       := tmp_result1_f
   */
  private void addGlobalAssignmentConstraints(final CFAEdge edge, final String function,
                                              final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
                                              final Constraints constraints, final ErrorConditions errorConditions,
                                              final String tmpNamePart, final boolean tmpAsLHS)
          throws UnrecognizedCCodeException, InterruptedException {

    if (options.useParameterVariablesForGlobals()) {

      // make assignments: tmp_param1_f==global1, tmp_param2_f==global2, ...
      // function-name is important, because otherwise the name is not unique over several function-calls.
      for (final CVariableDeclaration decl : globalDeclarations) {
        final CParameterDeclaration tmpParameter = new CParameterDeclaration(
                decl.getFileLocation(), decl.getType(), decl.getName() + tmpNamePart + function);
        tmpParameter.setQualifiedName(decl.getQualifiedName() + tmpNamePart + function);

        final CIdExpression tmp = new CIdExpression(decl.getFileLocation(), tmpParameter);
        final CIdExpression glob = new CIdExpression(decl.getFileLocation(), decl);

//        final BooleanFormula eq;
//        if (tmpAsLHS) {
//          eq = makeAssignment(tmp, glob, glob, edge, function, ssa, pts, constraints, errorConditions);
//        } else {
//          eq = makeAssignment(glob, glob, tmp, edge, function, ssa, pts, constraints, errorConditions);
//        }
//        constraints.addConstraint(eq);
        throw new RuntimeException("Not implemented");
      }

    }
  }

  /**
   * This helper method creates a formula for an CFA edge, given the current function, SSA map and constraints.
   *
   * @param edge the edge for which to create the formula
   * @param function the current scope
   * @param ssa the current SSA map
   * @param constraints the current constraints
   * @return the formula for the edge
   */
  private BooleanFormula createFormulaForEdge(
      final CFAEdge edge, final String function,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
      throws UnrecognizedCCodeException, UnrecognizedCFAEdgeException, InterruptedException,
             UnrecognizedJSCodeException {
    switch (edge.getEdgeType()) {
    case StatementEdge: {
      return makeStatement((JSStatementEdge) edge, function,
          ssa, pts, constraints, errorConditions);
    }

    case ReturnStatementEdge: {
      // TODO
      return bfmgr.makeTrue();
    }

    case DeclarationEdge: {
      return makeDeclaration((JSDeclarationEdge)edge, function, ssa, pts, constraints,
          errorConditions);
    }

    case AssumeEdge: {
      JSAssumeEdge assumeEdge = (JSAssumeEdge)edge;
      return makePredicate(assumeEdge.getExpression(), assumeEdge.getTruthAssumption(),
          assumeEdge, function, ssa, pts, constraints, errorConditions);
    }

    case BlankEdge: {
      return bfmgr.makeTrue();
    }

    case FunctionCallEdge: {
      // TODO
      return bfmgr.makeTrue();
    }

    case FunctionReturnEdge: {
      // TODO
      return bfmgr.makeTrue();
    }

    case CallToReturnEdge:
      // TODO
      return bfmgr.makeTrue();

    default:
      throw new UnrecognizedCFAEdgeException(edge);
    }
  }

  private BooleanFormula makeStatement(
      final JSStatementEdge statement,
      final String function,
      final SSAMapBuilder ssa,
      final PointerTargetSetBuilder pts,
      final Constraints constraints,
      final ErrorConditions errorConditions)
      throws UnrecognizedCCodeException, InterruptedException, UnrecognizedJSCodeException {

    JSStatement stmt = statement.getStatement();
    if (stmt instanceof JSAssignment) {
      JSAssignment assignment = (JSAssignment)stmt;
      return makeAssignment(assignment.getLeftHandSide(), assignment.getRightHandSide(),
          statement, function, ssa, pts, constraints, errorConditions);

    } else {
      if (stmt instanceof CFunctionCallStatement) {
//        CRightHandSideVisitor<Formula, UnrecognizedCCodeException> ev = createJSRightHandSideVisitor(
//            statement, function, ssa, pts, constraints, errorConditions);
//        CFunctionCallStatement callStmt = (CFunctionCallStatement)stmt;
//        callStmt.getFunctionCallExpression().accept(ev);

      } else if (!(stmt instanceof JSExpressionStatement)) {
        throw new UnrecognizedJSCodeException("Unknown statement", statement, stmt);
      }

      // side-effect free statement, ignore
      return bfmgr.makeTrue();
    }
  }

  protected BooleanFormula makeDeclaration(
      final JSDeclarationEdge edge, final String function,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
      throws UnrecognizedCCodeException, InterruptedException, UnrecognizedJSCodeException {

    if (!(edge.getDeclaration() instanceof JSVariableDeclaration)) {
      // struct prototype, function declaration, typedef etc.
      logfOnce(Level.FINEST, edge, "Ignoring declaration");
      return bfmgr.makeTrue();
    }

    JSVariableDeclaration decl = (JSVariableDeclaration)edge.getDeclaration();
    final String varName = decl.getQualifiedName();

//    if (!isRelevantVariable(decl)) {
//      logger.logfOnce(Level.FINEST, "%s: Ignoring declaration of unused variable: %s",
//          decl.getFileLocation(), decl.toASTString());
//      return bfmgr.makeTrue();
//    }
//
//    checkForLargeArray(edge, decl.getType().getCanonicalType());
//
//    if (options.useParameterVariablesForGlobals() && decl.isGlobal()) {
//      globalDeclarations.add(decl);
//    }

    // if the var is unsigned, add the constraint that it should
    // be > 0
    //    if (((CSimpleType)spec).isUnsigned()) {
    //    long z = mathsat.api.msat_make_number(msatEnv, "0");
    //    long mvar = buildMsatVariable(var, idx);
    //    long t = mathsat.api.msat_make_gt(msatEnv, mvar, z);
    //    t = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), t);
    //    m1 = new MathsatFormula(t);
    //    }

    // just increment index of variable in SSAMap
    // (a declaration contains an implicit assignment, even without initializer)
    // In case of an existing initializer, we increment the index twice
    // (here and below) so that the index 2 only occurs for uninitialized variables.
    // DO NOT OMIT THIS CALL, even without an initializer!
    if (direction == AnalysisDirection.FORWARD) {
      makeFreshIndex(varName, decl.getType(), ssa);
    }

    // if there is an initializer associated to this variable,
    // take it into account
    BooleanFormula result = bfmgr.makeTrue();

//    if (decl.getInitializer() instanceof CInitializerList) {
//      // If there is an initializer, all fields/elements not mentioned
//      // in the initializer are set to 0 (C standard § 6.7.9 (21)
//
//      int size = machineModel.getSizeof(decl.getType());
//      if (size > 0) {
//        Formula var = makeVariable(varName, decl.getType(), ssa);
//        CType elementCType = decl.getType();
//        FormulaType<?> elementFormulaType = getFormulaTypeFromCType(elementCType);
//        Formula zero = fmgr.makeNumber(elementFormulaType, 0L);
//        result = bfmgr.and(result, fmgr.assignment(var, zero));
//      }
//    }

    for (JSAssignment assignment : JSInitializers.convertToAssignments(decl, edge)) {
      result = bfmgr.and(result,
          makeAssignment(
              assignment.getLeftHandSide(),
              assignment.getRightHandSide(),
              edge,
              function,
              ssa,
              pts,
              constraints,
              errorConditions));
    }

    return result;
  }

  /**
   * Check whether a large array is declared and abort analysis in this case. This is a heuristic
   * for SV-COMP to avoid wasting a lot of time for programs we probably cannot handle anyway or
   * returning a wrong answer.
   */
  protected void checkForLargeArray(final CDeclarationEdge declarationEdge, CType declarationType)
      throws UnsupportedCCodeException {
    if (!options.shouldAbortOnLargeArrays() || !(declarationType instanceof CArrayType)) {
      return;
    }
    CArrayType arrayType = (CArrayType) declarationType;
    CType elementType = arrayType.getType();

    if (elementType instanceof CSimpleType
        && ((CSimpleType) elementType).getType().isFloatingPointType()) {
      if (arrayType.getLengthAsInt().orElse(0) > 100) {
        throw new UnsupportedCCodeException("large floating-point array", declarationEdge);
      }
    }

    if (elementType instanceof CSimpleType
        && ((CSimpleType) elementType).getType() == CBasicType.INT) {
      if (arrayType.getLengthAsInt().orElse(0) >= 10000) {
        throw new UnsupportedCCodeException("large integer array", declarationEdge);
      }
    }
  }

  protected BooleanFormula makeExitFunction(
      final CFunctionSummaryEdge ce, final String calledFunction,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
          throws UnrecognizedCCodeException, InterruptedException {

    addGlobalAssignmentConstraints(ce, calledFunction, ssa, pts, constraints, errorConditions, RETURN_VARIABLE_NAME, false);

    CFunctionCall retExp = ce.getExpression();
    if (retExp instanceof CFunctionCallStatement) {
      // this should be a void return, just do nothing...
      return bfmgr.makeTrue();

    } else if (retExp instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement exp = (CFunctionCallAssignmentStatement)retExp;
      CFunctionCallExpression funcCallExp = exp.getRightHandSide();

      String callerFunction = ce.getSuccessor().getFunctionName();
      final com.google.common.base.Optional<CVariableDeclaration> returnVariableDeclaration =
          ce.getFunctionEntry().getReturnVariable();
      if (!returnVariableDeclaration.isPresent()) {
        throw new UnrecognizedCCodeException("Void function used in assignment", ce, retExp);
      }
      final CIdExpression rhs = new CIdExpression(funcCallExp.getFileLocation(),
          returnVariableDeclaration.get());

//      return makeAssignment(exp.getLeftHandSide(), rhs, ce, callerFunction, ssa, pts, constraints, errorConditions);
      throw new RuntimeException("Not implemented");
    } else {
      throw new UnrecognizedCCodeException("Unknown function exit expression", ce, retExp);
    }
  }

  protected CType getReturnType(CFunctionCallExpression funcCallExp, CFAEdge edge) throws UnrecognizedCCodeException {
    // NOTE: When funCallExp.getExpressionType() does always return the return type of the function we don't
    // need this function. However I'm not sure because there can be implicit casts. Just to be safe.
    CType retType;
    CFunctionDeclaration funcDecl = funcCallExp.getDeclaration();
    if (funcDecl == null) {
      // Check if we have a function pointer here.
      CExpression functionNameExpression = funcCallExp.getFunctionNameExpression();
      CType expressionType = functionNameExpression.getExpressionType().getCanonicalType();
      if (expressionType instanceof CFunctionType) {
        CFunctionType funcPtrType = (CFunctionType)expressionType;
        retType = funcPtrType.getReturnType();
      } else if (CTypes.isFunctionPointer(expressionType)) {
        CFunctionType funcPtrType = (CFunctionType) ((CPointerType) expressionType).getType().getCanonicalType();
        retType = funcPtrType.getReturnType();
      } else {
        throw new UnrecognizedCCodeException("Cannot handle function pointer call with unknown type " + expressionType, edge, funcCallExp);
      }
      assert retType != null;
    } else {
      retType = funcDecl.getType().getReturnType();
    }

    CType expType = funcCallExp.getExpressionType();
    if (!expType.getCanonicalType().equals(retType.getCanonicalType())) {
      // Bit ignore for now because we sometimes just get ElaboratedType instead of CompositeType
      String functionName = funcDecl != null ? funcDecl.getName() : funcCallExp.getFunctionNameExpression().toASTString();
      logfOnce(Level.WARNING, edge,
          "Return type of function %s is %s, but result is used as type %s",
          functionName, retType, expType);
    }
    return expType;
  }


  protected BooleanFormula makeFunctionCall(
      final CFunctionCallEdge edge, final String callerFunction,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
          throws UnrecognizedCCodeException, InterruptedException {

    List<CExpression> actualParams = edge.getArguments();

    CFunctionEntryNode fn = edge.getSuccessor();
    List<CParameterDeclaration> formalParams = fn.getFunctionParameters();

    if (fn.getFunctionDefinition().getType().takesVarArgs()) {
      if (formalParams.size() > actualParams.size()) {
        throw new UnrecognizedCCodeException("Number of parameters on function call does " +
            "not match function definition", edge);
      }

      if (!SAFE_VAR_ARG_FUNCTIONS.contains(fn.getFunctionName())) {
        logfOnce(Level.WARNING, edge,
            "Ignoring parameters passed as varargs to function %s",
            fn.getFunctionName());
      }

    } else {
      if (formalParams.size() != actualParams.size()) {
        throw new UnrecognizedCCodeException("Number of parameters on function call does " +
            "not match function definition", edge);
      }
    }

    int i = 0;
    BooleanFormula result = bfmgr.makeTrue();
    for (CParameterDeclaration formalParam : formalParams) {
      CExpression paramExpression = actualParams.get(i++);
      CIdExpression lhs = new CIdExpression(paramExpression.getFileLocation(), formalParam);
      final CIdExpression paramLHS;
      if (options.useParameterVariables()) {
        // make assignments: tmp_param1==arg1, tmp_param2==arg2, ...
        CParameterDeclaration tmpParameter = new CParameterDeclaration(
                formalParam.getFileLocation(), formalParam.getType(), formalParam.getName() + PARAM_VARIABLE_NAME);
        tmpParameter.setQualifiedName(formalParam.getQualifiedName() + PARAM_VARIABLE_NAME);
        paramLHS = new CIdExpression(paramExpression.getFileLocation(), tmpParameter);
      } else {
        paramLHS = lhs;
      }

//      BooleanFormula eq = makeAssignment(paramLHS, lhs, paramExpression, edge, callerFunction, ssa, pts, constraints, errorConditions);
//      result = bfmgr.and(result, eq);
      throw new RuntimeException("Not implemented");
    }

    addGlobalAssignmentConstraints(edge, fn.getFunctionName(), ssa, pts, constraints, errorConditions, PARAM_VARIABLE_NAME, true);

    return result;
  }

  protected BooleanFormula makeReturn(final com.google.common.base.Optional<CAssignment> assignment,
      final CReturnStatementEdge edge, final String function,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
          throws UnrecognizedCCodeException, InterruptedException {
    if (!assignment.isPresent()) {
      // this is a return from a void function, do nothing
      return bfmgr.makeTrue();
    } else {

//      return makeAssignment(assignment.get().getLeftHandSide(), assignment.get().getRightHandSide(),
//          edge, function, ssa, pts, constraints, errorConditions);
      throw new RuntimeException("Not implemented");
    }
  }

  /**
   * Creates formula for the given assignment.
   * @param lhs the left-hand-side of the assignment
   * @param rhs the right-hand-side of the assignment
   * @return the assignment formula
   */
  private BooleanFormula makeAssignment(
      final JSLeftHandSide lhs, JSRightHandSide rhs,
      final CFAEdge edge, final String function,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
      throws UnrecognizedCCodeException, InterruptedException, UnrecognizedJSCodeException {
    // lhs is used twice, also as lhsForChecking!
    return makeAssignment(lhs, lhs, rhs, edge, function, ssa, pts, constraints, errorConditions);
  }

  /**
   * Creates formula for the given assignment.
   *
   * @param lhs the left-hand-side of the assignment
   * @param lhsForChecking a left-hand-side of the assignment (for most cases: lhs ==
   *     lhsForChecking), that is used to check, if the assignment is important. If the assignment
   *     is not important, we return TRUE.
   * @param rhs the right-hand-side of the assignment
   * @return the assignment formula
   * @throws InterruptedException may be thrown in subclasses
   */
  protected BooleanFormula makeAssignment(
      final JSLeftHandSide lhs,
      final JSLeftHandSide lhsForChecking,
      JSRightHandSide rhs,
      final CFAEdge edge,
      final String function,
      final SSAMapBuilder ssa,
      final PointerTargetSetBuilder pts,
      final Constraints constraints,
      final ErrorConditions errorConditions)
      throws UnrecognizedJSCodeException {
    final TypedValue r = buildTerm(rhs, edge, function, ssa, pts, constraints, errorConditions);
    final IntegerFormula l = buildLvalueTerm((JSIdExpression) lhs, ssa);
    return fmgr.makeAnd(
        fmgr.assignment(typedValues.typeof(l), r.getType()),
        fmgr.makeOr(
            fmgr.makeAnd(
                fmgr.makeEqual(typedValues.typeof(l), typeTags.BOOLEAN),
                fmgr.makeEqual(typedValues.booleanValue(l), toBoolean(r))),
            fmgr.makeAnd(
                fmgr.makeEqual(typedValues.typeof(l), typeTags.NUMBER),
                fmgr.makeEqual(typedValues.numberValue(l), toNumber(r)))));
  }

  BooleanFormula toBoolean(final TypedValue pValue) {
    final IntegerFormula type = pValue.getType();
    if (type.equals(typeTags.BOOLEAN)) {
      return (BooleanFormula) pValue.getValue();
    } else if (type.equals(typeTags.NUMBER)) {
      return numberToBoolean((FloatingPointFormula) pValue.getValue());
    } else if (type.equals(typeTags.STRING)) {
      // TODO empty string to boolean conversion of string constants should be possible
      // For now, assume that every string is not empty.
      return bfmgr.makeTrue();
    } else if (type.equals(typeTags.UNDEFINED)) {
      return bfmgr.makeFalse();
    } else if (type.equals(typeTags.OBJECT)) {
      return bfmgr.makeFalse(); // TODO handle non null objects
    } else {
      // variable
      final IntegerFormula variable = (IntegerFormula) pValue.getValue();
      return bfmgr.ifThenElse(
          fmgr.makeEqual(type, typeTags.BOOLEAN),
          typedValues.booleanValue(variable),
          bfmgr.ifThenElse(
              fmgr.makeEqual(type, typeTags.NUMBER),
              numberToBoolean(typedValues.numberValue(variable)),
              bfmgr.makeFalse()));
    }
  }

  private BooleanFormula numberToBoolean(final FloatingPointFormula pValue) {
    return bfmgr.ifThenElse(fpfmgr.isZero(pValue), bfmgr.makeFalse(), bfmgr.makeTrue());
  }

  FloatingPointFormula toNumber(final TypedValue pValue) {
    final IntegerFormula type = pValue.getType();
    if (type.equals(typeTags.BOOLEAN)) {
      return booleanToNumber((BooleanFormula) pValue.getValue());
    } else if (type.equals(typeTags.NUMBER)) {
      return (FloatingPointFormula) pValue.getValue();
    } else if (type.equals(typeTags.STRING)) {
      // TODO string to number conversion of string constants should be possible
      // For now, assume that every string is not a StringNumericLiteral, see
      // https://www.ecma-international.org/ecma-262/5.1/#sec-9.3
      return fpfmgr.makeNaN(Types.NUMBER_TYPE);
    } else if (type.equals(typeTags.UNDEFINED)) {
      return fpfmgr.makeNaN(Types.NUMBER_TYPE);
    } else if (type.equals(typeTags.OBJECT)) {
      return fmgr.makeNumber(Types.NUMBER_TYPE, 0); // TODO handle non null objects
    } else {
      // variable
      final IntegerFormula variable = (IntegerFormula) pValue.getValue();
      return bfmgr.ifThenElse(
          fmgr.makeEqual(type, typeTags.BOOLEAN),
          booleanToNumber(typedValues.booleanValue(variable)),
          typedValues.numberValue(variable));
    }
  }

  private FloatingPointFormula booleanToNumber(final BooleanFormula pValue) {
    return bfmgr.ifThenElse(
        pValue, fmgr.makeNumber(Types.NUMBER_TYPE, 1), fmgr.makeNumber(Types.NUMBER_TYPE, 0));
  }

  private IntegerFormula buildLvalueTerm(final JSIdExpression pLhs, final SSAMapBuilder pSsa) {
    return makeFreshVariable(pLhs.getDeclaration().getQualifiedName(), pSsa);
  }

  /**
   * Convert a simple C expression to a formula consistent with the current state of the {@code
   * pFormula}.
   *
   * @param pFormula Current {@link PathFormula}.
   * @param expr Expression to convert.
   * @param edge Reference edge, used for log messages only.
   * @return Created formula.
   */
  public final Formula buildTermFromPathFormula(
      PathFormula pFormula, CIdExpression expr, CFAEdge edge) throws UnrecognizedCCodeException {

    String functionName = edge.getPredecessor().getFunctionName();
    Constraints constraints = new Constraints(bfmgr);
//    return buildTerm(
//        expr,
//        edge,
//        functionName,
//        pFormula.getSsa().builder(),
//        createPointerTargetSetBuilder(pFormula.getPointerTargetSet()),
//        constraints,
//        ErrorConditions.dummyInstance(bfmgr)
//    );
    throw new RuntimeException("Not implemented");
  }

  private TypedValue buildTerm(
      JSRightHandSide exp,
      CFAEdge edge,
      String function,
      SSAMapBuilder ssa,
      PointerTargetSetBuilder pts,
      Constraints constraints,
      ErrorConditions errorConditions)
      throws UnrecognizedJSCodeException {
    return exp.accept(
        createJSRightHandSideVisitor(edge, function, ssa, pts, constraints, errorConditions));
  }

  protected Formula buildLvalueTerm(
      JSLeftHandSide exp,
      CFAEdge edge,
      String function,
      SSAMapBuilder ssa,
      PointerTargetSetBuilder pts,
      Constraints constraints,
      ErrorConditions errorConditions)
      throws UnrecognizedJSCodeException {
    return exp.accept(new LValueVisitor(this, edge, function, ssa, pts, constraints, errorConditions));
  }

  <T extends Formula> T ifTrueThenOneElseZero(FormulaType<T> type, BooleanFormula pCond) {
    T one = fmgr.makeNumber(type, 1);
    T zero = fmgr.makeNumber(type, 0);
    return bfmgr.ifThenElse(pCond, one, zero);
  }

  protected final <T extends Formula> BooleanFormula toBooleanFormula(T pF) {
    // If this is not a predicate, make it a predicate by adding a "!= 0"
    if (fmgr.getFormulaType(pF).isBooleanType()) {
      return (BooleanFormula) pF;
    }

    T zero = fmgr.makeNumber(fmgr.getFormulaType(pF), 0);

    Optional<Triple<BooleanFormula, T, T>> split = fmgr.splitIfThenElse(pF);
    if (split.isPresent()) {
      Triple<BooleanFormula, T, T> parts = split.get();

      T one = fmgr.makeNumber(fmgr.getFormulaType(pF), 1);
      if (parts.getSecond().equals(one) && parts.getThird().equals(zero)) {
        return parts.getFirst();
      } else if (parts.getSecond().equals(zero) && parts.getThird().equals(one)) {
        return bfmgr.not(parts.getFirst());
      }
    }

    return bfmgr.not(fmgr.makeEqual(pF, zero));
  }

  /** @throws InterruptedException may be thrown in subclasses */
  protected BooleanFormula makePredicate(
      JSExpression exp,
      boolean isTrue,
      CFAEdge edge,
      String function,
      SSAMapBuilder ssa,
      PointerTargetSetBuilder pts,
      Constraints constraints,
      ErrorConditions errorConditions)
      throws UnrecognizedJSCodeException, InterruptedException {

    final TypedValue condition =
        exp.accept(
            createJSRightHandSideVisitor(edge, function, ssa, pts, constraints, errorConditions));
    BooleanFormula result = toBoolean(condition);

    if (!isTrue) {
      result = bfmgr.not(result);
    }
    return result;
  }

  public final BooleanFormula makePredicate(
      JSExpression exp, CFAEdge edge, String function, SSAMapBuilder ssa)
      throws UnrecognizedJSCodeException, InterruptedException {
    PointerTargetSetBuilder pts = createPointerTargetSetBuilder(PointerTargetSet.emptyPointerTargetSet());
    Constraints constraints = new Constraints(bfmgr);
    ErrorConditions errorConditions = ErrorConditions.dummyInstance(bfmgr);
    BooleanFormula f = makePredicate(exp, true, edge, function, ssa, pts, constraints, errorConditions);
    return bfmgr.and(f, constraints.get());
  }

  /**
   * Parameters not used in {@link JSToFormulaConverter}, may be in subclasses they are.
   * @param pts the pointer target set to use initially
   */
  protected PointerTargetSetBuilder createPointerTargetSetBuilder(PointerTargetSet pts) {
    return DummyPointerTargetSetBuilder.INSTANCE;
  }

  /**
   * Parameters not used in {@link JSToFormulaConverter}, may be in subclasses they are.
   *
   * @param pts1 the first PointerTargetset
   * @param pts2 the second PointerTargetset
   * @param ssa the SSAMap to use
   * @throws InterruptedException may be thrown in subclasses
   */
  public MergeResult<PointerTargetSet> mergePointerTargetSets(
      final PointerTargetSet pts1, final PointerTargetSet pts2, final SSAMap ssa)
      throws InterruptedException {
    return MergeResult.trivial(pts1, bfmgr);
  }

  /**
   * Parameters not used in {@link JSToFormulaConverter}, may be in subclasses they are.
   *
   * @param pEdge the edge to be visited
   * @param pFunction the current function name
   * @param ssa the current SSAMapBuilder
   * @param pts the current PointerTargetSet
   * @param constraints the constraints needed during visiting
   * @param errorConditions the error conditions
   */
  private JSRightHandSideVisitor<TypedValue, UnrecognizedJSCodeException>
      createJSRightHandSideVisitor(
          CFAEdge pEdge,
          String pFunction,
          SSAMapBuilder ssa,
          PointerTargetSetBuilder pts,
          Constraints constraints,
          ErrorConditions errorConditions) {
    return new ExpressionToFormulaVisitor(this, fmgr, pEdge, pFunction, ssa, constraints);
  }

  /**
   * Creates a Formula which accesses the given bits.
   */
  private BitvectorFormula accessField(Triple<Integer, Integer, Boolean> msb_Lsb_signed, BitvectorFormula f) {
    return fmgr.makeExtract(f, msb_Lsb_signed.getFirst(), msb_Lsb_signed.getSecond(), msb_Lsb_signed.getThird());
  }

  /**
   * Creates a Formula which accesses the given Field
   */
  BitvectorFormula accessField(CFieldReference fExp, Formula f) throws UnrecognizedCCodeException {
    assert options.handleFieldAccess() : "Fieldaccess if only allowed with handleFieldAccess";
    assert f instanceof BitvectorFormula : "Fields need to be represented with bitvectors";
    // Get the underlaying structure
    Triple<Integer, Integer, Boolean> msb_Lsb_signed = getFieldOffsetMsbLsb(fExp);
    return accessField(msb_Lsb_signed, (BitvectorFormula)f);
  }

  /**
   * Return the bitvector for a struct with the bits for one field replaced
   * by another bitvector, or left out completely.
   * @param fExp The field of the struct to replace.
   * @param pLVar The full struct.
   * @param pRightVariable The replacement bitvector, or nothing.
   * @return If pRightVariable is present, a formula of the same size as pLVar, but with some bits replaced.
   * If pRightVariable is not present, a formula that is smaller then pLVar (with the field bits missing).
   */
  Formula replaceField(CFieldReference fExp, Formula pLVar, Optional<Formula> pRightVariable) throws UnrecognizedCCodeException {
    assert options.handleFieldAccess() : "Fieldaccess if only allowed with handleFieldAccess";

    Triple<Integer, Integer, Boolean> msb_Lsb = getFieldOffsetMsbLsb(fExp);

    int size = efmgr.getLength((BitvectorFormula) pLVar);
    assert size > msb_Lsb.getFirst() : "pLVar is too small";
    assert 0 <= msb_Lsb.getSecond() && msb_Lsb.getFirst() >= msb_Lsb.getSecond() : "msb_Lsb is invalid";

    // create a list with three formulas:
    // - prefix of struct (before the field)
    // - the replaced field
    // - suffix of struct (after the field)
    List<Formula> parts = new ArrayList<>(3);

    if (msb_Lsb.getFirst() + 1 < size) {
      parts.add(fmgr.makeExtract(pLVar, size - 1, msb_Lsb.getFirst() + 1, msb_Lsb.getThird()));
    }

    if (pRightVariable.isPresent()) {
      assert efmgr.getLength((BitvectorFormula) pRightVariable.get()) == msb_Lsb.getFirst() + 1 - msb_Lsb.getSecond() : "The new formula has not the right size";
      parts.add(pRightVariable.get());
    }

    if (msb_Lsb.getSecond() > 0) {
      parts.add(fmgr.makeExtract(pLVar, msb_Lsb.getSecond() - 1, 0, msb_Lsb.getThird()));
    }

    if (parts.isEmpty()) {
      // struct with no other fields, return empty bitvector
      return efmgr.makeBitvector(0, 0);
    }
    return fmgr.makeConcat(parts);
  }

  /**
   * Returns the offset of the given CFieldReference within the structure in bits.
   */
  private Triple<Integer, Integer, Boolean> getFieldOffsetMsbLsb(CFieldReference fExp) throws UnrecognizedCCodeException {
    CExpression fieldRef = getRealFieldOwner(fExp);
    CCompositeType structType = (CCompositeType)fieldRef.getExpressionType().getCanonicalType();

    // f is now the structure, access it:

    int offset;
    switch (structType.getKind()) {
    case UNION:
      offset = 0;
      break;
    case STRUCT:
      offset = getFieldOffset(structType, fExp.getFieldName());
      break;
    default:
      throw new UnrecognizedCCodeException("Unexpected field access", fExp);
    }

    CType type = fExp.getExpressionType();
    int fieldSize = getBitSizeof(type);

    // Crude hack for unions with zero-sized array fields produced by LDV
    // (ldv-consumption/32_7a_cilled_true_linux-3.8-rc1-32_7a-fs--ceph--ceph.ko-ldv_main7_sequence_infinite_withcheck_stateful.cil.out.c)
    if (fieldSize == 0 && structType.getKind() == ComplexTypeKind.UNION) {
      fieldSize = getBitSizeof(fieldRef.getExpressionType());
    }

    // we assume that only CSimpleTypes can be unsigned
    boolean signed = !(type instanceof CSimpleType)
        || machineModel.isSigned((CSimpleType) type);

    int lsb = offset;
    int msb = offset + fieldSize - 1;
    assert (lsb >= 0);
    assert (msb >= lsb);
    Triple<Integer, Integer, Boolean> msb_Lsb = Triple.of(msb, lsb, signed);
    return msb_Lsb;
  }

  /**
   * Returns the offset of the given field in the given struct in bits.
   *
   * This function does not handle UNIONs or ENUMs!
   */
  private int getFieldOffset(CCompositeType structType, String fieldName) {
    int off = 0;
    for (CCompositeTypeMemberDeclaration member : structType.getMembers()) {
      if (member.getName().equals(fieldName)) {
        return off;
      }

      off += getBitSizeof(member.getType());
    }

    throw new AssertionError("field " + fieldName + " was not found in " + structType);
  }

  /**
   * We call this method for unsupported Expressions and just make a new Variable.
   */
  Formula makeVariableUnsafe(JSExpression exp, String function, SSAMapBuilder ssa,
      boolean makeFresh) {

    if (makeFresh) {
      logger.logOnce(Level.WARNING, "Program contains array, or pointer (multiple level of indirection), or field (enable handleFieldAccess and handleFieldAliasing) access; analysis is imprecise in case of aliasing.");
    }
    logger.logfOnce(Level.FINEST, "%s: Unhandled expression treated as free variable: %s",
        exp.getFileLocation(), exp.toASTString());

    String var = exprToVarName(exp, function);
    if (makeFresh) {
      return makeFreshVariable(var, exp.getExpressionType(), ssa);
    } else {
      return makeVariable(var, exp.getExpressionType(), ssa);
    }
  }

  /**
   * Throwing two checked exception from a visitor is not possible directly,
   * thus we have trouble handling InterruptedExceptions in visitors.
   * This method allows them to be thrown without the compiler complaining.
   * This is safe because the public methods of this package specify InterruptedException
   * to be thrown, so callers need to handle it anyway.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Throwable> RuntimeException propagateInterruptedException(
      InterruptedException e) throws T {
    throw (T) e;
  }

  /**
   * Prints some information about the RegionManager.
   *
   * @param out - output stream
   */
  public void printStatistics(PrintStream out) {}
}
