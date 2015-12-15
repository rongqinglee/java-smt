/*
 *  JavaSMT is an API wrapper for a collection of SMT solvers.
 *  This file is part of JavaSMT.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.solver.visitors;

import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaManager;

import java.util.List;

public abstract class DefaultBooleanFormulaVisitor<R> extends BooleanFormulaVisitor<R> {

  protected DefaultBooleanFormulaVisitor(FormulaManager pFmgr) {
    super(pFmgr);
  }

  @Override
  public R visitTrue() {
    throw new UnsupportedOperationException();
  }

  @Override
  public R visitFalse() {
    throw new UnsupportedOperationException();
  }

  @Override
  public R visitAtom(BooleanFormula pAtom) {
    throw new UnsupportedOperationException();
  }

  @Override
  public R visitNot(BooleanFormula pOperand) {
    throw new UnsupportedOperationException();
  }

  @Override
  public R visitAnd(List<BooleanFormula> pOperands) {
    throw new UnsupportedOperationException();
  }

  @Override
  public R visitOr(List<BooleanFormula> pOperands) {
    throw new UnsupportedOperationException();
  }

  @Override
  public R visitEquivalence(BooleanFormula pOperand1, BooleanFormula pOperand2) {
    throw new UnsupportedOperationException();
  }

  @Override
  public R visitImplication(BooleanFormula pOperand1, BooleanFormula pOperand2) {
    throw new UnsupportedOperationException();
  }

  @Override
  public R visitIfThenElse(
      BooleanFormula pCondition, BooleanFormula pThenFormula, BooleanFormula pElseFormula) {
    throw new UnsupportedOperationException();
  }

  @Override
  public R visitForallQuantifier(List<? extends Formula> variables, BooleanFormula body) {
    throw new UnsupportedOperationException();
  }

  @Override
  public R visitExistsQuantifier(List<? extends Formula> variables, BooleanFormula body) {
    throw new UnsupportedOperationException();
  }
}