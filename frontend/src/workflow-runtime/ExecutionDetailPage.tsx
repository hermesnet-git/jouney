import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { instancesApi, type InstanceSummary, type NodeInstanceSummary } from '../api/instances'

const STATUS_LABEL: Record<string, string> = {
  PENDING: 'Pendente',
  RUNNING: 'Em execução',
  WAITING: 'Aguardando',
  COMPLETED: 'Concluída',
  FAILED: 'Falhou',
  CANCELLED: 'Cancelada',
  SKIPPED: 'Pulada',
}

/** T040 — status + histórico completo da execução (FR-009, FR-019); T073 traz cancel/retry. */
export function ExecutionDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [instance, setInstance] = useState<InstanceSummary | null>(null)
  const [history, setHistory] = useState<NodeInstanceSummary[]>([])

  function refresh() {
    if (!id) return
    instancesApi.get(id).then(setInstance)
    instancesApi.history(id).then(setHistory)
  }

  useEffect(refresh, [id])

  async function handleCancel() {
    if (!id) return
    await instancesApi.cancel(id)
    refresh()
  }

  async function handleRetry() {
    if (!id) return
    await instancesApi.retry(id)
    refresh()
  }

  if (!instance) return <p>Carregando…</p>

  return (
    <div>
      <h1>Execução {instance.id}</h1>
      <p>
        Status: <strong>{STATUS_LABEL[instance.status] ?? instance.status}</strong>
        {instance.businessKey && <> · Chave: {instance.businessKey}</>}
      </p>

      {instance.status === 'RUNNING' || instance.status === 'WAITING' ? (
        <button type="button" onClick={handleCancel}>
          Cancelar execução
        </button>
      ) : null}
      {instance.status === 'FAILED' ? (
        <button type="button" onClick={handleRetry}>
          Forçar nova tentativa
        </button>
      ) : null}

      <h2>Histórico</h2>
      <table>
        <thead>
          <tr>
            <th scope="col">Etapa</th>
            <th scope="col">Tipo</th>
            <th scope="col">Status</th>
            <th scope="col">Tentativa</th>
            <th scope="col">Início</th>
            <th scope="col">Fim</th>
          </tr>
        </thead>
        <tbody>
          {history.map((h) => (
            <tr key={h.id}>
              <td>{h.nodeId}</td>
              <td>{h.nodeType}</td>
              <td>{STATUS_LABEL[h.status] ?? h.status}</td>
              <td>{h.attempt}</td>
              <td>{new Date(h.startedAt).toLocaleTimeString('pt-BR')}</td>
              <td>{h.completedAt ? new Date(h.completedAt).toLocaleTimeString('pt-BR') : '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
