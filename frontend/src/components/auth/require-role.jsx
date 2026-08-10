import { Navigate, Outlet } from 'react-router-dom'
import { useCurrentUser } from '@/stores/auth-store'
import { hasAnyRole } from '@/lib/rbac'

/**
 * Route-level role gate, nested inside <ProtectedRoute/> (so the user is guaranteed to already
 * be authenticated by the time this runs). `roles` empty/undefined allows any signed-in user.
 *
 *   <Route element={<RequireRole roles={NAV_ACCESS.students} />}>
 *     <Route path="/students" element={<StudentsPage />} />
 *   </Route>
 */
export function RequireRole({ roles }) {
  const user = useCurrentUser()

  if (!hasAnyRole(user, roles)) {
    return <Navigate to="/403" replace />
  }

  return <Outlet />
}
