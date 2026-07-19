import { apiClient } from './client'

export interface ConnectorSummary {
  id: string
  type: string
  baseConfigJson: string
  credentialRef: string
  updatedAt: string
}

export const connectorsApi = {
  list: () => apiClient.get<ConnectorSummary[]>('/connectors'),
  create: (id: string, type: string, baseConfigJson: string, credentialRef: string) =>
    apiClient.post<ConnectorSummary>('/connectors', { id, type, baseConfigJson, credentialRef }),
  test: (id: string, operation: string) =>
    apiClient.post<{ success: boolean; errorMessage: string | null }>(`/connectors/${id}/test`, { operation }),
}
