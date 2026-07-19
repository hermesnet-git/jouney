import { Handle, Position, type NodeProps } from 'reactflow'
import './nodes.css'

export interface WorkflowNodeData {
  label: string
  nodeType: 'START' | 'END' | 'USER_TASK' | 'REST_SERVICE_TASK' | 'EXCLUSIVE_GATEWAY'
  configuration: Record<string, unknown>
}

const NODE_STYLE: Record<WorkflowNodeData['nodeType'], { icon: string; className: string }> = {
  START: { icon: '▶', className: 'wf-node--start' },
  END: { icon: '■', className: 'wf-node--end' },
  USER_TASK: { icon: '🧑', className: 'wf-node--user-task' },
  REST_SERVICE_TASK: { icon: '⇄', className: 'wf-node--rest' },
  EXCLUSIVE_GATEWAY: { icon: '◆', className: 'wf-node--gateway' },
}

/**
 * T027 — nó customizado único, parametrizado por tipo (reduz duplicação e mantém as convenções
 * visuais consistentes entre os 5 tipos — constitution II).
 */
export function WorkflowNodeShape({ data, selected }: NodeProps<WorkflowNodeData>) {
  const style = NODE_STYLE[data.nodeType]
  const showTarget = data.nodeType !== 'START'
  const showSource = data.nodeType !== 'END'

  return (
    <div
      className={`wf-node ${style.className} ${selected ? 'wf-node--selected' : ''}`}
      tabIndex={0}
      role="button"
      aria-label={`Etapa ${data.label} (${data.nodeType})`}
    >
      {showTarget && <Handle type="target" position={Position.Left} />}
      <span className="wf-node__icon" aria-hidden="true">
        {style.icon}
      </span>
      <span className="wf-node__label">{data.label}</span>
      {showSource && <Handle type="source" position={Position.Right} />}
    </div>
  )
}

export const nodeTypes = {
  START: WorkflowNodeShape,
  END: WorkflowNodeShape,
  USER_TASK: WorkflowNodeShape,
  REST_SERVICE_TASK: WorkflowNodeShape,
  EXCLUSIVE_GATEWAY: WorkflowNodeShape,
}
