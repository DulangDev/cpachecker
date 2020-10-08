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
package org.sosy_lab.cpachecker.cpa.loopacceleration;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;

public class LoopIOState implements AbstractState, Graphable {

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((input == null) ? 0 : input.hashCode());
    result = prime * result + name;
    result = prime * result + ((output == null) ? 0 : output.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    LoopIOState other = (LoopIOState) obj;
    if (input == null) {
      if (other.input != null) {
        return false;
      }
    } else if (!input.equals(other.input)) {
      return false;
    }
    if (name != other.name) {
      return false;
    }
    if (output == null) {
      if (other.output != null) {
        return false;
      }
    } else if (!output.equals(other.output)) {
      return false;
    }
    return true;
  }

  int name;
  int end;
  private List<String> input;
  private List<String> output;

  public LoopIOState() {
    input = new ArrayList<>();
    output = new ArrayList<>();
  }

  public void addToInput(String newVariable) {
    input.add(newVariable);
    if (input.contains(null)) {
      input.remove(null);
    }
  }

  public void addToOutput(String newVariable) {
    output.add(newVariable);
  }

  public void removeFromInput(String oldVariable) {
    // Fehlermeldung falls nicht enthalten ?

    if (input.contains(oldVariable)) {
      input.remove(oldVariable);
    }
  }

  public void removeFromOutput(String oldVariable) {
    // Fehlermeldung falld nicht enthalten ?

    if (output.contains(oldVariable)) {
      output.remove(oldVariable);
    }
  }

  public List<String> getInput() {
    return input;
  }

  public List<String> getOutput() {
    return output;
  }

  public void setInput(List<String> nInput) {
    input = nInput;
  }

  public void setOutput(List<String> nOutput) {
    output = nOutput;
  }

  public void setEnd(int end) {
    this.end = end;
  }

  public int getEnd() {
    return end;
  }

  @Override
  public String toString() {
    String out = "Name: " + name + ", ";

    out += "Input:";

    for (String i : input) {
      out += " ";
      out += i;
    }

    out += " Output:";
    for (String o : output) {
      out += " ";
      out += o;
    }

    return out;
  }

  public void setName(int name) {
    this.name = name;
  }

  public int getName() {
    return name;
  }

  @Override
  public String toDOTLabel() {
    // TODO Auto-generated method stub
    return toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    // TODO Auto-generated method stub
    return false;
  }

}
