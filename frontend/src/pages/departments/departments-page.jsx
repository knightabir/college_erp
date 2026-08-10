import { useState } from 'react'
import { Plus, Pencil, Ban, MoreHorizontal } from 'lucide-react'
import { toast } from 'sonner'
import { PageHeader } from '@/components/shared/page-header'
import { PageContainer } from '@/components/shared/page-container'
import { DataTable } from '@/components/shared/data-table'
import { FormDialog } from '@/components/shared/form-dialog'
import { ConfirmDialog } from '@/components/shared/confirm-dialog'
import { StatusBadge } from '@/components/shared/status-badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Spinner } from '@/components/ui/spinner'
import {
  DropdownMenu, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { useCurrentUser } from '@/stores/auth-store'
import { hasAnyRole, RoleGroups } from '@/lib/rbac'
import { getErrorMessage, getFieldErrors } from '@/lib/api-error'
import {
  useDepartments, useCreateDepartment, useUpdateDepartment, useDeactivateDepartment,
} from '@/api/academic'

const EMPTY_FORM = { name: '', code: '', description: '', contactEmail: '', contactPhone: '' }

function DepartmentForm({ initial = EMPTY_FORM, onSubmit, onCancel, submitLabel }) {
  const [form, setForm] = useState({ ...EMPTY_FORM, ...initial })
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
      await onSubmit(form)
    } catch (err) {
      const fieldErrors = getFieldErrors(err)
      setErrors({ ...fieldErrors, _global: Object.keys(fieldErrors).length ? undefined : getErrorMessage(err) })
    } finally {
      setLoading(false)
    }
  }

  const field = (id, label, props = {}) => (
    <div className="flex flex-col gap-1">
      <Label htmlFor={id} className="text-sm font-medium">{label}</Label>
      <Input id={id} aria-invalid={!!errors[id]} {...props} />
      {errors[id] && <p className="text-xs text-destructive">{errors[id]}</p>}
    </div>
  )

  return (
    <form onSubmit={handleSubmit} noValidate>
      <div className="grid grid-cols-2 gap-4 py-2">
        {field('name', 'Department Name', {
          placeholder: 'Computer Science',
          value: form.name,
          onChange: (e) => set('name', e.target.value),
          className: 'col-span-2',
        })}
        {field('code', 'Code', {
          placeholder: 'CS',
          value: form.code,
          onChange: (e) => set('code', e.target.value.toUpperCase()),
        })}
        {field('contactPhone', 'Contact Phone', {
          value: form.contactPhone ?? '',
          onChange: (e) => set('contactPhone', e.target.value),
        })}
        {field('contactEmail', 'Contact Email', {
          type: 'email',
          value: form.contactEmail ?? '',
          onChange: (e) => set('contactEmail', e.target.value),
          className: 'col-span-2',
        })}
        {field('description', 'Description', {
          value: form.description ?? '',
          onChange: (e) => set('description', e.target.value),
          className: 'col-span-2',
        })}
      </div>

      {errors._global && (
        <p className="mt-2 rounded-sm border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {errors._global}
        </p>
      )}

      <div className="mt-4 flex justify-end gap-2 border-t border-border pt-4">
        <Button type="button" variant="outline" onClick={onCancel} disabled={loading}>Cancel</Button>
        <Button type="submit" disabled={loading}>
          {loading ? (<><Spinner className="mr-2 size-3.5" />Saving…</>) : submitLabel}
        </Button>
      </div>
    </form>
  )
}

function RowActions({ dept, canWrite, onEdit, onDeactivate }) {
  return (
    <DropdownMenuTrigger>
      <Button variant="ghost" size="icon-xs" className="text-muted-foreground" aria-label="Row actions">
        <MoreHorizontal className="size-4" />
      </Button>
      <DropdownMenu className="w-40">
        <DropdownMenuItem onSelect={() => onEdit(dept)}>
          <Pencil className="mr-2 size-3.5" /> Edit
        </DropdownMenuItem>
        {canWrite && dept.active && (
          <>
            <DropdownMenuSeparator />
            <DropdownMenuItem onSelect={() => onDeactivate(dept)} className="text-destructive focus:text-destructive">
              <Ban className="mr-2 size-3.5" /> Deactivate
            </DropdownMenuItem>
          </>
        )}
      </DropdownMenu>
    </DropdownMenuTrigger>
  )
}

