// This file is part of JavaSMT,
// an API wrapper for a collection of SMT solvers:
// https://github.com/sosy-lab/java-smt
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.java_smt.solvers.boolector;

import org.sosy_lab.java_smt.basicimpl.AbstractUFManager;

final class BoolectorUFManager extends AbstractUFManager<Long, Long, Long, Long> {

  BoolectorUFManager(BoolectorFormulaCreator pCreator) {
    super(pCreator);
  }
}
