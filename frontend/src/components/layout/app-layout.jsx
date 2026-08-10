import { Outlet } from 'react-router-dom'
import { SidebarNav } from './sidebar-nav'

/**
 * Root application layout.
 * Fixed sidebar on the left, scrollable main content on the right.
 * The sidebar is 256px wide on desktop and hidden/overlaid on mobile.
 */
export function AppLayout() {
  return (
    <div className="flex h-svh w-full overflow-hidden bg-background">
      <SidebarNav />
      <div className="flex flex-1 flex-col overflow-hidden">
        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
