package com.jouney.workflow.runtime;

import java.util.HashMap;
import java.util.Map;

/** Variáveis da execução acessíveis aos NodeExecutor (mapeamentos, condições — seções 3.6/7). */
public class ExecutionContext {

  private final Map<String, Object> variables;

  public ExecutionContext(Map<String, Object> variables) {
    this.variables = new HashMap<>(variables);
  }

  public Object get(String key) {
    return variables.get(key);
  }

  public void putAll(Map<String, Object> values) {
    if (values != null) {
      variables.putAll(values);
    }
  }

  public Map<String, Object> asMap() {
    return variables;
  }
}
