package com.jouney.workflow.humantask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FR-012 — bloqueia a conclusão com uma mensagem clara quando um campo obrigatório está ausente.
 */
@Component
public class FormValidator {

  public List<String> validate(FormSchema schema, Map<String, Object> formData) {
    List<String> problems = new ArrayList<>();
    for (FormSchema.Field field : schema.fields()) {
      if (!field.required()) {
        continue;
      }
      Object value = formData == null ? null : formData.get(field.key());
      if (value == null || value.toString().isBlank()) {
        problems.add("Campo obrigatório não preenchido: " + field.label());
      }
    }
    return problems;
  }
}
