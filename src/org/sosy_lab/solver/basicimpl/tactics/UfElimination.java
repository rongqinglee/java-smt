/*
 *  JavaSMT is an API wrapper for a collection of SMT solvers.
 *  This file is part of JavaSMT.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.solver.basicimpl.tactics;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.maxBy;

import com.google.common.base.Verify;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.BitvectorFormula;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.FloatingPointFormula;
import org.sosy_lab.solver.api.FloatingPointFormulaManager;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaManager;
import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.FunctionDeclaration;
import org.sosy_lab.solver.api.FunctionDeclarationKind;
import org.sosy_lab.solver.api.NumeralFormula;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.solver.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.solver.visitors.DefaultFormulaVisitor;
import org.sosy_lab.solver.visitors.TraversalProcess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.CheckReturnValue;

/**
 * UfElimination replaces UFs by fresh variables
 * and adds constraints to enforce the functional consistency.
 */
public class UfElimination {

  public static class Result {

    private final BooleanFormula formula;
    private final Map<Formula, Formula> substitutions;

    Result(BooleanFormula pFormula, ImmutableMap<Formula, Formula> pSubstitutions) {
      formula = checkNotNull(pFormula);
      substitutions = checkNotNull(pSubstitutions);
    }

    /**
     * @return the new {@link Formula} without UFs
     */
    public BooleanFormula getFormula() {
      return formula;
    }

    /**
     * @return the substitution used to replace UFs
     */
    public Map<Formula, Formula> getSubstitution() {
      return substitutions;
    }
  }

  private static final UniqueIdGenerator UNIQUE_ID_GENERATOR = new UniqueIdGenerator();

  private static final String prefix = "__UF_fresh_";

  private final BooleanFormulaManager bfmgr;
  private final FormulaManager fmgr;

  public UfElimination(FormulaManager pFmgr) {
    bfmgr = pFmgr.getBooleanFormulaManager();
    fmgr = pFmgr;
  }

  /**
   * Applies the Ackermann transformation to the given {@link Formula}.
   * @param f the {@link Formula} to remove all Ufs from
   * @return the new {@link Formula} and the substitution done during transformation
   */
  public Result eliminateUfs(BooleanFormula f) {
    Multimap<FunctionDeclaration<?>, UninterpretedFunctionApplication> ufs = findUFs(f);
    int depth = getFunctionNestingDepth(f);

    ImmutableMap.Builder<Formula, Formula> substitutionsBuilder = ImmutableMap.builder();
    List<BooleanFormula> extraConstraints = new ArrayList<>();

    for (FunctionDeclaration<?> function : ufs.keySet()) {
      List<UninterpretedFunctionApplication> applications = new ArrayList<>(ufs.get(function));
      for (int idx1 = 0; idx1 < applications.size(); idx1++) {
        UninterpretedFunctionApplication application = applications.get(idx1);

        Formula uf = application.getFormula();
        List<Formula> args = application.getArguments();

        Formula substitution = application.getSubstitution();
        substitutionsBuilder.put(uf, substitution);

        for (int idx2 = idx1 + 1; idx2 < applications.size(); idx2++) {
          UninterpretedFunctionApplication application2 = applications.get(idx2);
          List<Formula> otherArgs = application2.getArguments();

          /**
           * Add constraints to enforce functional consistency.
           */
          Verify.verify(args.size() == otherArgs.size());
          Collection<BooleanFormula> argumentEquility = new ArrayList<>(args.size());
          for (int i = 0; i < args.size(); i++) {
            Formula arg1 = args.get(i);
            Formula arg2 = args.get(i);
            argumentEquility.add(makeEqual(arg1, arg2));
          }

          BooleanFormula functionEquility = makeEqual(substitution, application2.getSubstitution());
          extraConstraints.add(bfmgr.implication(bfmgr.and(argumentEquility), functionEquility));
          }
        }
      }

    // Get rid of UFs.
    ImmutableMap<Formula, Formula> substitutions = substitutionsBuilder.build();
    BooleanFormula formulaNoUFs = fmgr.substitute(f, substitutions);

    // substitute all UFs in the additional constraints,
    // required if UFs are arguments of UFs, e.g. uf(uf(1, 2), 2)
    for (int i = 0; i < depth; i++) {
      extraConstraints =
          extraConstraints
            .stream()
            .map(c -> fmgr.substitute(c, substitutions))
            .collect(Collectors.toList());
    }
    BooleanFormula newFormula = bfmgr.and(formulaNoUFs, bfmgr.and(extraConstraints));
    return new Result(newFormula, substitutions);
  }

