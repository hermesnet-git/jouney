import { NavLink, Outlet } from 'react-router-dom'
import './app-shell.css'

const NAV_ITEMS = [
  { to: '/workflows', label: 'Workflows' },
  { to: '/executions', label: 'Execuções' },
  { to: '/tasks', label: 'Minhas tarefas' },
  { to: '/connectors', label: 'Conectores' },
  { to: '/audit', label: 'Auditoria' },
]

export function AppShell() {
  return (
    <div className="app-shell">
      <nav className="app-shell__nav" aria-label="Navegação principal">
        <span className="app-shell__brand">Plataforma de Workflows</span>
        <ul>
          {NAV_ITEMS.map((item) => (
            <li key={item.to}>
              <NavLink to={item.to} className={({ isActive }) => (isActive ? 'active' : '')}>
                {item.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
      <main className="app-shell__content">
        <Outlet />
      </main>
    </div>
  )
}
