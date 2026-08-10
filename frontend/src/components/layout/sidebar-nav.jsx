import { useState } from 'react'
import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useTheme } from 'next-themes'
import { toast } from 'sonner'
import {
  LayoutDashboard,
  Users,
  GraduationCap,
  Building2,
  BookOpen,
  Clock,
  ClipboardList,
  Library,
  DollarSign,
  UserCog,
  Bell,
  Settings,
  Sun,
  Moon,
  Monitor,
  Menu,
  X,
  LogOut,
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { useAuthStore, useCurrentUser } from '@/stores/auth-store'
import { authApi } from '@/api/auth'
import { hasAnyRole, NAV_ACCESS } from '@/lib/rbac'

const NAV_GROUPS = [
  {
    label: 'Overview',
    items: [
      { label: 'Dashboard', href: '/', icon: LayoutDashboard, access: 'dashboard' },
    ],
  },
  {
    label: 'Academic',
    items: [
      { label: 'Students',          href: '/students',    icon: Users,        access: 'students' },
      { label: 'Faculty',           href: '/faculty',     icon: GraduationCap, access: 'faculty' },
      { label: 'Departments',       href: '/departments', icon: Building2,    access: 'departments' },
      { label: 'Courses & Subjects',href: '/courses',     icon: BookOpen,     access: 'courses' },
    ],
  },
  {
    label: 'Operations',
    items: [
      { label: 'Attendance',   href: '/attendance',   icon: Clock,         access: 'attendance' },
      { label: 'Examinations', href: '/examinations', icon: ClipboardList, access: 'examinations' },
      { label: 'Library',      href: '/library',      icon: Library,       access: 'library' },
    ],
  },
  {
    label: 'Finance & HR',
    items: [
      { label: 'Fee Management', href: '/fees', icon: DollarSign, access: 'fees' },
      { label: 'HR & Payroll',   href: '/hr',   icon: UserCog,    access: 'hr' },
    ],
  },
  {
    label: 'Administration',
    items: [
      { label: 'Notices',  href: '/notices',  icon: Bell,     access: 'notices' },
      { label: 'Settings', href: '/settings', icon: Settings, access: 'settings' },
    ],
  },
]

function ThemeToggle() {
  const { theme, setTheme } = useTheme()
  return (
    <DropdownMenuTrigger>
      <Button
        variant="ghost"
        size="icon-sm"
        className="text-sidebar-foreground/70 hover:text-sidebar-foreground hover:bg-sidebar-accent"
        aria-label="Toggle theme"
      >
        {theme === 'dark' ? (
          <Moon className="size-4" />
        ) : theme === 'light' ? (
          <Sun className="size-4" />
        ) : (
          <Monitor className="size-4" />
        )}
      </Button>
      <DropdownMenu className="w-36">
        <DropdownMenuItem onSelect={() => setTheme('light')}>
          <Sun className="mr-2 size-4" /> Light
        </DropdownMenuItem>
        <DropdownMenuItem onSelect={() => setTheme('dark')}>
          <Moon className="mr-2 size-4" /> Dark
        </DropdownMenuItem>
        <DropdownMenuItem onSelect={() => setTheme('system')}>
          <Monitor className="mr-2 size-4" /> System
        </DropdownMenuItem>
      </DropdownMenu>
    </DropdownMenuTrigger>
  )
}

function NavItem({ item }) {
  const location = useLocation()
  const isActive =
    item.href === '/'
      ? location.pathname === '/'
      : location.pathname.startsWith(item.href)

  return (
    <NavLink
      to={item.href}
      className={cn(
        'flex items-center gap-2.5 rounded-sm px-3 py-2 text-sm font-medium transition-colors',
        isActive
          ? 'bg-sidebar-accent text-sidebar-primary'
          : 'text-sidebar-foreground/70 hover:bg-sidebar-accent/60 hover:text-sidebar-foreground'
      )}
    >
      <item.icon
        className={cn('size-4 shrink-0', isActive ? 'text-sidebar-primary' : 'text-sidebar-foreground/50')}
      />
      {item.label}
    </NavLink>
  )
}

function NavGroup({ group, user }) {
  const visibleItems = group.items.filter((item) => hasAnyRole(user, NAV_ACCESS[item.access]))
  if (visibleItems.length === 0) return null

  return (
    <div className="px-3 py-1">
      <p className="mb-1 px-3 text-[11px] font-semibold uppercase tracking-wider text-sidebar-foreground/40">
        {group.label}
      </p>
      <ul className="space-y-0.5" role="list">
        {visibleItems.map((item) => (
          <li key={item.href}>
            <NavItem item={item} />
          </li>
        ))}
      </ul>
    </div>
  )
}

function initialsOf(fullName) {
  if (!fullName) return '?'
  const parts = fullName.trim().split(/\s+/)
  return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '')).toUpperCase() || '?'
}

