import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import type { Edge, Node } from 'reactflow'
import { ReactFlowProvider } from 'reactflow'
import { instancesApi } from '../api/instances'
import { workflowsApi } from '../api/workflows'
import { DesignerCanvas } from './canvas/DesignerCanvas'
import type { WorkflowNodeData } from './nodes/WorkflowNodeShape'
import { Palette } from './palette/Palette'
import { PropertiesPanel } from './properties-panel/PropertiesPanel'
import { fromWorkflowGraph, toWorkflowGraph } from './serialization/graph'
import { ValidationPanel } from './validation/ValidationPanel'

/** T031/T026-T030 — tela do designer: canvas + palette + propriedades + validação + publicação. */
export function WorkflowDesignerPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [workflowKey, setWorkflowKey] = useState('')
  const [nodes, setNodes] = useState<Node<WorkflowNodeData>[]>([])
  const [edges, setEdges] = useState<Edge[]>([])
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null)
  const [selectedEdgeId, setSelectedEdgeId] = useState<string | null>(null)
  const [problems, setProblems] = useState<string[] | null>(null)
  const [checking, setChecking] = useState(false)
  const [status, setStatus] = useState<'DRAFT' | 'PUBLISHED'>('DRAFT')
  const [message, setMessage] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    workflowsApi.get(id).then((workflow) => {
      setName(workflow.name)
      setWorkflowKey(workflow.workflowKey)
      setStatus(workflow.status)
      try {
        const graph = JSON.parse(workflow.graphJson)
        if (graph.nodes) {
          const { nodes: rfNodes, edges: rfEdges } = fromWorkflowGraph(graph)
          setNodes(rfNodes)
          setEdges(rfEdges)
        }
      } catch {
        // rascunho novo, sem grafo ainda
      }
    })
  }, [id])

  async function handleSave() {
    if (!id) return
    const graph = toWorkflowGraph(workflowKey, name, nodes, edges)
    await workflowsApi.updateDraft(id, name, null, JSON.stringify(graph))
    setMessage('Rascunho salvo.')
  }

  async function handleValidate() {
    if (!id) return
    await handleSave()
    setChecking(true)
    try {
      const result = await workflowsApi.validate(id)
      setProblems(result.problems)
    } finally {
      setChecking(false)
    }
  }

  async function handlePublish() {
    if (!id) return
    await handleSave()
    const result = await workflowsApi.validate(id)
    if (!result.valid) {
      setProblems(result.problems)
      setMessage('Corrija os problemas listados antes de publicar.')
      return
    }
    await workflowsApi.publish(id)
    setStatus('PUBLISHED')
    setMessage('Workflow publicado.')
  }

  /** T037 — inicia uma execução da versão ativa mais recente (US2). */
  async function handleStartExecution() {
    if (!id) return
    const versions = await workflowsApi.listVersions(id)
    const active = versions.find((v) => v.active)
    if (!active) {
      setMessage('Publique uma versão antes de iniciar uma execução.')
      return
    }
    const instance = await instancesApi.start(active.id)
    navigate(`/executions/${instance.id}`)
  }

  const selectedNode = nodes.find((n) => n.id === selectedNodeId) ?? null
  const selectedEdge = edges.find((e) => e.id === selectedEdgeId) ?? null

  return (
    <div>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
        <div>
          <input
            aria-label="Nome do workflow"
            value={name}
            onChange={(e) => setName(e.target.value)}
            style={{ fontSize: '1.1rem', fontWeight: 600, border: 'none' }}
          />
          <span style={{ marginLeft: '0.5rem', fontSize: '0.8rem', color: '#718096' }}>{status}</span>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button type="button" onClick={handleSave}>
            Salvar rascunho
          </button>
          <button type="button" onClick={handlePublish}>
            Publicar
          </button>
          {status === 'PUBLISHED' && (
            <button type="button" onClick={handleStartExecution}>
              Iniciar execução
            </button>
          )}
        </div>
      </header>

      {message && <p role="status">{message}</p>}

      <div style={{ display: 'flex' }}>
        <Palette
          onAddNode={(nodeType) =>
            setNodes([
              ...nodes,
              {
                id: `node-${Date.now()}`,
                type: nodeType,
                position: { x: 80 + nodes.length * 40, y: 80 + nodes.length * 20 },
                data: { label: nodeType, nodeType, configuration: {} },
              },
            ])
          }
        />
        <ReactFlowProvider>
          <DesignerCanvas
            nodes={nodes}
            edges={edges}
            onNodesChange={setNodes}
            onEdgesChange={setEdges}
            onSelectNode={setSelectedNodeId}
            onSelectEdge={setSelectedEdgeId}
          />
        </ReactFlowProvider>
        <PropertiesPanel
          selectedNode={selectedNode}
          selectedEdge={selectedEdge}
          onChangeNode={(nodeId, patch) =>
            setNodes(nodes.map((n) => (n.id === nodeId ? { ...n, data: { ...n.data, ...patch } } : n)))
          }
          onChangeEdgeCondition={(edgeId, condition) =>
            setEdges(edges.map((e) => (e.id === edgeId ? { ...e, data: { ...e.data, condition }, label: condition } : e)))
          }
          onChangeEdgePublishEvent={(edgeId, publishEvent) =>
            setEdges(edges.map((e) => (e.id === edgeId ? { ...e, data: { ...e.data, publishEvent } } : e)))
          }
        />
      </div>

      <ValidationPanel problems={problems} checking={checking} onValidate={handleValidate} />
    </div>
  )
}
