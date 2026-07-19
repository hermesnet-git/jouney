import { useCallback, useRef } from 'react'
import ReactFlow, {
  Background,
  Controls,
  addEdge,
  applyEdgeChanges,
  applyNodeChanges,
  type Connection,
  type Edge,
  type EdgeChange,
  type Node,
  type NodeChange,
  type ReactFlowInstance,
} from 'reactflow'
import { nodeTypes, type WorkflowNodeData } from '../nodes/WorkflowNodeShape'

interface DesignerCanvasProps {
  nodes: Node<WorkflowNodeData>[]
  edges: Edge[]
  onNodesChange: (nodes: Node<WorkflowNodeData>[]) => void
  onEdgesChange: (edges: Edge[]) => void
  onSelectNode: (nodeId: string | null) => void
  onSelectEdge: (edgeId: string | null) => void
}

let nodeIdCounter = 1
export function nextNodeId() {
  return `node-${Date.now()}-${nodeIdCounter++}`
}

/** T026 — canvas do designer: inserir, conectar, mover e excluir nós (FR-001). */
export function DesignerCanvas({
  nodes,
  edges,
  onNodesChange,
  onEdgesChange,
  onSelectNode,
  onSelectEdge,
}: DesignerCanvasProps) {
  const wrapperRef = useRef<HTMLDivElement>(null)
  const instanceRef = useRef<ReactFlowInstance | null>(null)

  const handleNodesChange = useCallback(
    (changes: NodeChange[]) => onNodesChange(applyNodeChanges(changes, nodes) as Node<WorkflowNodeData>[]),
    [nodes, onNodesChange],
  )

  const handleEdgesChange = useCallback(
    (changes: EdgeChange[]) => onEdgesChange(applyEdgeChanges(changes, edges)),
    [edges, onEdgesChange],
  )

  const handleConnect = useCallback(
    (connection: Connection) => onEdgesChange(addEdge(connection, edges)),
    [edges, onEdgesChange],
  )

  const handleDrop = useCallback(
    (event: React.DragEvent) => {
      event.preventDefault()
      const nodeType = event.dataTransfer.getData('application/workflow-node-type') as WorkflowNodeData['nodeType']
      if (!nodeType || !instanceRef.current || !wrapperRef.current) return

      const bounds = wrapperRef.current.getBoundingClientRect()
      const position = instanceRef.current.project({
        x: event.clientX - bounds.left,
        y: event.clientY - bounds.top,
      })
      const id = nextNodeId()
      const newNode: Node<WorkflowNodeData> = {
        id,
        type: nodeType,
        position,
        data: { label: labelFor(nodeType), nodeType, configuration: {} },
      }
      onNodesChange([...nodes, newNode])
    },
    [nodes, onNodesChange],
  )

  return (
    <div
      ref={wrapperRef}
      style={{ flex: 1, height: '70vh' }}
      onDrop={handleDrop}
      onDragOver={(e) => e.preventDefault()}
    >
      <ReactFlow
        nodes={nodes}
        edges={edges}
        nodeTypes={nodeTypes}
        onNodesChange={handleNodesChange}
        onEdgesChange={handleEdgesChange}
        onConnect={handleConnect}
        onInit={(instance) => (instanceRef.current = instance)}
        onNodeClick={(_, node) => {
          onSelectNode(node.id)
          onSelectEdge(null)
        }}
        onEdgeClick={(_, edge) => {
          onSelectEdge(edge.id)
          onSelectNode(null)
        }}
        onPaneClick={() => {
          onSelectNode(null)
          onSelectEdge(null)
        }}
        fitView
      >
        <Background />
        <Controls />
      </ReactFlow>
    </div>
  )
}

function labelFor(nodeType: WorkflowNodeData['nodeType']): string {
  switch (nodeType) {
    case 'START':
      return 'Início'
    case 'END':
      return 'Fim'
    case 'USER_TASK':
      return 'Tarefa de Usuário'
    case 'REST_SERVICE_TASK':
      return 'Chamada REST'
    case 'EXCLUSIVE_GATEWAY':
      return 'Decisão'
  }
}
