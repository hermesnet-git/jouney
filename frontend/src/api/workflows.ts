import { apiClient } from './client'

export interface WorkflowSummary {
  id: string
  workflowKey: string
  name: string
  description: string | null
  status: 'DRAFT' | 'PUBLISHED'
  graphJson: string
  createdAt: string
  updatedAt: string
}

export interface ValidationResult {
  valid: boolean
  problems: string[]
}

export interface WorkflowVersionSummary {
  id: string
  versionNumber: number
  publishedBy: string
  publishedAt: string
  active: boolean
}

export const workflowsApi = {
  list: () => apiClient.get<WorkflowSummary[]>('/workflows'),
  get: (id: string) => apiClient.get<WorkflowSummary>(`/workflows/${id}`),
  create: (workflowKey: string, name: string, description?: string) =>
    apiClient.post<WorkflowSummary>('/workflows', { workflowKey, name, description }),
  updateDraft: (id: string, name: string, description: string | null, graphJson: string) =>
    apiClient.put<WorkflowSummary>(`/workflows/${id}`, { name, description, graphJson }),
  validate: (id: string) => apiClient.post<ValidationResult>(`/workflows/${id}/validate`),
  publish: (id: string) => apiClient.post<WorkflowVersionSummary>(`/workflows/${id}/publish`),
  listVersions: (id: string) => apiClient.get<WorkflowVersionSummary[]>(`/workflows/${id}/versions`),
}
