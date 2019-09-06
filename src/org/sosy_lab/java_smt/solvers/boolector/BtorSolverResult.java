package org.sosy_lab.java_smt.solvers.boolector;
/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.0
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */


public final class BtorSolverResult {
  public final static BtorSolverResult BTOR_RESULT_SAT = new BtorSolverResult("BTOR_RESULT_SAT", BtorJNI.BTOR_RESULT_SAT_get());
  public final static BtorSolverResult BTOR_RESULT_UNSAT = new BtorSolverResult("BTOR_RESULT_UNSAT", BtorJNI.BTOR_RESULT_UNSAT_get());
  public final static BtorSolverResult BTOR_RESULT_UNKNOWN = new BtorSolverResult("BTOR_RESULT_UNKNOWN", BtorJNI.BTOR_RESULT_UNKNOWN_get());

  public final int swigValue() {
    return swigValue;
  }

  @Override
  public String toString() {
    return swigName;
  }

  public static BtorSolverResult swigToEnum(int swigValue) {
    if (swigValue < swigValues.length && swigValue >= 0 && swigValues[swigValue].swigValue == swigValue) {
      return swigValues[swigValue];
    }
    for (int i = 0; i < swigValues.length; i++) {
      if (swigValues[i].swigValue == swigValue) {
        return swigValues[i];
      }
    }
    throw new IllegalArgumentException("No enum " + BtorSolverResult.class + " with value " + swigValue);
  }

  private BtorSolverResult(String swigName) {
    this.swigName = swigName;
    this.swigValue = swigNext++;
  }

  private BtorSolverResult(String swigName, int swigValue) {
    this.swigName = swigName;
    this.swigValue = swigValue;
    swigNext = swigValue+1;
  }

  private BtorSolverResult(String swigName, BtorSolverResult swigEnum) {
    this.swigName = swigName;
    this.swigValue = swigEnum.swigValue;
    swigNext = this.swigValue+1;
  }

  private static BtorSolverResult[] swigValues = { BTOR_RESULT_SAT, BTOR_RESULT_UNSAT, BTOR_RESULT_UNKNOWN };
  private static int swigNext = 0;
  private final int swigValue;
  private final String swigName;
}

