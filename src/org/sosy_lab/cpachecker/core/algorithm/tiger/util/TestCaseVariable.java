/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.core.algorithm.tiger.util;

public class TestCaseVariable {
  private String name;
  private String value;

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public String getFullName() {
    return name;
  }

  public TestCaseVariable(String name, String value) {
      this.name = name;
      this.value = value;
  }

  @Override
  public int hashCode() {
    return 25 * name.hashCode() + 13 * value.hashCode();
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (!(pObj instanceof TestCaseVariable)) {
      return false;
    }
    TestCaseVariable other = (TestCaseVariable) pObj;
    if (!this.name.equals(other.name)) {
      return false;
    }
    if (!this.value.equals(other.value)) {
      return false;
    }
    return true;
  }

}