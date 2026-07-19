package com.jouney.workflow.shared.error;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converte exceções de domínio no formato único de erro (contracts/api.md), satisfazendo a
 * constitution II: toda mensagem de erro diz o que aconteceu e o que fazer a seguir.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiError.of("NOT_FOUND", ex.getMessage()));
  }

  @ExceptionHandler(ValidationFailedException.class)
  public ResponseEntity<ApiError> handleValidationFailed(ValidationFailedException ex) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(
            ApiError.of(
                "VALIDATION_FAILED",
                "A operação não pode ser concluída pelos motivos listados em" + " details.",
                ex.getProblems()));
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiError.of("CONFLICT", ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleBeanValidation(MethodArgumentNotValidException ex) {
    List<String> problems =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .toList();
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(ApiError.of("VALIDATION_FAILED", "Dados inválidos.", problems));
  }
}
