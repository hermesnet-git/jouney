package com.jouney.workflow.connector;

import com.jouney.workflow.shared.error.NotFoundException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ConnectorService {

  private final ConnectorDefinitionRepository repository;
  private final List<ConnectorExecutor> executors;

  public ConnectorService(
      ConnectorDefinitionRepository repository, List<ConnectorExecutor> executors) {
    this.repository = repository;
    this.executors = executors;
  }

  public ConnectorDefinition create(
      String id, String type, String baseConfigJson, String credentialRef) {
    ConnectorDefinition connector =
        new ConnectorDefinition(id, type, baseConfigJson, credentialRef);
    return repository.save(connector);
  }

  public ConnectorDefinition get(String id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Conector " + id + " não encontrado."));
  }

  public List<ConnectorDefinition> list() {
    return repository.findAll();
  }

  public ConnectorDefinition update(String id, String baseConfigJson, String credentialRef) {
    ConnectorDefinition connector = get(id);
    connector.update(baseConfigJson, credentialRef);
    return connector;
  }

  /**
   * T057 — testa a conectividade/credencial referenciada (seção 10) sem afetar dados de negócio.
   */
  public ConnectorResult test(String id, String operation) {
    ConnectorDefinition connector = get(id);
    ConnectorExecutor executor =
        executors.stream().filter(e -> e.supports(connector.getType())).findFirst().orElse(null);
    if (executor == null) {
      return ConnectorResult.failure("Nenhum executor para o tipo " + connector.getType(), false);
    }
    return executor.execute(connector, operation, Map.of(), Duration.ofSeconds(5), "test-" + id);
  }
}
