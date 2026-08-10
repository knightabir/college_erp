import { useState } from 'react'
import { useNavigate, Navigate, Link } from 'react-router-dom'
import { GraduationCap, AlertCircle } from 'lucide-react'
import { tenantApi } from '@/api/tenant'
import { useAuthStore, useIsAuthenticated } from '@/stores/auth-store'
import { getErrorMessage, getFieldErrors } from '@/lib/api-error'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Spinner } from '@/components/ui/spinner'

const EMPTY_FORM = {
  collegeName: '', collegeCode: '', affiliatedUniversity: '', accreditation: '',
  establishedYear: '', address: '', city: '', state: '', pincode: '',
  collegeContactEmail: '', collegeContactPhone: '', website: '',
  adminUsername: '', adminEmail: '', adminFullName: '', adminPassword: '', adminPhone: '',
}

/**
 * "Register your college" — the one public self-service signup flow in this system
 * (POST /api/v1/register). Creates a brand-new Tenant + its first SUPER_ADMIN account together
 * and logs that admin straight in, same as LoginPage.
 */
export default function RegisterPage() {
  const isAuthenticated = useIsAuthenticated()
  const setSession = useAuthStore((s) => s.setSession)
  const navigate = useNavigate()

  const [form, setForm] = useState(EMPTY_FORM)
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }))
    setErrors((e) => ({ ...e, [field]: undefined }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setErrors({})
    try {
      const response = await tenantApi.register({
        ...form,
        establishedYear: form.establishedYear ? Number(form.establishedYear) : null,
      })
      setSession({
        user: {
          userId: response.adminUserId,
          username: form.adminUsername,
          fullName: form.adminFullName,
          email: form.adminEmail,
          roles: ['SUPER_ADMIN'],
        },
        accessToken: response.accessToken,
        refreshToken: response.refreshToken,
      })
      navigate('/', { replace: true })
    } catch (err) {
      const fieldErrors = getFieldErrors(err)
      setErrors({ ...fieldErrors, _global: Object.keys(fieldErrors).length ? undefined : getErrorMessage(err) })
    } finally {
      setLoading(false)
    }
  }

  const field = (id, label, props = {}) => (
    <div className="flex flex-col gap-1">
      <Label htmlFor={id}>{label}</Label>
      <Input id={id} aria-invalid={!!errors[id]} {...props} />
      {errors[id] && <p className="text-xs text-destructive">{errors[id]}</p>}
    </div>
  )

  return (
    <div className="flex min-h-svh w-full items-center justify-center bg-muted/40 p-4 py-10">
      <div className="w-full max-w-2xl">
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
            <CardTitle className="text-lg">Register your college</CardTitle>
            <CardDescription>Create a workspace for your institution and its first administrator account.</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-6">
              <div>
                <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-muted-foreground">College Details</p>
                <div className="grid grid-cols-2 gap-4">
                  {field('collegeName', 'College Name', {
                    value: form.collegeName, onChange: (e) => set('collegeName', e.target.value), className: 'col-span-2',
                  })}
                  {field('collegeCode', 'College Code', {
                    placeholder: 'e.g. sxccollege', value: form.collegeCode,
                    onChange: (e) => set('collegeCode', e.target.value),
                  })}
                  {field('establishedYear', 'Established Year', {
                    type: 'number', value: form.establishedYear, onChange: (e) => set('establishedYear', e.target.value),
                  })}
                  {field('affiliatedUniversity', 'Affiliated University', {
                    value: form.affiliatedUniversity, onChange: (e) => set('affiliatedUniversity', e.target.value),
                  })}
                  {field('accreditation', 'Accreditation', {
                    value: form.accreditation, onChange: (e) => set('accreditation', e.target.value),
                  })}
                  {field('collegeContactEmail', 'College Contact Email', {
                    type: 'email', value: form.collegeContactEmail, onChange: (e) => set('collegeContactEmail', e.target.value),
                  })}
                  {field('collegeContactPhone', 'College Contact Phone', {
                    value: form.collegeContactPhone, onChange: (e) => set('collegeContactPhone', e.target.value),
                  })}
                  {field('website', 'Website', {
                    value: form.website, onChange: (e) => set('website', e.target.value), className: 'col-span-2',
                  })}
                  {field('address', 'Address', {
                    value: form.address, onChange: (e) => set('address', e.target.value), className: 'col-span-2',
                  })}
                  {field('city', 'City', { value: form.city, onChange: (e) => set('city', e.target.value) })}
                  {field('state', 'State', { value: form.state, onChange: (e) => set('state', e.target.value) })}
                  {field('pincode', 'Pincode', { value: form.pincode, onChange: (e) => set('pincode', e.target.value) })}
                </div>
              </div>

              <div className="border-t border-border pt-4">
                <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Administrator Account</p>
                <div className="grid grid-cols-2 gap-4">
                  {field('adminFullName', 'Full Name', {
                    value: form.adminFullName, onChange: (e) => set('adminFullName', e.target.value), className: 'col-span-2',
                  })}
                  {field('adminUsername', 'Username', {
                    value: form.adminUsername, onChange: (e) => set('adminUsername', e.target.value),
                  })}
                  {field('adminPhone', 'Phone', {
                    value: form.adminPhone, onChange: (e) => set('adminPhone', e.target.value),
                  })}
                  {field('adminEmail', 'Email', {
                    type: 'email', value: form.adminEmail, onChange: (e) => set('adminEmail', e.target.value), className: 'col-span-2',
                  })}
                  {field('adminPassword', 'Password', {
                    type: 'password', autoComplete: 'new-password', value: form.adminPassword,
                    onChange: (e) => set('adminPassword', e.target.value), className: 'col-span-2',
                  })}
                </div>
              </div>

              {errors._global && (
                <Alert variant="destructive">
                  <AlertCircle />
                  <AlertDescription>{errors._global}</AlertDescription>
                </Alert>
              )}

              <Button type="submit" className="w-full" disabled={loading}>
                {loading ? (
                  <>
                    <Spinner className="mr-2 size-3.5" />
                    Creating your workspace…
                  </>
                ) : (
                  'Register College'
                )}
              </Button>
            </form>
          </CardContent>
        </Card>

        <p className="mt-6 text-center text-xs text-muted-foreground">
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-primary hover:underline">Sign in</Link>
        </p>
      </div>
    </div>
  )
}
