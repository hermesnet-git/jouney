package com.jouney.workflow.connector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * T054 — implementação REST do ConnectorExecutor (FR-014/FR-015). Timeout obrigatório
 * (constitution/seção 13); URL vem só da configuração do conector, nunca de entrada do usuário em
 * tempo de execução (proteção contra SSRF).
 */
@Component
public class RestConnectorExecutor implements ConnectorExecutor {

  private static final Pattern PATH_PARAM = Pattern.compile("\\{(\\w+)}");

  private final ObjectMapper objectMapper;

  public RestConnectorExecutor(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean supports(String connectorType) {
    return "REST".equals(connectorType);
  }

  @Override
  public ConnectorResult execute(
      ConnectorDefinition connector,
      String operation,
      Map<String, Object> input,
      Duration timeout,
      String idempotencyKey) {
    try {
      Map<String, Object> baseConfig =
          objectMapper.readValue(
              connector.getBaseConfigJson(), new TypeReference<Map<String, Object>>() {});
      String baseUrl = String.valueOf(baseConfig.getOrDefault("baseUrl", ""));

      String[] methodAndPath = operation.trim().split("\\s+", 2);
      String method = methodAndPath.length == 2 ? methodAndPath[0] : "GET";
      String path = methodAndPath.length == 2 ? methodAndPath[1] : methodAndPath[0];
      String resolvedPath = resolvePathParams(path, input);

      HttpClient client = HttpClient.newBuilder().connectTimeout(timeout).build();
      HttpRequest.Builder requestBuilder =
          HttpRequest.newBuilder()
              .uri(URI.create(baseUrl + resolvedPath))
              .timeout(timeout)
              .header("Idempotency-Key", idempotencyKey)
              .header("Content-Type", "application/json");

      String body = "GET".equalsIgnoreCase(method) ? null : objectMapper.writeValueAsString(input);
      requestBuilder =
          switch (method.toUpperCase()) {
            case "POST" -> requestBuilder.POST(BodyPublishers.ofString(body));
            case "PUT" -> requestBuilder.PUT(BodyPublishers.ofString(body));
            default -> requestBuilder.GET();
          };

      HttpResponse<String> response = client.send(requestBuilder.build(), BodyHandlers.ofString());

      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        Map<String, Object> output =
            response.body() == null || response.body().isBlank()
                ? Map.of()
                : objectMapper.readValue(
                    response.body(), new TypeReference<Map<String, Object>>() {});
        return ConnectorResult.success(output);
      }
      boolean temporary = response.statusCode() >= 500;
      return ConnectorResult.failure(
          "Serviço externo respondeu HTTP " + response.statusCode(), temporary);
    } catch (java.net.http.HttpTimeoutException e) {
      return ConnectorResult.failure("Tempo limite excedido ao chamar o serviço externo.", true);
    } catch (java.io.IOException e) {
      return ConnectorResult.failure(
          "Falha de comunicação com o serviço externo: " + e.getMessage(), true);
    } catch (Exception e) {
      return ConnectorResult.failure("Erro ao chamar o serviço externo: " + e.getMessage(), false);
    }
  }

  private String resolvePathParams(String path, Map<String, Object> input) {
    Matcher matcher = PATH_PARAM.matcher(path);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      Object value = input.get(matcher.group(1));
      matcher.appendReplacement(
          result, value != null ? Matcher.quoteReplacement(String.valueOf(value)) : "");
    }
    matcher.appendTail(result);
    return result.toString();
  }
}
