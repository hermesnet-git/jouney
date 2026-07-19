import type { WorkflowNodeData } from '../nodes/WorkflowNodeShape'
import './palette.css'

const PALETTE_ITEMS: { nodeType: WorkflowNodeData['nodeType']; label: string }[] = [
  { nodeType: 'START', label: 'Início' },
  { nodeType: 'END', label: 'Fim' },
  { nodeType: 'USER_TASK', label: 'Tarefa de Usuário' },
  { nodeType: 'REST_SERVICE_TASK', label: 'Chamada REST' },
  { nodeType: 'EXCLUSIVE_GATEWAY', label: 'Decisão' },
]

/** T026 — paleta de etapas; arrastar para o canvas ou ativar por teclado (Enter) para adicionar. */
export function Palette({ onAddNode }: { onAddNode: (nodeType: WorkflowNodeData['nodeType']) => void }) {
  return (
    <aside className="palette" aria-label="Paleta de etapas">
      <h2>Etapas</h2>
      <ul>
        {PALETTE_ITEMS.map((item) => (
          <li key={item.nodeType}>
            <button
              type="button"
              className="palette__item"
              draggable
              onDragStart={(event) => {
                event.dataTransfer.setData('application/workflow-node-type', item.nodeType)
                event.dataTransfer.effectAllowed = 'move'
              }}
              onClick={() => onAddNode(item.nodeType)}
            >
              {item.label}
            </button>
          </li>
        ))}
      </ul>
    </aside>
  )
}
