// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// Copyright (C) 2007-2020  Dirk Beyer
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;

public class ASTConverterTest {
  private static JClassOrInterfaceType jClassOrInterfaceType;

  @BeforeClass
  public static void init() {
    jClassOrInterfaceType = createStringJClassOrInterfaceType("java.lang.String", "String");
  }

  @Test
  public void testGetClassOfJType() {
    Optional<Class<?>> optionalOfPrimitiveType =
        ASTConverter.getClassOfJType(JSimpleType.getBoolean(), ImmutableSet.of());
    assertThat(optionalOfPrimitiveType.get()).isEqualTo(boolean.class);
  }

  @Test
  public void testGetClassOfPrimitiveType() {
    Optional<Class<?>> optionalOfPrimitiveType =
        ASTConverter.getClassOfPrimitiveType(JSimpleType.getInt());
    assertThat(optionalOfPrimitiveType.get()).isEqualTo(int.class);

    optionalOfPrimitiveType = ASTConverter.getClassOfPrimitiveType(JSimpleType.getLong());
    assertThat(optionalOfPrimitiveType.get()).isEqualTo(long.class);

    optionalOfPrimitiveType = ASTConverter.getClassOfPrimitiveType(JSimpleType.getVoid());
    assertThat(optionalOfPrimitiveType).isEqualTo(Optional.absent());
  }

  @Test
  public void testGetClassOfJTypeForNonPrimitiveType() {
    Optional<Class<?>> optionalOfStringClass =
        ASTConverter.getClassOfJType(jClassOrInterfaceType, ImmutableSet.of());
    assertThat(optionalOfStringClass.get()).isEqualTo(String.class);
  }

  @Test
  public void testGetArrayClass() {
    JArrayType jArrayTypeOfString = new JArrayType(jClassOrInterfaceType, 3);
    Optional<Class<?>> optionalOfArrayClass =
        ASTConverter.getClassOfJType(jArrayTypeOfString, ImmutableSet.of());
    assertThat(optionalOfArrayClass.get().isArray()).isTrue();
    assertThat(optionalOfArrayClass.get().toGenericString()).isEqualTo("java.lang.String[][][]");
  }

  private static JClassOrInterfaceType createStringJClassOrInterfaceType(
      String pFullyQualifiedName, String pString) {
    return JClassType.valueOf(
        pFullyQualifiedName,
        pString,
        VisibilityModifier.PUBLIC,
        true,
        false,
        false,
        JClassType.getTypeOfObject(),
        ImmutableSet.of());
  }
}
