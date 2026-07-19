import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { workflowsApi, type WorkflowSummary } from '../api/workflows'

/** T031 — listagem, criação de rascunho e acesso ao designer/publicação. */
export function WorkflowListPage() {
  const [workflows, setWorkflows] = useState<WorkflowSummary[]>([])
  const [newKey, setNewKey] = useState('')
  const [newName, setNewName] = useState('')

  function refresh() {
    workflowsApi.list().then(setWorkflows)
  }

  useEffect(refresh, [])

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault()
    if (!newKey || !newName) return
    await workflowsApi.create(newKey, newName)
    setNewKey('')
    setNewName('')
    refresh()
  }

  return (
    <div>
      <h1>Workflows</h1>

      <form onSubmit={handleCreate} style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
        <label>
          Chave
          <input value={newKey} onChange={(e) => setNewKey(e.target.value)} placeholder="customer-onboarding" />
        </label>
        <label>
          Nome
          <input value={newName} onChange={(e) => setNewName(e.target.value)} placeholder="Onboarding de Cliente" />
        </label>
        <button type="submit">Criar rascunho</button>
      </form>

      <table>
        <thead>
          <tr>
            <th scope="col">Nome</th>
            <th scope="col">Chave</th>
            <th scope="col">Status</th>
            <th scope="col"></th>
          </tr>
        </thead>
        <tbody>
          {workflows.map((w) => (
            <tr key={w.id}>
              <td>{w.name}</td>
              <td>{w.workflowKey}</td>
              <td>{w.status}</td>
              <td>
                <Link to={`/workflows/${w.id}`}>Abrir no designer</Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
