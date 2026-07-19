package com.jouney.workflow.expression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** T059 — avaliador de condições declarativas (seção 3.6). */
class ExpressionEvaluatorTest {

  private final ExpressionEvaluator evaluator = new ExpressionEvaluator();

  @Test
  void evaluatesNumericComparison() {
    assertThat(evaluator.evaluate("age >= 18", Map.of("age", 20))).isTrue();
    assertThat(evaluator.evaluate("age >= 18", Map.of("age", 10))).isFalse();
  }

  @Test
  void evaluatesStringEquality() {
    assertThat(
            evaluator.evaluate(
                "validationResult == 'APPROVED'", Map.of("validationResult", "APPROVED")))
        .isTrue();
    assertThat(
            evaluator.evaluate(
                "validationResult == 'APPROVED'", Map.of("validationResult", "REJECTED")))
        .isFalse();
  }

  @Test
  void evaluatesNestedPath() {
    Map<String, Object> vars = Map.of("customer", Map.of("age", 25, "type", "PREMIUM"));
    assertThat(evaluator.evaluate("customer.age >= 18", vars)).isTrue();
    assertThat(evaluator.evaluate("customer.type == 'PREMIUM'", vars)).isTrue();
  }

  @Test
  void evaluatesAndCombination() {
    Map<String, Object> vars = Map.of("amount", 1500, "type", "PREMIUM");
    assertThat(evaluator.evaluate("amount > 1000 && type == 'PREMIUM'", vars)).isTrue();
    assertThat(evaluator.evaluate("amount > 1000 && type == 'BASIC'", vars)).isFalse();
  }

  @Test
  void blankExpressionIsAlwaysTrue() {
    assertThat(evaluator.evaluate("", Map.of())).isTrue();
    assertThat(evaluator.evaluate(null, Map.of())).isTrue();
  }

  @Test
  void malformedExpressionThrows() {
    assertThatThrownBy(() -> evaluator.evaluate("not a condition", Map.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
