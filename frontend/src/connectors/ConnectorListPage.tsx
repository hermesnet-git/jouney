import { useEffect, useState } from 'react'
import { connectorsApi, type ConnectorSummary } from '../api/connectors'

/** T058 — cadastro e teste de conectores (FR-014). */
export function ConnectorListPage() {
  const [connectors, setConnectors] = useState<ConnectorSummary[]>([])
  const [id, setId] = useState('')
  const [baseUrl, setBaseUrl] = useState('')
  const [credentialRef, setCredentialRef] = useState('')
  const [testResult, setTestResult] = useState<Record<string, string>>({})

  function refresh() {
    connectorsApi.list().then(setConnectors)
  }

  useEffect(refresh, [])

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault()
    if (!id || !baseUrl || !credentialRef) return
    await connectorsApi.create(id, 'REST', JSON.stringify({ baseUrl }), credentialRef)
    setId('')
    setBaseUrl('')
    setCredentialRef('')
    refresh()
  }

  async function handleTest(connectorId: string) {
    const result = await connectorsApi.test(connectorId, 'GET /health')
    setTestResult((prev) => ({
      ...prev,
      [connectorId]: result.success ? 'OK' : `Falhou: ${result.errorMessage}`,
    }))
  }

  return (
    <div>
      <h1>Conectores</h1>

      <form onSubmit={handleCreate} style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
        <label>
          Id
          <input value={id} onChange={(e) => setId(e.target.value)} placeholder="customer-api-prod" />
        </label>
        <label>
          URL base
          <input value={baseUrl} onChange={(e) => setBaseUrl(e.target.value)} placeholder="https://api.exemplo.com" />
        </label>
        <label>
          Referência da credencial
          <input value={credentialRef} onChange={(e) => setCredentialRef(e.target.value)} placeholder="vault://customer-api" />
        </label>
        <button type="submit">Cadastrar</button>
      </form>

      <table>
        <thead>
          <tr>
            <th scope="col">Id</th>
            <th scope="col">Tipo</th>
            <th scope="col"></th>
            <th scope="col"></th>
          </tr>
        </thead>
        <tbody>
          {connectors.map((c) => (
            <tr key={c.id}>
              <td>{c.id}</td>
              <td>{c.type}</td>
              <td>
                <button type="button" onClick={() => handleTest(c.id)}>
                  Testar
                </button>
              </td>
              <td>{testResult[c.id]}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
