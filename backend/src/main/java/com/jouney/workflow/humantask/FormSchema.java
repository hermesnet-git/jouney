package com.jouney.workflow.humantask;

import java.util.List;

/**
 * Schema declarativo de formulário (spec.md seção 9): Text, Number, Date, Boolean, Select,
 * Textarea.
 */
public record FormSchema(String title, List<Field> fields) {

  public record Field(String key, String label, String type, boolean required) {}
}
