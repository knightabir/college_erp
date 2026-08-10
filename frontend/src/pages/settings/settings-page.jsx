import { useState } from 'react'
import { useTheme } from 'next-themes'
import { toast } from 'sonner'
import { KeyRound, Sun, Moon, Monitor, UserCircle } from 'lucide-react'
import { PageHeader } from '@/components/shared/page-header'
import { PageContainer } from '@/components/shared/page-container'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Spinner } from '@/components/ui/spinner'
import { useCurrentUser } from '@/stores/auth-store'
import { authApi } from '@/api/auth'
import { getErrorMessage, getFieldErrors } from '@/lib/api-error'

function formatRole(role) {
  return role.toLowerCase().split('_').map((w) => w[0].toUpperCase() + w.slice(1)).join(' ')
}

function ProfileCard({ user }) {
  return (
    <Card className="rounded-sm shadow-none">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-sm font-semibold">
          <UserCircle className="size-4" /> Profile
        </CardTitle>
        <CardDescription className="text-xs">Your account details.</CardDescription>
      </CardHeader>
      <CardContent className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div>
          <p className="text-xs font-medium text-muted-foreground">Full Name</p>
          <p className="mt-0.5 text-sm text-foreground">{user?.fullName}</p>
        </div>
        <div>
          <p className="text-xs font-medium text-muted-foreground">Username</p>
          <p className="mt-0.5 text-sm text-foreground">{user?.username}</p>
        </div>
        <div>
          <p className="text-xs font-medium text-muted-foreground">Email</p>
          <p className="mt-0.5 text-sm text-foreground">{user?.email}</p>
        </div>
        <div>
          <p className="text-xs font-medium text-muted-foreground">Roles</p>
          <div className="mt-1 flex flex-wrap gap-1.5">
            {(user?.roles ?? []).map((r) => (
              <Badge key={r} variant="secondary" className="text-[10px]">{formatRole(r)}</Badge>
            ))}
          </div>
        </div>
      </CardContent>
    </Card>
  )
}

function ChangePasswordCard() {
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' })
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }))
    setErrors((e) => ({ ...e, [field]: undefined }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setErrors({})

    if (form.newPassword !== form.confirmPassword) {
      setErrors({ confirmPassword: 'Passwords do not match.' })
      return
    }

    setLoading(true)
    try {
      await authApi.changePassword({ currentPassword: form.currentPassword, newPassword: form.newPassword })
      toast.success('Password changed successfully.')
      setForm({ currentPassword: '', newPassword: '', confirmPassword: '' })
    } catch (err) {
      const fieldErrors = getFieldErrors(err)
      setErrors({ ...fieldErrors, _global: Object.keys(fieldErrors).length ? undefined : getErrorMessage(err) })
    } finally {
      setLoading(false)
    }
  }

  return (
    <Card className="rounded-sm shadow-none">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-sm font-semibold">
          <KeyRound className="size-4" /> Change Password
        </CardTitle>
        <CardDescription className="text-xs">Update the password used to sign in.</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} noValidate className="flex max-w-sm flex-col gap-4">
          <div className="flex flex-col gap-1">
            <Label htmlFor="current-password">Current Password</Label>
            <Input
              id="current-password" type="password" autoComplete="current-password"
              value={form.currentPassword} onChange={(e) => set('currentPassword', e.target.value)}
              aria-invalid={!!errors.currentPassword} required
            />
            {errors.currentPassword && <p className="text-xs text-destructive">{errors.currentPassword}</p>}
          </div>
          <div className="flex flex-col gap-1">
            <Label htmlFor="new-password">New Password</Label>
            <Input
              id="new-password" type="password" autoComplete="new-password"
              value={form.newPassword} onChange={(e) => set('newPassword', e.target.value)}
              aria-invalid={!!errors.newPassword} required minLength={8}
            />
            {errors.newPassword && <p className="text-xs text-destructive">{errors.newPassword}</p>}
          </div>
          <div className="flex flex-col gap-1">
            <Label htmlFor="confirm-password">Confirm New Password</Label>
            <Input
              id="confirm-password" type="password" autoComplete="new-password"
              value={form.confirmPassword} onChange={(e) => set('confirmPassword', e.target.value)}
              aria-invalid={!!errors.confirmPassword} required
            />
            {errors.confirmPassword && <p className="text-xs text-destructive">{errors.confirmPassword}</p>}
          </div>

          {errors._global && (
            <p className="rounded-sm border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">{errors._global}</p>
          )}

          <Button type="submit" className="w-fit" disabled={loading}>
            {loading ? (<><Spinner className="mr-2 size-3.5" />Updating…</>) : 'Update Password'}
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}

function AppearanceCard() {
  const { theme, setTheme } = useTheme()
  const options = [
    { value: 'light', label: 'Light', icon: Sun },
    { value: 'dark', label: 'Dark', icon: Moon },
    { value: 'system', label: 'System', icon: Monitor },
  ]

  return (
    <Card className="rounded-sm shadow-none">
      <CardHeader>
        <CardTitle className="text-sm font-semibold">Appearance</CardTitle>
        <CardDescription className="text-xs">Choose how CollegeERP looks on this device.</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="flex gap-2">
          {options.map((o) => (
            <Button
              key={o.value}
              variant={theme === o.value ? 'default' : 'outline'}
              size="sm"
              onClick={() => setTheme(o.value)}
            >
              <o.icon className="mr-1.5 size-3.5" /> {o.label}
            </Button>
          ))}
        </div>
      </CardContent>
    </Card>
  )
}

export default function SettingsPage() {
  const user = useCurrentUser()

  return (
    <div>
      <PageHeader title="Settings" description="Manage your account and preferences." />
      <PageContainer className="max-w-2xl">
        <ProfileCard user={user} />
        <ChangePasswordCard />
        <AppearanceCard />
      </PageContainer>
    </div>
  )
}
