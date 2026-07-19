import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { instancesApi, type InstanceSummary } from '../api/instances'

/** T040 — lista de execuções com status atual (FR-019). */
export function ExecutionListPage() {
  const [instances, setInstances] = useState<InstanceSummary[]>([])

  useEffect(() => {
    instancesApi.list().then(setInstances)
  }, [])

  return (
    <div>
      <h1>Execuções</h1>
      <table>
        <thead>
          <tr>
            <th scope="col">Iniciada em</th>
            <th scope="col">Chave de negócio</th>
            <th scope="col">Status</th>
            <th scope="col">Etapa atual</th>
            <th scope="col"></th>
          </tr>
        </thead>
        <tbody>
          {instances.map((i) => (
            <tr key={i.id}>
              <td>{new Date(i.startedAt).toLocaleString('pt-BR')}</td>
              <td>{i.businessKey ?? '—'}</td>
              <td>{i.status}</td>
              <td>{i.currentNodeId ?? '—'}</td>
              <td>
                <Link to={`/executions/${i.id}`}>Ver detalhes</Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
