/*
 *  JavaSMT is an API wrapper for a collection of SMT solvers.
 *  This file is part of JavaSMT.
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
package org.sosy_lab.java_smt.solvers.wrapper.caching;

import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

public class InMemorySMTCache implements SMTCache, Serializable {

  private static final long serialVersionUID = 1L;

  private final Map<Integer, Boolean> unsatMap = new HashMap<>();

  private final Map<Integer, Model> modelMap = new HashMap<>();

  private final Map<Integer, ImmutableList<ValueAssignment>> assignmentMap = new HashMap<>();

  private final Map<Integer, List<BooleanFormula>> usatCoreMap = new HashMap<>();

  private final Map<Integer, BooleanFormula> interpolantMap = new HashMap<>();

  private final Map<Integer, List<BooleanFormula>> treeInterpolantMap = new HashMap<>();

  private final Map<Integer, Rational> upperMap = new HashMap<>();

  private final Map<Integer, Rational> lowerMap = new HashMap<>();

  private final static int prime = 7;

  private Map<Integer, Integer> maxMap = new HashMap<>();

  private Map<Integer, Integer> minMap = new HashMap<>();

  @Override
  public Boolean storeFormulaUnsat(BooleanFormula pFormula, boolean pUnsat) {
    return unsatMap.put(pFormula.hashCode(), pUnsat);
  }

  @Override
  public Boolean isFormulaUnsat(BooleanFormula pFormula) {
    return unsatMap.get(pFormula.hashCode());
  }

  @Override
  public Boolean storeFormulaUnsatWithAssumptions(
      BooleanFormula pFormula,
      boolean pUnsat,
      Collection<BooleanFormula> pAssumptions) {
    return unsatMap.put(pFormula.hashCode() + prime * pAssumptions.hashCode(), pUnsat);
  }

  @Override
  public Boolean isFormulaUnsatWithAssumptions(
      BooleanFormula pFormula,
      Collection<BooleanFormula> pAssumptions) {
    return unsatMap.get(pFormula.hashCode() + prime * pAssumptions.hashCode());
  }

  @Override
  public Model storeFormulaModel(BooleanFormula pFormula, Model pModel) {
    return modelMap.put(pFormula.hashCode(), pModel);
  }

  @Override
  public Model getFormulaModel(BooleanFormula pFormula) {
    return modelMap.get(pFormula.hashCode());
  }

  @Override
  public ImmutableList<ValueAssignment> storeFormulaModelAssignments(
      BooleanFormula pFormula,
      ImmutableList<ValueAssignment> pAssignments) {
    return assignmentMap.put(pFormula.hashCode(), pAssignments);
  }

  @Override
  public ImmutableList<ValueAssignment> getFormulaModelAssignments(BooleanFormula pFormula) {
    return assignmentMap.get(pFormula.hashCode());
  }

  @Override
  public List<BooleanFormula>
      storeFormulaUnsatCore(BooleanFormula pFormula, List<BooleanFormula> pUnsatCore) {
    return usatCoreMap.put(pFormula.hashCode(), pUnsatCore);
  }

  @Override
  public List<BooleanFormula> getFormulaUnsatCore(BooleanFormula pFormula) {
    return usatCoreMap.get(pFormula.hashCode());
  }

  @Override
  public Optional<List<BooleanFormula>> storeFormulaUnsatCoreOverAssumptions(
      BooleanFormula pFormula,
      Optional<List<BooleanFormula>> pUnsatCore,
      Collection<BooleanFormula> pAssumptions) {
    Collection<BooleanFormula> unsatCore = null;
    if (pUnsatCore.isPresent()) {
      unsatCore = pUnsatCore.get();
    }
    unsatCore =
        usatCoreMap
            .put(pFormula.hashCode() + prime * pAssumptions.hashCode(), new ArrayList<>(unsatCore));
    return optionalList(unsatCore);
  }

  @Override
  public Optional<List<BooleanFormula>> getFormulaUnsatCoreOverAssumptions(
      BooleanFormula pFormula,
      Collection<BooleanFormula> pAssumptions) {
    Collection<BooleanFormula> unsatCore =
        usatCoreMap.get(pFormula.hashCode() + prime * pAssumptions.hashCode());
    return optionalList(unsatCore);
  }

  private Optional<List<BooleanFormula>> optionalList(Collection<BooleanFormula> unsatCore) {
    if (unsatCore != null) {
      return Optional.of(new ArrayList<>(unsatCore));
    } else {
      return Optional.empty();
    }
  }

  @Override
  public BooleanFormula storeFormulaInterpolant(
      BooleanFormula pFormula,
      BooleanFormula pInterpolant,
      Collection<?> pFormulasOfA) {
    return interpolantMap.put(pFormula.hashCode() + prime * pFormulasOfA.hashCode(), pInterpolant);
  }

  @Override
  public BooleanFormula getFormulaInterpolant(BooleanFormula pFormula, Collection<?> pFormulasOfA) {
    return interpolantMap.get(pFormula.hashCode() + prime * pFormulasOfA.hashCode());
  }

  @Override
  public List<BooleanFormula> storeFormulaTreeInterpolants(
      BooleanFormula pFormula,
      List<BooleanFormula> pTreeInterpolants,
      List<? extends Collection<?>> pPartitionedFormulas,
      int[] pStartOfSubTree) {
    int key = pFormula.hashCode();
    key += prime * pPartitionedFormulas.hashCode();
    key += prime * Arrays.hashCode(pStartOfSubTree);
    return treeInterpolantMap.put(key, pTreeInterpolants);
  }

  @Override
  public List<BooleanFormula> getFormulaTreeInterpolants(
      BooleanFormula pFormula,
      List<? extends Collection<?>> pPartitionedFormulas,
      int[] pStartOfSubTree) {
    int key = pFormula.hashCode();
    key += prime * pPartitionedFormulas.hashCode();
    key += prime * Arrays.hashCode(pStartOfSubTree);
    return treeInterpolantMap.get(key);
  }

  @Override
  public Integer storeFormulaMaximize(BooleanFormula pFormula, Integer pMax, Formula pObjective) {
    return maxMap.put(pFormula.hashCode(), pMax);
  }

  @Override
  public Integer getFormulaMaximize(BooleanFormula pFormula, Formula pObjective) {
    return maxMap.get(pFormula.hashCode());
  }

  @Override
  public Integer storeFormulaMinimize(BooleanFormula pFormula, Integer pMin, Formula pObjective) {
    return minMap.put(pFormula.hashCode(), pMin);
  }

  @Override
  public Integer getFormulaMinimize(BooleanFormula pFormula, Formula pObjective) {
    return minMap.get(pFormula.hashCode());
  }

  @Override
  public Optional<Rational> storeFormulaUpper(
      BooleanFormula pFormula,
      Optional<Rational> pUpper,
      int pHandle,
      Rational pEpsilon) {
    int key = pFormula.hashCode();
    key += prime * pEpsilon.hashCode();
    key += prime * pHandle;
    Rational last = null;
    if (pUpper.isPresent()) {
      last = upperMap.put(key, pUpper.get());
    }
    return Optional.ofNullable(last);
  }

  @Override
  public Optional<Rational>
      getFormulaUpper(BooleanFormula pFormula, int pHandle, Rational pEpsilon) {
    int key = pFormula.hashCode();
    key += prime * pEpsilon.hashCode();
    key += prime * pHandle;
    Rational value = upperMap.get(key);
    return Optional.ofNullable(value);
  }

  @Override
  public Optional<Rational> storeFormulaLower(
      BooleanFormula pFormula,
      Optional<Rational> pLower,
      int pHandle,
      Rational pEpsilon) {
    int key = pFormula.hashCode();
    key += prime * pEpsilon.hashCode();
    key += prime * pHandle;
    Rational last = null;
    if (pLower.isPresent()) {
      last = lowerMap.put(key, pLower.get());
    }
    return Optional.ofNullable(last);
  }

  @Override
  public Optional<Rational>
      getFormulaLower(BooleanFormula pFormula, int pHandle, Rational pEpsilon) {
    int key = pFormula.hashCode();
    key += prime * pEpsilon.hashCode();
    key += prime * pHandle;
    Rational value = lowerMap.get(key);
    return Optional.ofNullable(value);
  }

  @Override
  public void close() {
    // Nothing to do
  }
}