export default function DepartmentsPage() {
  const user = useCurrentUser()
  const canWrite = hasAnyRole(user, RoleGroups.SYSTEM_ADMINS)

  const [addOpen, setAddOpen] = useState(false)
  const [editTarget, setEditTarget] = useState(null)
  const [deactivateTarget, setDeactivateTarget] = useState(null)

  const { data, isLoading, isError, error, refetch } = useDepartments({ size: 100 })
  const createDept = useCreateDepartment()
  const updateDept = useUpdateDepartment()
  const deactivateDept = useDeactivateDepartment()

  const departments = data?.content ?? []

  const columns = [
    { key: 'code', label: 'Code', className: 'font-mono text-xs text-muted-foreground w-20' },
    { key: 'name', label: 'Name', render: (d) => <span className="font-medium text-foreground">{d.name}</span> },
    { key: 'contactEmail', label: 'Contact', className: 'text-muted-foreground hidden md:table-cell' },
    {
      key: 'active', label: 'Status', className: 'w-24',
      render: (d) => <StatusBadge status={d.active ? 'active' : 'inactive'} />,
    },
    {
      key: 'actions', label: '', className: 'w-10 text-right',
      render: (d) => <RowActions dept={d} canWrite={canWrite} onEdit={setEditTarget} onDeactivate={setDeactivateTarget} />,
    },
  ]

  async function handleAdd(form) {
    await createDept.mutateAsync(form)
    setAddOpen(false)
    toast.success('Department created.', { description: `${form.name} has been added.` })
  }

  async function handleEdit(form) {
    // Guard against a stale/late invocation racing the dialog closing — never trust that a
    // closed-over "which row" state is still set by the time this callback actually runs.
    if (!editTarget) return
    await updateDept.mutateAsync({ id: editTarget.id, ...form })
    setEditTarget(null)
    toast.success('Department updated.')
  }

  async function handleDeactivate() {
    if (!deactivateTarget) return
    await deactivateDept.mutateAsync(deactivateTarget.id)
    toast.success('Department deactivated.')
  }

  return (
    <div>
      <PageHeader
        title="Departments"
        description={`${departments.length} departments`}
        breadcrumb="Academic"
        actions={
          canWrite && (
            <Button size="sm" onClick={() => setAddOpen(true)}>
              <Plus className="mr-1.5 size-3.5" /> Add Department
            </Button>
          )
        }
      />
      <PageContainer>
        <DataTable
          columns={columns}
          data={departments}
          searchKeys={['name', 'code', 'contactEmail']}
          searchPlaceholder="Search departments…"
          loading={isLoading}
          error={isError ? getErrorMessage(error) : null}
          onRetry={refetch}
          emptyTitle="No departments yet"
          emptyDescription="Add your institution's first academic department."
          emptyAction={
            canWrite && (
              <Button size="sm" onClick={() => setAddOpen(true)}>
                <Plus className="mr-1.5 size-3.5" /> Add Department
              </Button>
            )
          }
        />
      </PageContainer>

      <FormDialog open={addOpen} onOpenChange={setAddOpen} title="Add Department" description="Create a new academic department.">
        <DepartmentForm onSubmit={handleAdd} onCancel={() => setAddOpen(false)} submitLabel="Add Department" />
      </FormDialog>

      <FormDialog open={!!editTarget} onOpenChange={(v) => !v && setEditTarget(null)} title="Edit Department" description="Update department information.">
        {editTarget && <DepartmentForm initial={editTarget} onSubmit={handleEdit} onCancel={() => setEditTarget(null)} submitLabel="Save Changes" />}
      </FormDialog>

      <ConfirmDialog
        open={!!deactivateTarget}
        onOpenChange={(v) => !v && setDeactivateTarget(null)}
        title="Deactivate Department"
        entityName={deactivateTarget?.name}
        actionLabel="Deactivate"
        onConfirm={handleDeactivate}
      />
    </div>
  )
}
