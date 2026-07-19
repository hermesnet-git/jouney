import { useState } from 'react'

export interface FormField {
  key: string
  label: string
  type: 'text' | 'number' | 'date' | 'boolean' | 'select' | 'textarea'
  required: boolean
  options?: string[]
}

export interface FormSchema {
  title: string
  fields: FormField[]
}

/** T049 — renderiza dinamicamente os tipos de campo do MVP (spec.md §9). */
export function DynamicForm({
  schema,
  onSubmit,
}: {
  schema: FormSchema
  onSubmit: (data: Record<string, unknown>) => void
}) {
  const [values, setValues] = useState<Record<string, unknown>>({})

  function setValue(key: string, value: unknown) {
    setValues((prev) => ({ ...prev, [key]: value }))
  }

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        onSubmit(values)
      }}
    >
      <h3>{schema.title}</h3>
      {schema.fields.map((field) => (
        <div key={field.key} style={{ marginBottom: '0.6rem' }}>
          <label htmlFor={`field-${field.key}`}>
            {field.label}
            {field.required && ' *'}
          </label>
          {renderInput(field, values[field.key], (v) => setValue(field.key, v))}
        </div>
      ))}
      <button type="submit">Concluir tarefa</button>
    </form>
  )
}

function renderInput(field: FormField, value: unknown, onChange: (value: unknown) => void) {
  const id = `field-${field.key}`
  switch (field.type) {
    case 'boolean':
      return (
        <input
          id={id}
          type="checkbox"
          checked={Boolean(value)}
          onChange={(e) => onChange(e.target.checked)}
        />
      )
    case 'textarea':
      return <textarea id={id} value={(value as string) ?? ''} onChange={(e) => onChange(e.target.value)} />
    case 'select':
      return (
        <select id={id} value={(value as string) ?? ''} onChange={(e) => onChange(e.target.value)}>
          <option value="">Selecione…</option>
          {(field.options ?? []).map((opt) => (
            <option key={opt} value={opt}>
              {opt}
            </option>
          ))}
        </select>
      )
    case 'number':
      return (
        <input
          id={id}
          type="number"
          value={(value as string) ?? ''}
          onChange={(e) => onChange(e.target.valueAsNumber)}
        />
      )
    case 'date':
      return <input id={id} type="date" value={(value as string) ?? ''} onChange={(e) => onChange(e.target.value)} />
    default:
      return <input id={id} type="text" value={(value as string) ?? ''} onChange={(e) => onChange(e.target.value)} />
  }
}
