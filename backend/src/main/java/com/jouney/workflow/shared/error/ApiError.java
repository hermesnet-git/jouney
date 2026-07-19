package com.jouney.workflow.shared.error;

import java.util.List;

/** Formato único de erro da API (contracts/api.md — seção "Erros"). */
public record ApiError(String code, String message, List<String> details) {

  public static ApiError of(String code, String message) {
    return new ApiError(code, message, List.of());
  }

  public static ApiError of(String code, String message, List<String> details) {
    return new ApiError(code, message, details);
  }
}
