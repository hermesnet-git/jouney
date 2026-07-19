import { useEffect, useState } from 'react'
import type { Edge, Node } from 'reactflow'
import { connectorsApi, type ConnectorSummary } from '../../api/connectors'
import type { WorkflowNodeData } from '../nodes/WorkflowNodeShape'
import './properties-panel.css'

interface PropertiesPanelProps {
  selectedNode: Node<WorkflowNodeData> | null
  selectedEdge: Edge | null
  onChangeNode: (nodeId: string, patch: Partial<WorkflowNodeData>) => void
  onChangeEdgeCondition: (edgeId: string, condition: string) => void
  onChangeEdgePublishEvent: (edgeId: string, publishEvent: Record<string, unknown> | null) => void
}

/**
 * T028/T058 — painel de propriedades: campos genéricos do MVP (Início, Fim, Tarefa de Usuário,
 * Gateway/condição de aresta) com navegação por teclado (constitution II), mais os campos do
 * conector REST (T058, US4: timeout, tentativas, mapeamento).
 */
export function PropertiesPanel({
  selectedNode,
  selectedEdge,
  onChangeNode,
  onChangeEdgeCondition,
  onChangeEdgePublishEvent,
}: PropertiesPanelProps) {
  const [connectors, setConnectors] = useState<ConnectorSummary[]>([])

  useEffect(() => {
    connectorsApi.list().then(setConnectors)
  }, [])

  if (selectedEdge) {
    return (
      <aside className="properties-panel" aria-label="Propriedades da conexão">
        <h2>Conexão</h2>
        <label htmlFor="edge-condition">Condição (opcional, ex.: customer.age &gt;= 18)</label>
        <input
          id="edge-condition"
          type="text"
          defaultValue={(selectedEdge.data?.condition as string) ?? ''}
          onBlur={(e) => onChangeEdgeCondition(selectedEdge.id, e.target.value)}
        />

        <label htmlFor="edge-publish-event">
          Publicar evento (opcional, JSON: connectorId/topic/payload — FR-018)
        </label>
        <textarea
          id="edge-publish-event"
          rows={4}
          defaultValue={
            selectedEdge.data?.publishEvent ? JSON.stringify(selectedEdge.data.publishEvent, null, 2) : ''
          }
          onBlur={(e) => {
            const raw = e.target.value.trim()
            if (!raw) {
              onChangeEdgePublishEvent(selectedEdge.id, null)
              return
            }
            try {
              onChangeEdgePublishEvent(selectedEdge.id, JSON.parse(raw))
            } catch {
              // JSON inválido: mantém o valor anterior até o usuário corrigir
            }
          }}
        />
      </aside>
    )
  }

  if (!selectedNode) {
    return (
      <aside className="properties-panel" aria-label="Propriedades da etapa">
        <p>Selecione uma etapa ou conexão para editar.</p>
      </aside>
    )
  }

  const { data } = selectedNode

  return (
    <aside className="properties-panel" aria-label="Propriedades da etapa">
      <h2>{data.label || 'Etapa'}</h2>

      <label htmlFor="node-name">Nome</label>
      <input
        id="node-name"
        type="text"
        value={data.label}
        onChange={(e) => onChangeNode(selectedNode.id, { label: e.target.value })}
      />

      {data.nodeType === 'USER_TASK' && (
        <>
          <label htmlFor="node-form-key">Formulário</label>
          <input
            id="node-form-key"
            type="text"
            placeholder="ex.: personal-data"
            value={(data.configuration.formKey as string) ?? ''}
            onChange={(e) =>
              onChangeNode(selectedNode.id, { configuration: { ...data.configuration, formKey: e.target.value } })
            }
          />

          <label htmlFor="node-assignment">Atribuição</label>
          <select
            id="node-assignment"
            value={((data.configuration.assignment as { type?: string })?.type as string) ?? 'INITIATOR'}
            onChange={(e) =>
              onChangeNode(selectedNode.id, {
                configuration: { ...data.configuration, assignment: { type: e.target.value } },
              })
            }
          >
            <option value="INITIATOR">Quem iniciou a execução</option>
            <option value="GROUP">Grupo</option>
          </select>
        </>
      )}

      {data.nodeType === 'REST_SERVICE_TASK' && (
        <>
          <label htmlFor="node-connector">Conector</label>
          <select
            id="node-connector"
            value={(data.configuration.connectorId as string) ?? ''}
            onChange={(e) =>
              onChangeNode(selectedNode.id, { configuration: { ...data.configuration, connectorId: e.target.value } })
            }
          >
            <option value="">Selecione…</option>
            {connectors.map((c) => (
              <option key={c.id} value={c.id}>
                {c.id}
              </option>
            ))}
          </select>

          <label htmlFor="node-operation">Operação (ex.: GET /customers/{'{id}'})</label>
          <input
            id="node-operation"
            type="text"
            value={(data.configuration.operation as string) ?? ''}
            onChange={(e) =>
              onChangeNode(selectedNode.id, { configuration: { ...data.configuration, operation: e.target.value } })
            }
          />

          <label htmlFor="node-timeout">Timeout (segundos)</label>
          <input
            id="node-timeout"
            type="number"
            value={(data.configuration.timeoutSeconds as number) ?? 5}
            onChange={(e) =>
              onChangeNode(selectedNode.id, {
                configuration: { ...data.configuration, timeoutSeconds: e.target.valueAsNumber },
              })
            }
          />

          <label htmlFor="node-max-attempts">Tentativas</label>
          <input
            id="node-max-attempts"
            type="number"
            value={(data.configuration.maxAttempts as number) ?? 3}
            onChange={(e) =>
              onChangeNode(selectedNode.id, {
                configuration: { ...data.configuration, maxAttempts: e.target.valueAsNumber },
              })
            }
          />
        </>
      )}

      {data.nodeType === 'EXCLUSIVE_GATEWAY' && (
        <p className="properties-panel__hint">
          Configure a condição de cada caminho de saída clicando na conexão correspondente.
        </p>
      )}
    </aside>
  )
}
