package com.jouney.workflow.shared.error;

/** Ex.: claim de tarefa já assumida por outro usuário (edge case de concorrência, US3). */
public class ConflictException extends RuntimeException {
  public ConflictException(String message) {
    super(message);
  }
}
