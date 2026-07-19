package com.jouney.workflow.shared.error;

import java.util.List;

/** FR-004/FR-012: erro que carrega TODOS os problemas encontrados de uma vez, não só o primeiro. */
public class ValidationFailedException extends RuntimeException {
  private final List<String> problems;

  public ValidationFailedException(List<String> problems) {
    super("Validation failed with " + problems.size() + " problem(s)");
    this.problems = problems;
  }

  public List<String> getProblems() {
    return problems;
  }
}
