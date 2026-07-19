package com.jouney.workflow.expression;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * FR-017 — linguagem declarativa limitada para condições de gateway (seção 3.6), sem execução de
 * código arbitrário: {@code customer.age >= 18}, {@code a == 'APPROVED' && b > 10}.
 */
@Component
public class ExpressionEvaluator {

  private static final Pattern COMPARISON =
      Pattern.compile("\\s*([\\w.]+)\\s*(>=|<=|==|!=|>|<)\\s*(.+?)\\s*");

  public boolean evaluate(String expression, Map<String, Object> variables) {
    if (expression == null || expression.isBlank()) {
      return true;
    }
    String[] clauses = expression.split("&&");
    for (String clause : clauses) {
      if (!evaluateSingle(clause.trim(), variables)) {
        return false;
      }
    }
    return true;
  }

  private boolean evaluateSingle(String clause, Map<String, Object> variables) {
    Matcher matcher = COMPARISON.matcher(clause);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Condição inválida: " + clause);
    }
    String path = matcher.group(1);
    String operator = matcher.group(2);
    String rawRight = matcher.group(3);

    Object left = resolvePath(path, variables);
    Object right = parseLiteral(rawRight);

    return compare(left, operator, right);
  }

  private Object resolvePath(String path, Map<String, Object> variables) {
    Object current = variables;
    for (String segment : path.split("\\.")) {
      if (!(current instanceof Map<?, ?> map)) {
        return null;
      }
      current = map.get(segment);
    }
    return current;
  }

  private Object parseLiteral(String raw) {
    if ((raw.startsWith("'") && raw.endsWith("'"))
        || (raw.startsWith("\"") && raw.endsWith("\""))) {
      return raw.substring(1, raw.length() - 1);
    }
    if ("true".equals(raw) || "false".equals(raw)) {
      return Boolean.parseBoolean(raw);
    }
    try {
      return Double.parseDouble(raw);
    } catch (NumberFormatException e) {
      return raw;
    }
  }

  @SuppressWarnings("unchecked")
  private boolean compare(Object left, String operator, Object right) {
    if (left instanceof Number || right instanceof Number) {
      double l = toDouble(left);
      double r = toDouble(right);
      return switch (operator) {
        case ">=" -> l >= r;
        case "<=" -> l <= r;
        case ">" -> l > r;
        case "<" -> l < r;
        case "==" -> l == r;
        case "!=" -> l != r;
        default -> throw new IllegalArgumentException("Operador desconhecido: " + operator);
      };
    }
    String l = String.valueOf(left);
    String r = String.valueOf(right);
    return switch (operator) {
      case "==" -> l.equals(r);
      case "!=" -> !l.equals(r);
      default ->
          throw new IllegalArgumentException(
              "Operador '" + operator + "' não suportado para texto.");
    };
  }

  private double toDouble(Object value) {
    if (value instanceof Number n) {
      return n.doubleValue();
    }
    try {
      return Double.parseDouble(String.valueOf(value));
    } catch (NumberFormatException e) {
      return Double.NaN;
    }
  }
}
