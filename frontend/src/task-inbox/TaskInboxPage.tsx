import { useEffect, useState } from 'react'
import { tasksApi, type TaskSummary } from '../api/tasks'
import { ApiRequestError } from '../api/client'
import { DynamicForm, type FormSchema } from './DynamicForm'

/** T050 — inbox: listar, assumir e concluir tarefas (FR-010, FR-011). */
export function TaskInboxPage() {
  const [tasks, setTasks] = useState<TaskSummary[]>([])
  const [claimed, setClaimed] = useState<TaskSummary | null>(null)
  const [error, setError] = useState<string | null>(null)

  function refresh() {
    tasksApi.list().then(setTasks)
  }

  useEffect(refresh, [])

  async function handleClaim(task: TaskSummary) {
    setError(null)
    try {
      const updated = await tasksApi.claim(task.id)
      setClaimed(updated)
    } catch (e) {
      if (e instanceof ApiRequestError) {
        setError(e.apiError.message)
      }
      refresh()
    }
  }

  async function handleComplete(formData: Record<string, unknown>) {
    if (!claimed) return
    setError(null)
    try {
      await tasksApi.complete(claimed.id, formData)
      setClaimed(null)
      refresh()
    } catch (e) {
      if (e instanceof ApiRequestError) {
        setError([e.apiError.message, ...e.apiError.details].join(' '))
      }
    }
  }

  if (claimed) {
    const schema = JSON.parse(claimed.formSchemaJson) as FormSchema
    return (
      <div>
        <h1>{claimed.name}</h1>
        {error && <p role="alert">{error}</p>}
        <DynamicForm schema={schema} onSubmit={handleComplete} />
      </div>
    )
  }

  return (
    <div>
      <h1>Minhas tarefas</h1>
      {error && <p role="alert">{error}</p>}
      <ul>
        {tasks.map((task) => (
          <li key={task.id}>
            {task.name}
            <button type="button" onClick={() => handleClaim(task)} style={{ marginLeft: '0.5rem' }}>
              Assumir
            </button>
          </li>
        ))}
      </ul>
      {tasks.length === 0 && <p>Nenhuma tarefa pendente.</p>}
    </div>
  )
}
