import './validation-panel.css'

interface ValidationPanelProps {
  problems: string[] | null
  checking: boolean
  onValidate: () => void
}

/** T030 — consome POST /workflows/{id}/validate e lista todos os problemas (FR-004). */
export function ValidationPanel({ problems, checking, onValidate }: ValidationPanelProps) {
  return (
    <section className="validation-panel" aria-live="polite">
      <button type="button" onClick={onValidate} disabled={checking}>
        {checking ? 'Validando…' : 'Validar workflow'}
      </button>

      {problems !== null && problems.length === 0 && (
        <p className="validation-panel__ok">Nenhum problema encontrado — pronto para publicar.</p>
      )}

      {problems !== null && problems.length > 0 && (
        <ul className="validation-panel__problems">
          {problems.map((problem, index) => (
            <li key={index}>{problem}</li>
          ))}
        </ul>
      )}
    </section>
  )
}