/** Turns "HEAD_ACCOUNTANT" into "Head Accountant" for a friendlier footer subtitle. */
function formatRole(role) {
  if (!role) return ''
  return role
    .toLowerCase()
    .split('_')
    .map((w) => w[0].toUpperCase() + w.slice(1))
    .join(' ')
}

/**
 * Institutional sidebar navigation.
 * Desktop: fixed 256px panel. Mobile: hidden, shown via overlay.
 */
export function SidebarNav() {
  const [mobileOpen, setMobileOpen] = useState(false)
  const [loggingOut, setLoggingOut] = useState(false)
  const user = useCurrentUser()
  const refreshToken = useAuthStore((s) => s.refreshToken)
  const clearSession = useAuthStore((s) => s.clear)
  const navigate = useNavigate()

  async function handleLogout() {
    setLoggingOut(true)
    try {
      if (refreshToken) await authApi.logout(refreshToken)
    } catch {
      // Best-effort — the token may already be invalid/expired server-side. The user's intent
      // is to leave this session either way, so proceed to clear it locally regardless.
    } finally {
      clearSession()
      setLoggingOut(false)
      toast.success('Signed out')
      navigate('/login', { replace: true })
    }
  }

  const sidebarContent = (
    <aside className="flex h-full w-64 flex-col border-r border-sidebar-border bg-sidebar">
      {/* Brand header */}
      <div className="flex h-14 items-center justify-between border-b border-sidebar-border px-4">
        <div className="flex items-center gap-2.5">
          <div className="flex h-7 w-7 items-center justify-center rounded-sm bg-sidebar-primary text-sidebar-primary-foreground">
            <GraduationCap className="size-4" />
          </div>
          <div>
            <p className="text-sm font-semibold leading-none text-sidebar-foreground">
              CollegeERP
            </p>
            <p className="text-[10px] text-sidebar-foreground/50">
              Management System
            </p>
          </div>
        </div>
        <ThemeToggle />
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto py-3" aria-label="Main navigation">
        <div className="space-y-4">
          {NAV_GROUPS.map((group) => (
            <NavGroup key={group.label} group={group} user={user} />
          ))}
        </div>
      </nav>

      {/* User footer */}
      <div className="border-t border-sidebar-border p-3">
        <div className="flex items-center gap-2.5 rounded-sm px-2 py-2">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-sidebar-primary/10 text-xs font-semibold text-sidebar-primary">
            {initialsOf(user?.fullName)}
          </div>
          <div className="flex-1 overflow-hidden">
            <p className="truncate text-sm font-medium text-sidebar-foreground">
              {user?.fullName ?? 'Unknown user'}
            </p>
            <p className="truncate text-xs text-sidebar-foreground/50">
              {formatRole(user?.roles?.[0]) || user?.email}
            </p>
          </div>
          <Button
            variant="ghost"
            size="icon-sm"
            className="text-sidebar-foreground/40 hover:text-sidebar-foreground hover:bg-sidebar-accent"
            aria-label="Sign out"
            onClick={handleLogout}
            disabled={loggingOut}
          >
            <LogOut className="size-4" />
          </Button>
        </div>
      </div>
    </aside>
  )

  return (
    <>
      {/* Desktop sidebar */}
      <div className="hidden lg:flex lg:shrink-0">
        {sidebarContent}
      </div>

      {/* Mobile hamburger */}
      <div className="fixed top-3 left-3 z-50 lg:hidden">
        <Button
          variant="outline"
          size="icon-sm"
          onClick={() => setMobileOpen(true)}
          aria-label="Open navigation"
        >
          <Menu className="size-4" />
        </Button>
      </div>

      {/* Mobile overlay */}
      {mobileOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div
            className="absolute inset-0 bg-black/40"
            onClick={() => setMobileOpen(false)}
          />
          <div className="absolute inset-y-0 left-0 flex">
            {sidebarContent}
            <Button
              variant="ghost"
              size="icon-sm"
              className="absolute right-2 top-3 text-sidebar-foreground"
              onClick={() => setMobileOpen(false)}
              aria-label="Close navigation"
            >
              <X className="size-4" />
            </Button>
          </div>
        </div>
      )}
    </>
  )
}
