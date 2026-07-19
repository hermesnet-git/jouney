package com.jouney.workflow.connector;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * T052 — chamada REST real (contra um servidor HTTP local de teste, sem precisar de Docker) com
 * sucesso e com falha temporária seguida de nova tentativa.
 */
class RestConnectorIntegrationTest {

  private HttpServer server;
  private int port;
  private final AtomicInteger requestCount = new AtomicInteger();

  @BeforeEach
  void startServer() throws Exception {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    port = server.getAddress().getPort();
  }

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void successfulCallReturnsMappedOutput() throws Exception {
    server.createContext(
        "/validate",
        exchange -> {
          requestCount.incrementAndGet();
          byte[] body = "{\"result\":\"APPROVED\"}".getBytes();
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();

    RestConnectorExecutor executor = new RestConnectorExecutor(new ObjectMapper());
    ConnectorDefinition connector =
        new ConnectorDefinition(
            "test", "REST", "{\"baseUrl\":\"http://localhost:" + port + "\"}", "ref");

    ConnectorResult result =
        executor.execute(
            connector, "POST /validate", Map.of("cpf", "123"), Duration.ofSeconds(2), "key-1");

    assertThat(result.success()).isTrue();
    assertThat(result.output()).containsEntry("result", "APPROVED");
    assertThat(requestCount.get()).isEqualTo(1);
  }

  @Test
  void serverErrorIsReportedAsTemporaryFailure() throws Exception {
    server.createContext(
        "/validate",
        exchange -> {
          exchange.sendResponseHeaders(503, -1);
          exchange.close();
        });
    server.start();

    RestConnectorExecutor executor = new RestConnectorExecutor(new ObjectMapper());
    ConnectorDefinition connector =
        new ConnectorDefinition(
            "test", "REST", "{\"baseUrl\":\"http://localhost:" + port + "\"}", "ref");

    ConnectorResult result =
        executor.execute(connector, "POST /validate", Map.of(), Duration.ofSeconds(2), "key-1");

    assertThat(result.success()).isFalse();
    assertThat(result.temporary()).isTrue();
  }

  @Test
  void clientErrorIsReportedAsNonTemporaryFailure() throws Exception {
    server.createContext(
        "/validate",
        exchange -> {
          exchange.sendResponseHeaders(400, -1);
          exchange.close();
        });
    server.start();

    RestConnectorExecutor executor = new RestConnectorExecutor(new ObjectMapper());
    ConnectorDefinition connector =
        new ConnectorDefinition(
            "test", "REST", "{\"baseUrl\":\"http://localhost:" + port + "\"}", "ref");

    ConnectorResult result =
        executor.execute(connector, "POST /validate", Map.of(), Duration.ofSeconds(2), "key-1");

    assertThat(result.success()).isFalse();
    assertThat(result.temporary()).isFalse();
  }
}
