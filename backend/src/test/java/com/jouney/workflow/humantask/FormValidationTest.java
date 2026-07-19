package com.jouney.workflow.humantask;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** T041 — FR-012: campo obrigatório ausente é reportado com clareza. */
class FormValidationTest {

  private final FormValidator validator = new FormValidator();

  private final FormSchema schema =
      new FormSchema(
          "Dados pessoais",
          List.of(
              new FormSchema.Field("name", "Nome", "text", true),
              new FormSchema.Field("nickname", "Apelido", "text", false)));

  @Test
  void missingRequiredFieldIsReported() {
    List<String> problems = validator.validate(schema, Map.of());
    assertThat(problems).anyMatch(p -> p.contains("Nome"));
  }

  @Test
  void blankRequiredFieldIsReported() {
    List<String> problems = validator.validate(schema, Map.of("name", "  "));
    assertThat(problems).isNotEmpty();
  }

  @Test
  void allRequiredFieldsPresentPassesValidation() {
    List<String> problems = validator.validate(schema, Map.of("name", "Alice"));
    assertThat(problems).isEmpty();
  }

  @Test
  void optionalFieldMissingDoesNotFail() {
    List<String> problems = validator.validate(schema, Map.of("name", "Alice"));
    assertThat(problems).isEmpty();
  }
}
