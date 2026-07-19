import { apiClient } from './client'

export interface AuditEntry {
  actor: string
  action: string
  entityType: string
  entityId: string
  occurredAt: string
  detailJson: string | null
}

export const auditApi = {
  search: () => apiClient.get<AuditEntry[]>('/audit'),
}
