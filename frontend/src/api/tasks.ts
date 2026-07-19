import { apiClient } from './client'

export interface TaskSummary {
  id: string
  name: string
  status: 'PENDING' | 'CLAIMED' | 'COMPLETED' | 'CANCELLED'
  assignee: string | null
  candidateGroup: string | null
  formSchemaJson: string
  createdAt: string
}

export const tasksApi = {
  list: () => apiClient.get<TaskSummary[]>('/tasks'),
  get: (id: string) => apiClient.get<TaskSummary>(`/tasks/${id}`),
  claim: (id: string) => apiClient.post<TaskSummary>(`/tasks/${id}/claim`),
  complete: (id: string, formData: Record<string, unknown>) =>
    apiClient.post<void>(`/tasks/${id}/complete`, { formData }),
}
