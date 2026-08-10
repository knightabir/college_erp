import { useState } from 'react'
import { Plus, Pin, Bell } from 'lucide-react'
import { toast } from 'sonner'
import { PageHeader } from '@/components/shared/page-header'
import { PageContainer } from '@/components/shared/page-container'
import { FormDialog } from '@/components/shared/form-dialog'
import { EmptyState } from '@/components/shared/empty-state'
import { ErrorState } from '@/components/shared/error-state'
import { TableLoadingState } from '@/components/shared/loading-state'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Switch } from '@/components/ui/switch'
import { Spinner } from '@/components/ui/spinner'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { useCurrentUser } from '@/stores/auth-store'
import { hasAnyRole, RoleGroups } from '@/lib/rbac'
import { getErrorMessage, getFieldErrors } from '@/lib/api-error'
import { useNotices, useCreateNotice } from '@/api/communication'
import { useActiveDepartments } from '@/api/academic'

const TARGET_TYPES = ['ALL', 'DEPARTMENT', 'BATCH', 'ROLE']

const EMPTY_FORM = { title: '', body: '', targetType: 'ALL', targetId: '', pinned: false, expiresAt: '' }

function NoticeForm({ departments, onSubmit, onCancel }) {
  const [form, setForm] = useState(EMPTY_FORM)
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }))
    setErrors((e) => ({ ...e, [field]: undefined }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setErrors({})
    try {
      await onSubmit({
        title: form.title,
        body: form.body,
        targetType: form.targetType,
        targetId: form.targetType === 'DEPARTMENT' ? form.targetId || null : null,
        pinned: form.pinned,
        expiresAt: form.expiresAt ? new Date(form.expiresAt).toISOString() : null,
      })
    } catch (err) {
      const fieldErrors = getFieldErrors(err)
      setErrors({ ...fieldErrors, _global: Object.keys(fieldErrors).length ? undefined : getErrorMessage(err) })
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4 py-2">
      <div className="flex flex-col gap-1">
        <Label htmlFor="n-title">Title</Label>
        <Input id="n-title" value={form.title} onChange={(e) => set('title', e.target.value)} aria-invalid={!!errors.title} />
        {errors.title && <p className="text-xs text-destructive">{errors.title}</p>}
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor="n-body">Body</Label>
        <Textarea id="n-body" rows={4} value={form.body} onChange={(e) => set('body', e.target.value)} aria-invalid={!!errors.body} />
        {errors.body && <p className="text-xs text-destructive">{errors.body}</p>}
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="flex flex-col gap-1">
          <Label>Audience</Label>
          <Select value={form.targetType} onValueChange={(v) => set('targetType', v)}>
            <SelectTrigger><SelectValue /></SelectTrigger>
            <SelectContent>
              {TARGET_TYPES.map((t) => <SelectItem key={t} value={t}>{t[0] + t.slice(1).toLowerCase()}</SelectItem>)}
            </SelectContent>
          </Select>
        </div>
        {form.targetType === 'DEPARTMENT' && (
          <div className="flex flex-col gap-1">
            <Label>Department</Label>
            <Select value={form.targetId} onValueChange={(v) => set('targetId', v)}>
              <SelectTrigger><SelectValue placeholder="Select…" /></SelectTrigger>
              <SelectContent>
                {departments.map((d) => <SelectItem key={d.id} value={d.id}>{d.name}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>
        )}
        <div className="flex flex-col gap-1">
          <Label htmlFor="n-expires">Expires (optional)</Label>
          <Input id="n-expires" type="date" value={form.expiresAt} onChange={(e) => set('expiresAt', e.target.value)} />
        </div>
      </div>
      <div className="flex items-center gap-2">
        <Switch isSelected={form.pinned} onChange={(v) => set('pinned', v)} />
        <span className="text-sm text-foreground">Pin to top</span>
      </div>

      {errors._global && (
        <p className="rounded-sm border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">{errors._global}</p>
      )}

      <div className="flex justify-end gap-2 border-t border-border pt-4">
        <Button type="button" variant="outline" onClick={onCancel} disabled={loading}>Cancel</Button>
        <Button type="submit" disabled={loading}>
          {loading ? (<><Spinner className="mr-2 size-3.5" />Publishing…</>) : 'Publish Notice'}
        </Button>
      </div>
    </form>
  )
}

const TARGET_LABEL = { ALL: 'Everyone', DEPARTMENT: 'Department', BATCH: 'Batch', ROLE: 'Role' }

export default function NoticesPage() {
  const user = useCurrentUser()
  const canCreate = hasAnyRole(user, RoleGroups.ACADEMIC_OFFICE)

  const [departmentId, setDepartmentId] = useState('')
  const [addOpen, setAddOpen] = useState(false)

  const { data: departments } = useActiveDepartments()
  const { data: notices, isLoading, isError, error, refetch } = useNotices({ departmentId: departmentId || undefined })
  const createNotice = useCreateNotice()

  async function handleAdd(body) {
    await createNotice.mutateAsync(body)
    setAddOpen(false)
    toast.success('Notice published.')
  }

  return (
    <div>
      <PageHeader
        title="Notices"
        description="Institutional announcements and notices."
        actions={
          canCreate && (
            <Button size="sm" onClick={() => setAddOpen(true)}>
              <Plus className="mr-1.5 size-3.5" /> New Notice
            </Button>
          )
        }
      />
      <PageContainer>
        <div className="flex items-end gap-3">
          <div className="flex min-w-52 flex-col gap-1">
            <span className="text-xs font-medium text-muted-foreground">Filter by department</span>
            <Select value={departmentId || 'all'} onValueChange={(v) => setDepartmentId(v === 'all' ? '' : v)}>
              <SelectTrigger className="h-8 text-sm"><SelectValue placeholder="All" /></SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All notices</SelectItem>
                {(departments ?? []).map((d) => <SelectItem key={d.id} value={d.id}>{d.name}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>
        </div>

        {isLoading ? (
          <Card className="rounded-sm shadow-none"><CardContent className="p-0"><TableLoadingState rows={5} cols={1} /></CardContent></Card>
        ) : isError ? (
          <Card className="rounded-sm shadow-none"><CardContent><ErrorState description={getErrorMessage(error)} onRetry={refetch} /></CardContent></Card>
        ) : !notices?.length ? (
          <Card className="rounded-sm shadow-none">
            <CardContent>
              <EmptyState
                icon={Bell}
                title="No notices"
                description="Nothing has been published yet."
                action={canCreate && (
                  <Button size="sm" onClick={() => setAddOpen(true)}><Plus className="mr-1.5 size-3.5" /> New Notice</Button>
                )}
              />
            </CardContent>
          </Card>
        ) : (
          <div className="flex flex-col gap-3">
            {notices.map((n) => (
              <Card key={n.id} className="rounded-sm shadow-none">
                <CardContent className="p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-2">
                      {n.pinned && <Pin className="size-3.5 shrink-0 text-primary" />}
                      <h3 className="text-sm font-semibold text-foreground">{n.title}</h3>
                    </div>
                    <div className="flex shrink-0 items-center gap-2">
                      <Badge variant="outline" className="text-[10px]">{TARGET_LABEL[n.targetType]}</Badge>
                      <span className="text-xs text-muted-foreground">
                        {n.publishedAt ? new Date(n.publishedAt).toLocaleDateString() : 'Draft'}
                      </span>
                    </div>
                  </div>
                  <p className="mt-1.5 whitespace-pre-wrap text-sm text-muted-foreground">{n.body}</p>
                  {n.expiresAt && (
                    <p className="mt-2 text-xs text-muted-foreground">Expires {new Date(n.expiresAt).toLocaleDateString()}</p>
                  )}
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </PageContainer>

      <FormDialog open={addOpen} onOpenChange={setAddOpen} title="New Notice" description="Publish an announcement." size="lg">
        <NoticeForm departments={departments ?? []} onSubmit={handleAdd} onCancel={() => setAddOpen(false)} />
      </FormDialog>
    </div>
  )
}
