import { apiClient } from './client'

export interface InstanceSummary {
  id: string
  workflowVersionId: string
  businessKey: string | null
  status: 'CREATED' | 'RUNNING' | 'WAITING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  currentNodeId: string | null
  startedAt: string
  completedAt: string | null
}

export interface NodeInstanceSummary {
  id: string
  nodeId: string
  nodeType: string
  status: string
  attempt: number
  startedAt: string
  completedAt: string | null
}

export const instancesApi = {
  list: () => apiClient.get<InstanceSummary[]>('/instances'),
  get: (id: string) => apiClient.get<InstanceSummary>(`/instances/${id}`),
  history: (id: string) => apiClient.get<NodeInstanceSummary[]>(`/instances/${id}/history`),
  start: (workflowVersionId: string, businessKey?: string) =>
    apiClient.post<InstanceSummary>(`/workflow-versions/${workflowVersionId}/instances`, { businessKey }),
  cancel: (id: string) => apiClient.post<void>(`/instances/${id}/cancel`),
  retry: (id: string) => apiClient.post<void>(`/instances/${id}/retry`),
}
