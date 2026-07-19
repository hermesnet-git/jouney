import { createBrowserRouter, Navigate } from 'react-router-dom'
import { AppShell } from './AppShell'
import { WorkflowListPage } from '../workflow-management/WorkflowListPage'
import { WorkflowDesignerPage } from '../workflow-designer/WorkflowDesignerPage'
import { ExecutionListPage } from '../workflow-runtime/ExecutionListPage'
import { ExecutionDetailPage } from '../workflow-runtime/ExecutionDetailPage'
import { TaskInboxPage } from '../task-inbox/TaskInboxPage'
import { ConnectorListPage } from '../connectors/ConnectorListPage'
import { AuditTrailPage } from '../shared/AuditTrailPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppShell />,
    children: [
      { index: true, element: <Navigate to="/workflows" replace /> },
      { path: 'workflows', element: <WorkflowListPage /> },
      { path: 'workflows/:id', element: <WorkflowDesignerPage /> },
      { path: 'executions', element: <ExecutionListPage /> },
      { path: 'executions/:id', element: <ExecutionDetailPage /> },
      { path: 'tasks', element: <TaskInboxPage /> },
      { path: 'connectors', element: <ConnectorListPage /> },
      { path: 'audit', element: <AuditTrailPage /> },
    ],
  },
])
