import type { Edge, Node } from 'reactflow'

export interface WorkflowNodeConfig {
  formKey?: string
  assignment?: { type: 'INITIATOR' | 'GROUP'; group?: string }
  connectorId?: string
  operation?: string
  inputMapping?: Record<string, string>
  outputMapping?: Record<string, string>
  timeoutSeconds?: number
  maxAttempts?: number
  [key: string]: unknown
}

export interface WorkflowGraphNode {
  id: string
  type: 'START' | 'END' | 'USER_TASK' | 'REST_SERVICE_TASK' | 'EXCLUSIVE_GATEWAY'
  name: string
  configuration: WorkflowNodeConfig
}

export interface WorkflowGraphEdge {
  id: string
  source: string
  target: string
  condition?: string
  publishEvent?: Record<string, unknown> | null
}

export interface WorkflowGraph {
  workflowKey: string
  name: string
  startNodeId: string | null
  nodes: WorkflowGraphNode[]
  edges: WorkflowGraphEdge[]
}

/** T029/T019 — converte o estado do React Flow (nodes/edges na tela) no JSON canônico (spec.md §5). */
export function toWorkflowGraph(
  workflowKey: string,
  name: string,
  nodes: Node<{ label: string; nodeType: WorkflowGraphNode['type']; configuration: WorkflowNodeConfig }>[],
  edges: Edge<{ condition?: string; publishEvent?: Record<string, unknown> | null }>[],
): WorkflowGraph {
  const startNode = nodes.find((n) => n.data.nodeType === 'START')
  return {
    workflowKey,
    name,
    startNodeId: startNode ? startNode.id : null,
    nodes: nodes.map((n) => ({
      id: n.id,
      type: n.data.nodeType,
      name: n.data.label,
      configuration: n.data.configuration ?? {},
    })),
    edges: edges.map((e) => ({
      id: e.id,
      source: e.source,
      target: e.target,
      condition: e.data?.condition,
      publishEvent: e.data?.publishEvent ?? null,
    })),
  }
}

/** Converte o JSON canônico de volta para nodes/edges do React Flow (ao carregar um rascunho). */
export function fromWorkflowGraph(graph: WorkflowGraph): {
  nodes: Node<{ label: string; nodeType: WorkflowGraphNode['type']; configuration: WorkflowNodeConfig }>[]
  edges: Edge<{ condition?: string; publishEvent?: Record<string, unknown> | null }>[]
} {
  return {
    nodes: graph.nodes.map((n, index) => ({
      id: n.id,
      type: n.type,
      position: { x: (index % 5) * 220, y: Math.floor(index / 5) * 140 },
      data: { label: n.name, nodeType: n.type, configuration: n.configuration },
    })),
    edges: graph.edges.map((e) => ({
      id: e.id,
      source: e.source,
      target: e.target,
      data: { condition: e.condition, publishEvent: e.publishEvent },
      label: e.condition,
    })),
  }
}
