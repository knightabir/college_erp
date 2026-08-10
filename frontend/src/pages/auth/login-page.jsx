import { useState } from 'react'
import { useNavigate, useLocation, Navigate, Link } from 'react-router-dom'
import { GraduationCap, AlertCircle } from 'lucide-react'
import { authApi } from '@/api/auth'
import { useAuthStore, useIsAuthenticated } from '@/stores/auth-store'
import { toAppError } from '@/lib/api-error'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Spinner } from '@/components/ui/spinner'

/**
 * Public login screen — the only unauthenticated route besides itself.
 * On success, stores the session (LoginResponse: user + tokens) and redirects either back to
 * wherever ProtectedRoute intercepted the user from, or to the dashboard.
 */
export default function LoginPage() {
  const isAuthenticated = useIsAuthenticated()
  const setSession = useAuthStore((s) => s.setSession)
  const navigate = useNavigate()
  const location = useLocation()

  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  // Already signed in — don't show the login form again.
  if (isAuthenticated) {
    const redirectTo = location.state?.from ?? '/'
    return <Navigate to={redirectTo} replace />
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const response = await authApi.login({ identifier, password })
      setSession({
        user: {
          userId: response.userId,
          username: response.username,
          fullName: response.fullName,
          email: response.email,
          roles: Array.from(response.roles ?? []),
        },
        accessToken: response.accessToken,
        refreshToken: response.refreshToken,
      })
      const redirectTo = location.state?.from ?? '/'
      navigate(redirectTo, { replace: true })
    } catch (err) {
      setError(toAppError(err).message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-svh w-full items-center justify-center bg-muted/40 p-4">
      <div className="w-full max-w-sm">
        <div className="mb-6 flex flex-col items-center gap-2 text-center">
          <div className="flex h-11 w-11 items-center justify-center rounded-sm bg-primary text-primary-foreground">
            <GraduationCap className="size-6" />
          </div>
          <div>
            <p className="text-lg font-semibold text-foreground">CollegeERP</p>
            <p className="text-xs text-muted-foreground">College Management System</p>
          </div>
        </div>

        <Card className="rounded-sm shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">Sign in</CardTitle>
            <CardDescription>Enter your credentials to access your account.</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="identifier">Username or Email</Label>
                <Input
                  id="identifier"
                  autoComplete="username"
                  placeholder="you@college.edu"
                  value={identifier}
                  onChange={(e) => setIdentifier(e.target.value)}
                  autoFocus
                  required
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="password">Password</Label>
                <Input
                  id="password"
                  type="password"
                  autoComplete="current-password"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>

              {error && (
                <Alert variant="destructive">
                  <AlertCircle />
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}

              <Button type="submit" className="mt-1 w-full" disabled={loading}>
                {loading ? (
                  <>
                    <Spinner className="mr-2 size-3.5" />
                    Signing in…
                  </>
                ) : (
                  'Sign in'
                )}
              </Button>
            </form>
          </CardContent>
        </Card>

        <p className="mt-6 text-center text-xs text-muted-foreground">
          Accounts are provisioned by your institution's administration.
          <br />
          New institution?{' '}
          <Link to="/register" className="font-medium text-primary hover:underline">Register your college</Link>
        </p>
      </div>
    </div>
  )
}