  @SuppressWarnings("unchecked")
  @CheckReturnValue
  private BooleanFormula makeEqual(Formula pLhs, Formula pRhs) {
    BooleanFormula t;
    if (pLhs instanceof BooleanFormula && pRhs instanceof BooleanFormula) {
      t = bfmgr.equivalence((BooleanFormula) pLhs, (BooleanFormula) pRhs);
    } else if (pLhs instanceof IntegerFormula && pRhs instanceof IntegerFormula) {
      t = fmgr.getIntegerFormulaManager().equal((IntegerFormula) pLhs, (IntegerFormula) pRhs);
    } else if (pLhs instanceof NumeralFormula && pRhs instanceof NumeralFormula) {
      t = fmgr.getRationalFormulaManager().equal((NumeralFormula) pLhs, (NumeralFormula) pRhs);
    } else if (pLhs instanceof BitvectorFormula) {
      t = fmgr.getBitvectorFormulaManager().equal((BitvectorFormula) pLhs, (BitvectorFormula) pRhs);
    } else if (pLhs instanceof FloatingPointFormula && pRhs instanceof FloatingPointFormula) {
      FloatingPointFormulaManager fpfmgr = fmgr.getFloatingPointFormulaManager();
      t = fpfmgr.equalWithFPSemantics((FloatingPointFormula) pLhs, (FloatingPointFormula) pRhs);
    } else if (pLhs instanceof ArrayFormula<?, ?> && pRhs instanceof ArrayFormula<?, ?>) {
      ArrayFormula<?, ?> lhs = (ArrayFormula<?, ?>) pLhs;
      @SuppressWarnings("rawtypes")
      ArrayFormula rhs = (ArrayFormula) pRhs;
      t = fmgr.getArrayFormulaManager().equivalence(lhs, rhs);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return t;
  }

  private int getFunctionNestingDepth(Formula f) {
    return fmgr.visit(f, new DefaultFormulaVisitor<Integer>() {

      @Override
      protected Integer visitDefault(Formula pF) {
        return 0;
      }

      @Override
      public Integer visitFunction(Formula pF, List<Formula> pArgs,
          FunctionDeclaration<?> pFunctionDeclaration) {
        return
            pArgs
              .stream()
              .map(f -> fmgr.visit(f, this) + 1)
              .collect(maxBy(Integer::compareTo))
              .orElse(0);
      }

      @Override
      public Integer visitQuantifier(BooleanFormula pF, Quantifier pQ,
          List<Formula> pBoundVariables, BooleanFormula pBody) {
        return fmgr.visit(pBody, this);
      }

    });
  }

  private Multimap<FunctionDeclaration<?>, UninterpretedFunctionApplication> findUFs(Formula f) {
    Multimap<FunctionDeclaration<?>, UninterpretedFunctionApplication> ufs = HashMultimap.create();

    fmgr.visitRecursively(f, new DefaultFormulaVisitor<TraversalProcess>() {
      @Override
      protected TraversalProcess visitDefault(Formula f) {
        return TraversalProcess.CONTINUE;
      }

      @Override
      public TraversalProcess visitFunction(Formula f,
          List<Formula> args,
          FunctionDeclaration<?> decl) {
        if (decl.getKind() == FunctionDeclarationKind.UF) {
          Formula substitution = freshUfReplaceVariable(decl.getType());
          ufs.put(decl, new UninterpretedFunctionApplication(f, args, substitution));

        }
        return TraversalProcess.CONTINUE;
      }
    });

    return ufs;
  }

  private Formula freshUfReplaceVariable(FormulaType<?> pType) {
    return fmgr.makeVariable(pType, prefix + UNIQUE_ID_GENERATOR.getFreshId());
  }

  private static class UninterpretedFunctionApplication {

    private final Formula formula;
    private final List<Formula> arguments;
    private final Formula substitution;

    UninterpretedFunctionApplication(Formula pF, List<Formula> pArguments, Formula pSubstitution) {
      formula = checkNotNull(pF);
      arguments = checkNotNull(pArguments);
      substitution = checkNotNull(pSubstitution);
    }

    public Formula getFormula() {
      return formula;
    }

    public List<Formula> getArguments() {
      return arguments;
    }

    public Formula getSubstitution() {
      return substitution;
    }
  }
}
