import { useState } from 'react'
import { Plus, Layers, MoreHorizontal } from 'lucide-react'
import { toast } from 'sonner'
import { PageHeader } from '@/components/shared/page-header'
import { PageContainer } from '@/components/shared/page-container'
import { DataTable } from '@/components/shared/data-table'
import { FormDialog } from '@/components/shared/form-dialog'
import { StatusBadge } from '@/components/shared/status-badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Spinner } from '@/components/ui/spinner'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import {
  DropdownMenu, DropdownMenuItem, DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { BatchesDialog } from './batches-dialog'
import { useCurrentUser } from '@/stores/auth-store'
import { hasAnyRole, RoleGroups } from '@/lib/rbac'
import { getErrorMessage, getFieldErrors } from '@/lib/api-error'
import { useActiveDepartments, useCourses, useCreateCourse } from '@/api/academic'

const LEVELS = ['UG', 'PG', 'DIPLOMA', 'PHD', 'CERTIFICATE']

const EMPTY_FORM = {
  departmentId: '', name: '', code: '', level: '', durationYears: 4, totalCredits: 160, intakeCapacity: 60, description: '',
}

function CourseForm({ departments, onSubmit, onCancel }) {
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
        ...form,
        durationYears: Number(form.durationYears),
        totalCredits: Number(form.totalCredits),
        intakeCapacity: Number(form.intakeCapacity),
      })
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
        <div className="col-span-2 flex flex-col gap-1">
          <Label className="text-sm font-medium">Department</Label>
          <Select value={form.departmentId} onValueChange={(v) => set('departmentId', v)}>
            <SelectTrigger aria-invalid={!!errors.departmentId}><SelectValue placeholder="Select department…" /></SelectTrigger>
            <SelectContent>
              {departments.map((d) => <SelectItem key={d.id} value={d.id}>{d.name}</SelectItem>)}
            </SelectContent>
          </Select>
          {errors.departmentId && <p className="text-xs text-destructive">{errors.departmentId}</p>}
        </div>
        {field('name', 'Course Name', {
          placeholder: 'B.Tech Computer Science',
          value: form.name,
          onChange: (e) => set('name', e.target.value),
          className: 'col-span-2',
        })}
        {field('code', 'Code', {
          placeholder: 'BTCS',
          value: form.code,
          onChange: (e) => set('code', e.target.value.toUpperCase()),
        })}
        <div className="flex flex-col gap-1">
          <Label className="text-sm font-medium">Level</Label>
          <Select value={form.level} onValueChange={(v) => set('level', v)}>
            <SelectTrigger aria-invalid={!!errors.level}><SelectValue placeholder="Select…" /></SelectTrigger>
            <SelectContent>
              {LEVELS.map((l) => <SelectItem key={l} value={l}>{l}</SelectItem>)}
            </SelectContent>
          </Select>
          {errors.level && <p className="text-xs text-destructive">{errors.level}</p>}
        </div>
        {field('durationYears', 'Duration (years)', {
          type: 'number', min: 1,
          value: form.durationYears,
          onChange: (e) => set('durationYears', e.target.value),
        })}
        {field('totalCredits', 'Total Credits', {
          type: 'number', min: 0,
          value: form.totalCredits,
          onChange: (e) => set('totalCredits', e.target.value),
        })}
        {field('intakeCapacity', 'Intake Capacity', {
          type: 'number', min: 1,
          value: form.intakeCapacity,
          onChange: (e) => set('intakeCapacity', e.target.value),
        })}
        {field('description', 'Description', {
          value: form.description,
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
          {loading ? (<><Spinner className="mr-2 size-3.5" />Saving…</>) : 'Add Course'}
        </Button>
      </div>
    </form>
  )
}

export default function CoursesSubjectsPage() {
  const user = useCurrentUser()
  const canWrite = hasAnyRole(user, RoleGroups.SYSTEM_ADMINS)

  const [deptFilter, setDeptFilter] = useState('all')
  const [addOpen, setAddOpen] = useState(false)
  const [batchesTarget, setBatchesTarget] = useState(null)

  const { data: departments } = useActiveDepartments()
  const { data, isLoading, isError, error, refetch } = useCourses({ size: 100 })
  const createCourse = useCreateCourse()

  const courses = data?.content ?? []
  const filtered = deptFilter === 'all' ? courses : courses.filter((c) => c.departmentId === deptFilter)

  const columns = [
    { key: 'code', label: 'Code', className: 'font-mono text-xs text-muted-foreground w-20' },
    { key: 'name', label: 'Name', render: (c) => <span className="font-medium text-foreground">{c.name}</span> },
    { key: 'departmentName', label: 'Department', className: 'text-muted-foreground' },
    { key: 'level', label: 'Level', className: 'w-24' },
    { key: 'durationYears', label: 'Duration', className: 'w-24 text-muted-foreground', render: (c) => `${c.durationYears} yr` },
    {
      key: 'active', label: 'Status', className: 'w-24',
      render: (c) => <StatusBadge status={c.active ? 'active' : 'inactive'} />,
    },
    {
      key: 'actions', label: '', className: 'w-10 text-right',
      render: (c) => (
        <DropdownMenuTrigger>
          <Button variant="ghost" size="icon-xs" className="text-muted-foreground" aria-label="Row actions">
            <MoreHorizontal className="size-4" />
          </Button>
          <DropdownMenu className="w-40">
            <DropdownMenuItem onSelect={() => setBatchesTarget(c)}>
              <Layers className="mr-2 size-3.5" /> View Batches
            </DropdownMenuItem>
          </DropdownMenu>
        </DropdownMenuTrigger>
      ),
    },
  ]

  async function handleAdd(form) {
    await createCourse.mutateAsync(form)
    setAddOpen(false)
    toast.success('Course created.', { description: `${form.name} has been added.` })
  }

  return (
    <div>
      <PageHeader
        title="Courses & Subjects"
        description={`${filtered.length} courses`}
        breadcrumb="Academic"
        actions={
          canWrite && (
            <Button size="sm" onClick={() => setAddOpen(true)}>
              <Plus className="mr-1.5 size-3.5" /> Add Course
            </Button>
          )
        }
      />
      <PageContainer>
        <DataTable
          columns={columns}
          data={filtered}
          searchKeys={['name', 'code', 'departmentName']}
          searchPlaceholder="Search courses…"
          loading={isLoading}
          error={isError ? getErrorMessage(error) : null}
          onRetry={refetch}
          filters={
            <Select value={deptFilter} onValueChange={setDeptFilter}>
              <SelectTrigger className="h-8 w-52 text-sm"><SelectValue placeholder="All departments" /></SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Departments</SelectItem>
                {(departments ?? []).map((d) => <SelectItem key={d.id} value={d.id}>{d.name}</SelectItem>)}
              </SelectContent>
            </Select>
          }
          emptyTitle="No courses yet"
          emptyDescription="Add your institution's first course."
          emptyAction={
            canWrite && (
              <Button size="sm" onClick={() => setAddOpen(true)}>
                <Plus className="mr-1.5 size-3.5" /> Add Course
              </Button>
            )
          }
        />
      </PageContainer>

      <FormDialog open={addOpen} onOpenChange={setAddOpen} title="Add Course" description="Create a new course offered by a department." size="lg">
        <CourseForm departments={departments ?? []} onSubmit={handleAdd} onCancel={() => setAddOpen(false)} />
      </FormDialog>

      <BatchesDialog course={batchesTarget} open={!!batchesTarget} onOpenChange={(v) => !v && setBatchesTarget(null)} canWrite={canWrite} />
    </div>
  )
}
