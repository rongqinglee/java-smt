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
package org.sosy_lab.java_smt.solvers.boolector;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Longs;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.ArrayFormulaType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager;
import org.sosy_lab.java_smt.api.visitors.FormulaVisitor;
import org.sosy_lab.java_smt.basicimpl.FormulaCreator;
import org.sosy_lab.java_smt.basicimpl.FunctionDeclarationImpl;
import org.sosy_lab.java_smt.solvers.boolector.BoolectorFormula.BoolectorArrayFormula;
import org.sosy_lab.java_smt.solvers.boolector.BoolectorFormula.BoolectorBitvectorFormula;
import org.sosy_lab.java_smt.solvers.boolector.BoolectorFormula.BoolectorBooleanFormula;
import org.sosy_lab.java_smt.solvers.boolector.BoolectorQuantifiedFormulaManager.QuantifiedFormula;

public class BoolectorFormulaCreator
    extends FormulaCreator<Long, Long, BoolectorEnvironment, Long> {

  // Boolector can't give back the used args of an uf, just a string, so we save it ourself.
  private Map<Long, Long[]> ufMap = new HashMap<>();
  // Mapping the internal unique variable name in boolector to the JavaSMT name
  private Map<String, String> newNameNameMap = new HashMap<>();

  BoolectorFormulaCreator(BoolectorEnvironment pEnv) {
    super(pEnv, pEnv.getBoolSort(), null, null);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Formula> FormulaType<T> getFormulaType(T pFormula) {
    long btor = getEnv().getBtor();
    if (pFormula instanceof BitvectorFormula) {
      long sort = BtorJNI.boolector_get_sort(btor, extractInfo(pFormula));
      checkArgument(
          BtorJNI.boolector_is_bitvec_sort(btor, sort),
          "BitvectorFormula with type missmatch: " + pFormula);
      return (FormulaType<T>) FormulaType
          .getBitvectorTypeWithSize(BtorJNI.boolector_get_width(btor, extractInfo(pFormula)));
    } else if (pFormula instanceof ArrayFormula<?, ?>) {
      FormulaType<T> arrayIndexType = getArrayFormulaIndexType((ArrayFormula<T, T>) pFormula);
      FormulaType<T> arrayElementType = getArrayFormulaElementType((ArrayFormula<T, T>) pFormula);
      return (FormulaType<T>) FormulaType.getArrayType(arrayIndexType, arrayElementType);
    }
    return super.getFormulaType(pFormula);
  }

  @Override
  public Long extractInfo(Formula pT) {
    return BoolectorFormulaManager.getBtorTerm(pT);
  }

  @Override
  public FormulaType<?> getFormulaType(Long pFormula) {
    long btor = getEnv().getBtor();
    long sort = BtorJNI.boolector_get_sort(btor, pFormula);
    if (BtorJNI.boolector_is_bitvec_sort(btor, sort)) {
      if (sort == 1) {
        return FormulaType.BooleanType;
      } else {
        return FormulaType
            .getBitvectorTypeWithSize(BtorJNI.boolector_get_width(btor, pFormula));
      }
    } else if (BtorJNI.boolector_is_array_sort(btor, sort)) {
      int indexWidth = BtorJNI.boolector_get_index_width(btor, pFormula);
      int elementWidth = BtorJNI.boolector_get_width(btor, pFormula);
      return FormulaType.getArrayType(
          FormulaType.getBitvectorTypeWithSize(indexWidth),
          FormulaType.getBitvectorTypeWithSize(elementWidth));
    }
    throw new IllegalArgumentException("Unknown formula type for " + pFormula);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Formula> T encapsulate(FormulaType<T> pType, Long pTerm) {
    assert pType.equals(getFormulaType(pTerm)) : String
        .format("Trying to encapsulate formula of type %s as %s", getFormulaType(pTerm), pType);
    if (pType.isBooleanType()) {
      return (T) new BoolectorBooleanFormula(
          pTerm);
    } else if (pType.isArrayType()) {
      ArrayFormulaType<?, ?> arrFt = (ArrayFormulaType<?, ?>) pType;
      return (T) new BoolectorArrayFormula<>(pTerm, arrFt.getIndexType(), arrFt.getElementType());
    } else if (pType.isBitvectorType()) {
      return (T) new BoolectorBitvectorFormula(pTerm);
    }
    throw new IllegalArgumentException(
        "Cannot create formulas of type " + pType + " in Boolector.");
  }

  @Override
  public BooleanFormula encapsulateBoolean(Long pTerm) {
    assert getFormulaType(pTerm).isBooleanType();
    return new BoolectorBooleanFormula(pTerm);
  }

  @Override
  public BitvectorFormula encapsulateBitvector(Long pTerm) {
    assert getFormulaType(pTerm).isBitvectorType();
    return new BoolectorBitvectorFormula(pTerm);
  }

  @Override
  protected <TI extends Formula, TE extends Formula> ArrayFormula<TI, TE>
      encapsulateArray(Long pTerm, FormulaType<TI> pIndexType, FormulaType<TE> pElementType) {
    assert getFormulaType(pTerm).isArrayType();
    return new BoolectorArrayFormula<>(pTerm, pIndexType, pElementType);
  }



  // In Boolector a type is called a sort
  @Override
  public Long getBitvectorType(int pBitwidth) {
    return BtorJNI.boolector_bitvec_sort(getEnv().getBtor(), pBitwidth);
  }

  @Override
  public Long getFloatingPointType(FloatingPointType pType) {
    throw new UnsupportedOperationException(
        "Boolector does not support floating point operations.");
  }

  @Override
  public Long getArrayType(Long pIndexType, Long pElementType) {
    return BtorJNI.boolector_array_sort(getEnv().getBtor(), pIndexType, pElementType);
  }

  @Override
  public Long makeVariable(Long pType, String varName) {
    String newVarName = varName;
    if (!newNameNameMap.containsKey(varName)) {
      newVarName = getNewVarName(varName);
    }
    newNameNameMap.put(newVarName, varName);
    return BtorJNI.boolector_var(getEnv().getBtor(), pType, newVarName);
  }

  /**
   * Checks whether or not the varName is used and finds a new one. Boolector does not have an
   * internal variables cache.
   *
   * @param javaSMTVarName Variable name used in JavaSmt.
   * @return new variable name.
   */
  private String getNewVarName(String javaSMTVarName) {
    String btorVarName = javaSMTVarName;
    int tail = 1;
    while (!newNameNameMap.containsKey(btorVarName)) {
      btorVarName = javaSMTVarName;
      btorVarName.concat(Integer.toString(tail));
      tail++;
    }
    return btorVarName;
  }

  /**
   * Gives back the variable name used in JavaSMT from the unique name in boolector.
   *
   * @param btorVarName unique variable name used in boolector.
   * @return variable name used in JavaSMT.
   */
  protected String getJavaSMTVarName(String btorVarName) {
    return newNameNameMap.get(btorVarName);
  }

  // This method is a massive problem... you CANT get the value formulas(nodes) of ufs because its
  // only build and used internally in boolector....
  @Override
  public <R> R visit(FormulaVisitor<R> visitor, Formula pFormula, Long pF) {
    QuantifiedFormula bodyVars = BoolectorQuantifiedFormulaManager.getQuantVars(pF);
    if (BtorJNI.boolector_is_const(getEnv().getBtor(), pF)) {
      // Handles all constants (bitvec, bool)
      String f = BtorJNI.boolector_get_bits(getEnv().getBtor(), pF);
      return visitor.visitConstant(pFormula, convertValue(pF, parseBitvector(f)));
    } else if (BtorJNI.boolector_is_array(getEnv().getBtor(), pF)) {
      // array = function?!
    } else if (BtorJNI.boolector_is_uf(getEnv().getBtor(), pF)) {
      // UF
      String[][] ufAssignment = BtorJNI.boolector_uf_assignment_helper(getEnv().getBtor(), pF);
      Long[] ufSorts = ufMap.get(pF);
      int arity = BtorJNI.boolector_get_fun_arity(getEnv().getBtor(), pF);
      ImmutableList.Builder<Formula> args = ImmutableList.builder();
      ImmutableList.Builder<FormulaType<?>> argTypes = ImmutableList.builder();
      for (int i = 0; i < arity; i++) {
        FormulaType<?> argumentType = getFormulaType(arg);
        args.add(encapsulate(argumentType, arg));
        argTypes.add(argumentType);
      }
      return visitor.visitFunction(pFormula, args.build(), FunctionDeclarationImpl.of());
    } else if (BtorJNI.boolector_is_param(getEnv().getBtor(), pF)) {
      // Quantifier var (param)
      int deBruijnIdx; // TODO: how?
      return visitor.visitBoundVariable(pFormula, deBruijnIdx);
    } else if (bodyVars != null) {
      // Quantifier node
      QuantifiedFormulaManager.Quantifier quantifier = QuantifiedFormulaManager.Quantifier.FORALL;
      if (bodyVars.isForall()) {
        quantifier = QuantifiedFormulaManager.Quantifier.EXISTS;
      }
      List<Formula> boundVariables = new ArrayList<>();
      for (long arg : bodyVars.getBoundVariables()) {
        boundVariables.add(encapsulate(getFormulaType(arg), arg));
      }
      long body = bodyVars.getBody();
      FormulaType<?> argumentType = getFormulaType(body);
      // if this isnt working or too much, pass an empty list.
      // But Boolector simply holds not information at all about the quantifier besides the result.
      return visitor
          .visitQuantifier(
              (BoolectorBooleanFormula) pFormula,
              quantifier,
              boundVariables,
              new BoolectorBooleanFormula(body));
    }
      else {
      // must be bitvector var at this point
      return visitor
          .visitFreeVariable(pFormula, getName(getEnv().getBtor(), pF));
    }
  }

  @Override
  public Long callFunctionImpl(Long pDeclaration, List<Long> pArgs) {
    return BtorJNI
        .boolector_apply(getEnv().getBtor(), Longs.toArray(pArgs), pArgs.size(), pDeclaration);
  }

  @Override
  public Long declareUFImpl(String pName, Long pReturnType, List<Long> pArgTypes) {
    long[] funSorts = Longs.toArray(pArgTypes);
    long sort;
    Long[] mapValue;
    if (pArgTypes.isEmpty()) {
      sort = pReturnType;
      mapValue = new Long[1];
      mapValue[0] = sort;
    } else {
      sort = BtorJNI.boolector_fun_sort(getEnv().getBtor(), funSorts, funSorts.length, pReturnType);
      mapValue = new Long[1 + funSorts.length];
      mapValue[0] = pReturnType;
      for (int i = 1; i < funSorts.length + 1; i++) {
        mapValue[i] = funSorts[i - 1];
      }
    }
    Long uf = BtorJNI.boolector_uf(getEnv().getBtor(), sort, pName);
    ufMap.put(uf, mapValue);
    return uf;
  }

  @Override
  public Object convertValue(Long key, Long term) {
    // To get the correct type, we check the width of key.
    int width = BtorJNI.boolector_get_width(getEnv().getBtor(), key);
    if (width == 1) {
      if (term == 1) {
        return true;
      } else if (term == 0) {
        return false;
      } else {
        throw new IllegalArgumentException("Unexpected type: " + key + ", with value: " + term);
      }
    }
    return BigInteger.valueOf(term);
    }


  /**
   * Transforms String bitvec into Long
   *
   * @param bitVec return value of Boolector
   * @return gives back the long version of the bitvector
   */
  private Long parseBitvector(String bitVec) {
    try {
      BigInteger bigInt = new BigInteger(bitVec, 2);
      return bigInt.longValue();
    } catch (NumberFormatException e) {
      char[] charArray = bitVec.toCharArray();
      for (int i = 0; i < charArray.length; i++) {
        if (charArray[i] == 'x') {
          charArray[i] = '1';
        } else if (charArray[i] != '0' && charArray[i] != '1') {
          throw new IllegalArgumentException(
              "Boolector gave back an assignment that is not parseable.");
        }
      }
      bitVec = charArray.toString();
    }
    BigInteger bigInt = new BigInteger(bitVec, 2);
    return bigInt.longValue();
  }

  String getName(long pKey, long btor) {
    return getJavaSMTVarName(BtorJNI.boolector_get_symbol(btor, pKey));
  }

  @Override
  public Object convertValue(Long pF) {
    throw new UnsupportedOperationException(
        "Boolector needs a second term to determine a correct type. Please use the other method.");
  }

  @Override
  protected Long getBooleanVarDeclarationImpl(Long pTFormulaInfo) {
    // declaration of constant or fun
    if(BtorJNI.boolector_is_const(getEnv().getBtor(), pTFormulaInfo)) {
      return parseBitvector(BtorJNI.boolector_get_bits(getEnv().getBtor(), pTFormulaInfo));
    } else if (BtorJNI.boolector_is_var(getEnv().getBtor(), pTFormulaInfo)) {
      return parseBitvector(BtorJNI.boolector_bv_assignment(getEnv().getBtor(), pTFormulaInfo));
    } else {
      throw new IllegalArgumentException(
          "Debug only! getBooleanVarDeclarationImpl in BtorFormulaCreator");
    }
  }

  /**
   * Gives you a map of the UFs and applys existing (key). The value is an array with the first item
   * being the return type (sort), rest is arg types (sorts). Check for null!
   *
   * @return ufMap or null
   */
  protected Map<Long, Long[]> getUfs() {
    return ufMap;
  }

}
