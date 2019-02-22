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


import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSArrayLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBracketPropertyAccess;
import org.sosy_lab.cpachecker.cfa.ast.js.JSDeclaredByExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFieldAccess;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSObjectLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSObjectLiteralField;
import org.sosy_lab.cpachecker.cfa.ast.js.JSRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.js.JSRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUndefinedLiteralExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.smt.FloatingPointFormulaManagerView;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

/** Management of formula encoding of JavaScript expressions. */
@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class ExpressionToFormulaVisitor extends ManagerWithEdgeContext
    implements JSExpressionFormulaManager,
        JSRightHandSideVisitor<TypedValue, UnrecognizedCodeException> {

  ExpressionToFormulaVisitor(final EdgeManagerContext pCtx) {
    super(pCtx);
  }

  @Override
  public TypedValue visit(final JSFunctionCallExpression pFunctionCallExpression)
      throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException(
        "JSFunctionCallExpression not implemented yet", pFunctionCallExpression);
  }

  @Override
  public TypedValue makeExpression(final JSRightHandSide pExpression)
      throws UnrecognizedCodeException {
    return pExpression.accept(this);
  }

  @Override
  public TypedValue visit(final JSBinaryExpression pBinaryExpression)
      throws UnrecognizedCodeException {
    final TypedValue leftOperand = visit(pBinaryExpression.getOperand1());
    final TypedValue rightOperand = visit(pBinaryExpression.getOperand2());
    switch (pBinaryExpression.getOperator()) {
      case EQUALS:
        // Treat == like === until differences between these operators are implemented.
        // It might lead to false positives in some cases, but it allows to analyze many real world
        // programs that could not be analyzed otherwise right now.
      case EQUAL_EQUAL_EQUAL:
        return tvmgr.createBooleanValue(makeEqual(leftOperand, rightOperand));
      case NOT_EQUALS:
        // Treat != like !== until differences between these operators are implemented.
        // It might lead to false positives in some cases, but it allows to analyze many real world
        // programs that could not be analyzed otherwise right now.
      case NOT_EQUAL_EQUAL:
        return tvmgr.createBooleanValue(fmgr.makeNot(makeEqual(leftOperand, rightOperand)));
      case PLUS:
        return makePlus(leftOperand, rightOperand);
      case MINUS:
        return tvmgr.createNumberValue(
            fpfmgr.subtract(valConv.toNumber(leftOperand), valConv.toNumber(rightOperand)));
      case TIMES:
        return tvmgr.createNumberValue(
            fpfmgr.multiply(valConv.toNumber(leftOperand), valConv.toNumber(rightOperand)));
      case DIVIDE:
        return tvmgr.createNumberValue(
            fpfmgr.divide(valConv.toNumber(leftOperand), valConv.toNumber(rightOperand)));
      case REMAINDER:
        logger.logfOnce(
            Level.WARNING,
            "Formula encoding of remainder is not implemented yet for cases, where neither an infinity, nor a zero, nor NaN is involved (%s): %s",
            pBinaryExpression.getFileLocation(),
            pBinaryExpression);
        return tvmgr.createNumberValue(makeRemainder(leftOperand, rightOperand));
      case LESS:
        return tvmgr.createBooleanValue(
            fpfmgr.lessThan(valConv.toNumber(leftOperand), valConv.toNumber(rightOperand)));
      case LESS_EQUALS:
        return tvmgr.createBooleanValue(
            fpfmgr.lessOrEquals(valConv.toNumber(leftOperand), valConv.toNumber(rightOperand)));
      case GREATER:
        return tvmgr.createBooleanValue(
            fpfmgr.greaterThan(valConv.toNumber(leftOperand), valConv.toNumber(rightOperand)));
      case GREATER_EQUALS:
        return tvmgr.createBooleanValue(
            fpfmgr.greaterOrEquals(valConv.toNumber(leftOperand), valConv.toNumber(rightOperand)));
      case AND:
        return makeBitwiseOperator(bitVecMgr::and, leftOperand, rightOperand);
      case OR:
        return makeBitwiseOperator(bitVecMgr::or, leftOperand, rightOperand);
      case LEFT_SHIFT:
        return makeBitwiseOperator(bitVecMgr::shiftLeft, leftOperand, rightOperand);
      case RIGHT_SHIFT_UNSIGNED:
        return makeBitwiseOperator(
            (l, r) -> bitVecMgr.shiftRight(l, r, false), leftOperand, rightOperand);
      case RIGHT_SHIFT_SIGNED:
        return makeBitwiseOperator(
            (l, r) -> bitVecMgr.shiftRight(l, r, true), leftOperand, rightOperand);
      case XOR:
        return makeBitwiseOperator(bitVecMgr::xor, leftOperand, rightOperand);
      default:
        throw new UnrecognizedCodeException(
            "JSBinaryExpression not implemented yet", pBinaryExpression);
    }
  }

  @Nonnull
  private TypedValue makeBitwiseOperator(
      final BinaryOperator<BitvectorFormula> pOperator,
      final TypedValue pLeftOperand,
      final TypedValue pRightOperand) {
    return tvmgr.createNumberValue(
        fpfmgr.castFrom(
            pOperator.apply(valConv.toInt32(pLeftOperand), valConv.toInt32(pRightOperand)),
            true,
            FormulaType.getDoublePrecisionFloatingPointType()));
  }

  private TypedValue makePlus(final TypedValue pLeftOperand, final TypedValue pRightOperand) {
    final IntegerFormula resultVar = ctx.varMgr.makeFreshVariable("additionResult");
    final BooleanFormula resultIsString =
        bfmgr.or(
            fmgr.makeEqual(pLeftOperand.getType(), typeTags.STRING),
            fmgr.makeEqual(pRightOperand.getType(), typeTags.STRING));
    final FloatingPointFormula concatResult =
        strMgr.concat(
            valConv.toStringFormula(pLeftOperand), valConv.toStringFormula(pRightOperand));
    final FloatingPointFormula numericAdditionResult =
        fpfmgr.add(valConv.toNumber(pLeftOperand), valConv.toNumber(pRightOperand));
    ctx.constraints.addConstraint(
        bfmgr.ifThenElse(
            resultIsString,
            ctx.assignmentMgr.makeAssignment(resultVar, tvmgr.createStringValue(concatResult)),
            ctx.assignmentMgr.makeAssignment(
                resultVar, tvmgr.createNumberValue(numericAdditionResult))));
    ctx.constraints.addConstraint(
        fmgr.makeEqual(
            typedVarValues.typeof(resultVar),
            bfmgr.ifThenElse(resultIsString, typeTags.STRING, typeTags.NUMBER)));
    return new TypedValue(typedVarValues.typeof(resultVar), resultVar);
  }

  /**
   * Formula encode remainder operation. Operands are converted to number. <cite> The result of an
   * ECMAScript floating-point remainder operation is determined by the rules of IEEE arithmetic:
   *
   * <ul>
   *   <li>If either operand is NaN, the result is NaN.
   *   <li>The sign of the result equals the sign of the dividend.
   *   <li>If the dividend is an infinity, or the divisor is a zero, or both, the result is NaN.
   *   <li>If the dividend is finite and the divisor is an infinity, the result equals the dividend.
   *   <li>If the dividend is a zero and the divisor is nonzero and finite, the result is the same
   *       as the dividend.
   *   <li>In the remaining cases, where neither an infinity, nor a zero, nor NaN is involved, the
   *       floating-point remainder r from a dividend n and a divisor d is defined by the
   *       mathematical relation r = n − (d × q) where q is an integer that is negative only if n/d
   *       is negative and positive only if n/d is positive, and whose magnitude is as large as
   *       possible without exceeding the magnitude of the true mathematical quotient of n and d. r
   *       is computed and rounded to the nearest representable value using IEEE 754
   *       round-to-nearest mode.
   * </ul>
   *
   * </cite>
   *
   * @param pLeftOperand Dividend
   * @param pRightOperand Divisor
   * @return Remainder
   * @see <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-11.5.3">11.5.3 Applying the
   *     % Operator</a>
   */
  @Nonnull
  private FloatingPointFormula makeRemainder(
      final TypedValue pLeftOperand, final TypedValue pRightOperand) {
    final FloatingPointFormulaManagerView f = fpfmgr;
    final FloatingPointFormula dividend = valConv.toNumber(pLeftOperand);
    final FloatingPointFormula divisor = valConv.toNumber(pRightOperand);
    // TODO In the remaining cases, where neither an infinity, nor a zero, nor NaN is involved, the
    // floating-point remainder r from a dividend n and a divisor d is defined by the mathematical
    // relation r = n − (d × q) where q is an integer that is negative only if n/d is negative and
    // positive only if n/d is positive, and whose magnitude is as large as possible without
    // exceeding the magnitude of the true mathematical quotient of n and d. r is computed and
    // rounded to the nearest representable value using IEEE 754 round-to-nearest mode.
    return bfmgr.ifThenElse(
        bfmgr.or(
            numMgr.isNaN(dividend),
            numMgr.isNaN(divisor),
            numMgr.isInfinity(dividend),
            f.isZero(divisor)),
        f.makeNaN(Types.NUMBER_TYPE),
        bfmgr.ifThenElse(
            bfmgr.or(numMgr.isInfinity(divisor), f.isZero(dividend)), dividend, dividend));
  }

  @Nonnull
  private BooleanFormula makeEqual(final TypedValue pLeftOperand, final TypedValue pRightOperand) {
    final IntegerFormula leftType = pLeftOperand.getType();
    final IntegerFormula rightType = pRightOperand.getType();
    final BooleanFormula nanCase =
        jsOptions.useNaN
            ? bfmgr.and(
                fmgr.makeNot(numMgr.isNaN(valConv.toNumber(pLeftOperand))),
                fmgr.makeNot(numMgr.isNaN(valConv.toNumber(pRightOperand))))
            : bfmgr.makeTrue();
    return fmgr.makeAnd(
        fmgr.makeEqual(leftType, rightType),
        bfmgr.or(
            fmgr.makeEqual(typeTags.UNDEFINED, leftType),
            bfmgr.and(
                fmgr.makeEqual(typeTags.NUMBER, leftType),
                nanCase,
                fmgr.makeEqual(valConv.toNumber(pLeftOperand), valConv.toNumber(pRightOperand))),
            fmgr.makeAnd(
                fmgr.makeEqual(typeTags.BOOLEAN, leftType),
                fmgr.makeEqual(valConv.toBoolean(pLeftOperand), valConv.toBoolean(pRightOperand))),
            fmgr.makeAnd(
                fmgr.makeEqual(typeTags.OBJECT, leftType),
                fmgr.makeEqual(valConv.toObject(pLeftOperand), valConv.toObject(pRightOperand))),
            fmgr.makeAnd(
                fmgr.makeEqual(typeTags.STRING, leftType),
                fmgr.makeEqual(
                    valConv.toStringFormula(pLeftOperand), valConv.toStringFormula(pRightOperand))),
            fmgr.makeAnd(
                fmgr.makeEqual(typeTags.FUNCTION, leftType),
                fmgr.makeEqual(
                    valConv.toFunction(pLeftOperand), valConv.toFunction(pRightOperand)))));
  }

  @Override
  public TypedValue visit(final JSStringLiteralExpression pStringLiteralExpression) {
    return tvmgr.createStringValue(strMgr.getStringFormula(pStringLiteralExpression.getValue()));
  }

  @Override
  public TypedValue visit(final JSFloatLiteralExpression pLiteral) {
    return makeNumber(pLiteral.getValue());
  }

  @Nonnull
  private TypedValue makeNumber(final BigDecimal pValue) {
    return tvmgr.createNumberValue(
        fpfmgr.makeNumber(pValue, FormulaType.getDoublePrecisionFloatingPointType()));
  }

  @Override
  public TypedValue visit(final JSUnaryExpression pUnaryExpression)
      throws UnrecognizedCodeException {
    final TypedValue operand = visit(pUnaryExpression.getOperand());
    switch (pUnaryExpression.getOperator()) {
      case COMPLEMENT:
        return tvmgr.createNumberValue(
            fpfmgr.castFrom(
                bitVecMgr.not(valConv.toInt32(operand)),
                true,
                FormulaType.getDoublePrecisionFloatingPointType()));
      case NOT:
        return tvmgr.createBooleanValue(fmgr.makeNot(valConv.toBoolean(operand)));
      case PLUS:
        return tvmgr.createNumberValue(valConv.toNumber(operand));
      case MINUS:
        return tvmgr.createNumberValue(fmgr.makeNegate(valConv.toNumber(operand)));
      case VOID:
        return tvmgr.getUndefinedValue();
      case TYPE_OF:
        return tvmgr.createStringValue(typeOf(operand));
      default:
        throw new UnrecognizedCodeException(
            "JSUnaryExpression not implemented yet", pUnaryExpression);
    }
  }

  @Nonnull
  private FloatingPointFormula typeOf(final TypedValue pValue) {
    final IntegerFormula type = pValue.getType();
    if (type.equals(typeTags.BOOLEAN)) {
      return strMgr.getStringFormula("boolean");
    } else if (type.equals(typeTags.FUNCTION)) {
      return strMgr.getStringFormula("function");
    } else if (type.equals(typeTags.NUMBER)) {
      return strMgr.getStringFormula("number");
    } else if (type.equals(typeTags.STRING)) {
      return strMgr.getStringFormula("string");
    } else if (type.equals(typeTags.UNDEFINED)) {
      return strMgr.getStringFormula("undefined");
    } else if (type.equals(typeTags.OBJECT)) {
      return strMgr.getStringFormula("object");
    } else {
      // variable
      return bfmgr.ifThenElse(
          fmgr.makeEqual(type, typeTags.BOOLEAN),
          strMgr.getStringFormula("boolean"),
          bfmgr.ifThenElse(
              fmgr.makeEqual(type, typeTags.FUNCTION),
              strMgr.getStringFormula("function"),
              bfmgr.ifThenElse(
              fmgr.makeEqual(type, typeTags.NUMBER),
              strMgr.getStringFormula("number"),
              bfmgr.ifThenElse(
                  fmgr.makeEqual(type, typeTags.STRING),
                  strMgr.getStringFormula("string"),
                  bfmgr.ifThenElse(
                    fmgr.makeEqual(type, typeTags.UNDEFINED),
                    strMgr.getStringFormula("undefined"),
                    strMgr.getStringFormula("object"))))));
    }
  }


  @Override
  public TypedValue visit(final JSIntegerLiteralExpression pIntegerLiteralExpression) {
    return makeNumber(new BigDecimal(pIntegerLiteralExpression.getValue()));
  }

  @Override
  public TypedValue visit(final JSBooleanLiteralExpression pBooleanLiteralExpression) {
    return tvmgr.createBooleanValue(bfmgr.makeBoolean(pBooleanLiteralExpression.getValue()));
  }

  @Override
  public TypedValue visit(final JSNullLiteralExpression pNullLiteralExpression) {
    return tvmgr.getNullValue();
  }

  @Override
  public TypedValue visit(final JSObjectLiteralExpression pObjectLiteralExpression)
      throws UnrecognizedCodeException {
    return ctx.objMgr.createObject(pObjectLiteralExpression);
  }

  @Override
  public TypedValue visit(final JSArrayLiteralExpression pArrayLiteralExpression)
      throws UnrecognizedCodeException {
    final IntegerFormula objectId = objIdMgr.createObjectId();
    final TypedValue objectValue = tvmgr.createObjectValue(objectId);
    final List<JSExpression> elements = pArrayLiteralExpression.getElements();
    // +1 due to additional property `length`
    final List<JSObjectLiteralField> fields = new ArrayList<>(elements.size() + 1);
    fields.add(
        new JSObjectLiteralField(
            "length",
            new JSIntegerLiteralExpression(
                FileLocation.DUMMY, BigInteger.valueOf(elements.size()))));
    for (int i = 0; i < elements.size(); i++) {
      fields.add(new JSObjectLiteralField(Integer.toString(i), elements.get(i)));
    }
    ctx.objMgr.setObjectFields(objectId, fields);
    ctx.objMgr.markAsArray(objectId);
    return objectValue;
  }

  @Override
  public TypedValue visit(final JSUndefinedLiteralExpression pUndefinedLiteralExpression) {
    return tvmgr.getUndefinedValue();
  }

  @Override
  public TypedValue visit(final JSThisExpression pThisExpression) throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("JSThisExpression not implemented yet", pThisExpression);
  }

  @Override
  public TypedValue visit(final JSDeclaredByExpression pDeclaredByExpression) {
    return ctx.jsFunDeclMgr.makeDeclaredBy(pDeclaredByExpression);
  }

  @Override
  public TypedValue visit(final JSIdExpression pIdExpression) throws UnrecognizedCodeException {
    JSSimpleDeclaration declaration = pIdExpression.getDeclaration();
    if (declaration == null) {
      if (!globalDeclarationsMgr.has(pIdExpression)) {
        return handlePredefined(pIdExpression);
      }
      declaration = globalDeclarationsMgr.get(pIdExpression);
    }
    final IntegerFormula variable = ctx.scopeMgr.scopedVariable(declaration);
    return new TypedValue(typedVarValues.typeof(variable), variable);
  }

  @Override
  public TypedValue visit(final JSFieldAccess pFieldAccess) throws UnrecognizedCodeException {
    final JSSimpleDeclaration objectDeclaration =
        ctx.propMgr.getObjectDeclarationOfFieldAccess(pFieldAccess);
    final IntegerFormula objectId =
        typedVarValues.objectValue(ctx.scopeMgr.scopedVariable(objectDeclaration));
    final FloatingPointFormula fieldName = strMgr.getStringFormula(pFieldAccess.getFieldName());
    return ctx.propMgr.accessField(objectId, fieldName);
  }

  @Override
  public TypedValue visit(final JSBracketPropertyAccess pPropertyAccess)
      throws UnrecognizedCodeException {
    final JSSimpleDeclaration objectDeclaration =
        ctx.propMgr.getObjectDeclarationOfObjectExpression(pPropertyAccess.getObjectExpression());
    final IntegerFormula objectId =
        typedVarValues.objectValue(ctx.scopeMgr.scopedVariable(objectDeclaration));
    final JSExpression propertyNameExpression = pPropertyAccess.getPropertyNameExpression();
    final TypedValue propertyNameValue = visit(propertyNameExpression);
    final FloatingPointFormula fieldName =
        (propertyNameExpression instanceof JSStringLiteralExpression)
            ? strMgr.getStringFormula(
                ((JSStringLiteralExpression) propertyNameExpression).getValue())
            : valConv.toStringFormula(propertyNameValue);
    final TypedValue propertyValue = ctx.propMgr.accessField(objectId, fieldName);
    final TypedValue lengthProperty =
        ctx.propMgr.accessField(objectId, strMgr.getStringFormula("length"));
    final BooleanFormula isUndefinedArrayElementIndex =
        bfmgr.and(
            ctx.objMgr.isArray(objectId),
            fmgr.makeEqual(propertyNameValue.getType(), typeTags.NUMBER),
            fpfmgr.greaterOrEquals(
                valConv.toNumber(propertyNameValue), valConv.toNumber(lengthProperty)));
    return tvmgr.ifThenElse(isUndefinedArrayElementIndex, tvmgr.getUndefinedValue(), propertyValue);
  }

  private TypedValue handlePredefined(final JSIdExpression pIdExpression) {
    final String name = pIdExpression.getName();
    switch (name) {
      case "Infinity":
        return tvmgr.createNumberValue(fpfmgr.makePlusInfinity(Types.NUMBER_TYPE));
      case "NaN":
        if (!jsOptions.useNaN) {
          logger.log(
              Level.WARNING,
              "NaN is used in "
                  + pIdExpression.getFileLocation()
                  + " even though cpa.predicate.js.useNaN=false");
        }
        return tvmgr.createNumberValue(fpfmgr.makeNaN(Types.NUMBER_TYPE));
      default:
        logger.logfOnce(
            Level.WARNING,
            "Variable without declaration is not defined on global object (%s): %s",
            pIdExpression.getFileLocation(),
            pIdExpression);
        return tvmgr.getUndefinedValue();
    }
  }

}
