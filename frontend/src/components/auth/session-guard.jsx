import { useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { toast } from 'sonner'
import { onAuthFailure } from '@/lib/api-client'

/**
 * Registers the api-client's "the session couldn't be renewed" callback with the router, so a
 * failed silent-refresh (expired refresh token, revoked session, ...) sends the user back to
 * /login with an explanatory toast instead of leaving them on a page that will just keep
 * failing every request. Mount once, near the top of the route tree, inside <BrowserRouter/>.
 */
export function SessionGuard() {
  const navigate = useNavigate()
  const location = useLocation()

  useEffect(() => {
    onAuthFailure(() => {
      if (location.pathname === '/login') return
      toast.error('Session expired', { description: 'Please sign in again to continue.' })
      navigate('/login', { replace: true, state: { from: location.pathname + location.search } })
    })
  }, [navigate, location])

  return null
}
