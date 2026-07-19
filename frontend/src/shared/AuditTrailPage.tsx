import { useEffect, useState } from 'react'
import { auditApi, type AuditEntry } from '../api/audit'

/** T074 — trilha de auditoria consultável (FR-021, US6). */
export function AuditTrailPage() {
  const [entries, setEntries] = useState<AuditEntry[]>([])

  useEffect(() => {
    auditApi.search().then(setEntries)
  }, [])

  return (
    <div>
      <h1>Auditoria</h1>
      <table>
        <thead>
          <tr>
            <th scope="col">Quando</th>
            <th scope="col">Quem</th>
            <th scope="col">O quê</th>
            <th scope="col">Entidade</th>
          </tr>
        </thead>
        <tbody>
          {entries.map((e, i) => (
            <tr key={i}>
              <td>{new Date(e.occurredAt).toLocaleString('pt-BR')}</td>
              <td>{e.actor}</td>
              <td>{e.action}</td>
              <td>
                {e.entityType} {e.entityId}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {entries.length === 0 && <p>Nenhum evento de auditoria ainda.</p>}
    </div>
  )
}
