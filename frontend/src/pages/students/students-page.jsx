import { useState } from 'react'
import { Plus, Pencil, MoreHorizontal, GraduationCap, Users2 } from 'lucide-react'
import { toast } from 'sonner'
import { PageHeader } from '@/components/shared/page-header'
import { PageContainer } from '@/components/shared/page-container'
import { DataTable } from '@/components/shared/data-table'
import { FormDialog } from '@/components/shared/form-dialog'
import { ConfirmDialog } from '@/components/shared/confirm-dialog'
import { StatusBadge } from '@/components/shared/status-badge'
import { EmptyState } from '@/components/shared/empty-state'
import { Button } from '@/components/ui/button'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import {
  DropdownMenu, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { StudentForm } from './student-form'
import { GuardiansDialog } from './guardians-dialog'
import { useCurrentUser } from '@/stores/auth-store'
import { hasAnyRole, RoleGroups } from '@/lib/rbac'
import { getErrorMessage } from '@/lib/api-error'
import {
  useActiveDepartments, useCoursesByDepartment, useBatchesByCourse, useSectionsByBatch,
} from '@/api/academic'
import { useStudentsByBatch, useCreateStudent, useUpdateStudent, useGraduateStudent } from '@/api/students'

const STUDENT_STATUS_BADGE = {
  ACTIVE: { status: 'active', label: 'Active' },
  ON_LEAVE: { status: 'pending', label: 'On Leave' },
  DETAINED: { status: 'warning', label: 'Detained' },
  DISCONTINUED: { status: 'rejected', label: 'Discontinued' },
  TRANSFERRED: { status: 'processing', label: 'Transferred' },
  GRADUATED: { status: 'completed', label: 'Graduated' },
}

function RowActions({ student, canEdit, canGraduate, onEdit, onGraduate, onGuardians }) {
  return (
    <DropdownMenuTrigger>
      <Button variant="ghost" size="icon-xs" className="text-muted-foreground" aria-label="Row actions">
        <MoreHorizontal className="size-4" />
      </Button>
      <DropdownMenu className="w-44">
        {canEdit && (
          <DropdownMenuItem onSelect={() => onEdit(student)}>
            <Pencil className="mr-2 size-3.5" /> Edit
          </DropdownMenuItem>
        )}
        <DropdownMenuItem onSelect={() => onGuardians(student)}>
          <Users2 className="mr-2 size-3.5" /> Guardians
        </DropdownMenuItem>
        {canGraduate && student.status === 'ACTIVE' && (
          <>
            <DropdownMenuSeparator />
            <DropdownMenuItem onSelect={() => onGraduate(student)}>
              <GraduationCap className="mr-2 size-3.5" /> Mark Graduated
            </DropdownMenuItem>
          </>
        )}
      </DropdownMenu>
    </DropdownMenuTrigger>
  )
}

export default function StudentsPage() {
  const user = useCurrentUser()
  const canCreate = hasAnyRole(user, RoleGroups.ACADEMIC_OFFICE)
  const canEdit = hasAnyRole(user, RoleGroups.ACADEMIC_OFFICE)
  const canGraduate = hasAnyRole(user, RoleGroups.SYSTEM_ADMINS)

  const [departmentId, setDepartmentId] = useState('')
  const [courseId, setCourseId] = useState('')
  const [batchId, setBatchId] = useState('')

  const [addOpen, setAddOpen] = useState(false)
  const [editTarget, setEditTarget] = useState(null)
  const [graduateTarget, setGraduateTarget] = useState(null)
  const [guardiansTarget, setGuardiansTarget] = useState(null)

  const { data: departments, isLoading: deptsLoading } = useActiveDepartments()
  const { data: courses, isLoading: coursesLoading } = useCoursesByDepartment(departmentId)
  const { data: batches, isLoading: batchesLoading } = useBatchesByCourse(courseId)
  const { data: sections } = useSectionsByBatch(batchId)
  const {
    data: studentsPage, isLoading: studentsLoading, isError, error, refetch,
  } = useStudentsByBatch(batchId, { size: 200 })

  const createStudent = useCreateStudent()
  const updateStudent = useUpdateStudent()
  const graduateStudent = useGraduateStudent()

  const selectedBatch = batches?.find((b) => b.id === batchId)
  const students = studentsPage?.content ?? []

  function handleDepartmentChange(v) {
    setDepartmentId(v)
    setCourseId('')
    setBatchId('')
  }
  function handleCourseChange(v) {
    setCourseId(v)
    setBatchId('')
  }

  const columns = [
    { key: 'enrollmentNumber', label: 'Enrollment No.', className: 'font-mono text-xs text-muted-foreground w-32' },
    {
      key: 'name', label: 'Name',
      render: (s) => <span className="font-medium text-foreground">{s.fullName}</span>,
    },
    { key: 'personalEmail', label: 'Email', className: 'text-muted-foreground hidden md:table-cell' },
    { key: 'phone', label: 'Phone', className: 'text-muted-foreground w-32' },
    {
      key: 'status', label: 'Status', className: 'w-28',
      render: (s) => {
        const badge = STUDENT_STATUS_BADGE[s.status] ?? { status: 'inactive', label: s.status }
        return <StatusBadge status={badge.status} label={badge.label} />
      },
    },
    {
      key: 'actions', label: '', className: 'w-10 text-right',
      render: (s) => (
        <RowActions
          student={s}
          canEdit={canEdit}
          canGraduate={canGraduate}
          onEdit={setEditTarget}
          onGraduate={setGraduateTarget}
          onGuardians={setGuardiansTarget}
        />
      ),
    },
  ]

  async function handleAdd(form) {
    await createStudent.mutateAsync({ ...form, batchId })
    setAddOpen(false)
    toast.success('Student enrolled successfully.', {
      description: `${form.firstName} ${form.lastName} has been added to ${selectedBatch?.label}.`,
    })
  }

  async function handleEdit(form) {
    // Guard against a stale/late invocation racing the dialog closing — never trust that a
    // closed-over "which row" state is still set by the time this callback actually runs.
    if (!editTarget) return
    await updateStudent.mutateAsync({ id: editTarget.id, ...form })
    setEditTarget(null)
    toast.success('Student updated successfully.')
  }

  async function handleGraduate() {
    // Errors intentionally propagate to <ConfirmDialog/>, which renders them inline and keeps
    // the dialog open — no separate toast here (the mutation is registered with
    // skipGlobalErrorToast for the same reason).
    if (!graduateTarget) return
    await graduateStudent.mutateAsync(graduateTarget.id)
    toast.success('Student marked as graduated.')
  }

  return (
    <div>
      <PageHeader
        title="Students"
        description={selectedBatch ? `${students.length} students in ${selectedBatch.label}` : 'Select a batch to view its students'}
        breadcrumb="Academic"
        actions={
          canCreate && (
            <Button size="sm" onClick={() => setAddOpen(true)} disabled={!batchId}>
              <Plus className="mr-1.5 size-3.5" /> Add Student
            </Button>
          )
        }
      />

      <PageContainer>
        {/* Cascading Department → Course → Batch selector */}
        <div className="flex flex-wrap items-end gap-3 rounded-sm border border-border bg-card p-4">
          <div className="flex min-w-45 flex-col gap-1">
            <span className="text-xs font-medium text-muted-foreground">Department</span>
            <Select value={departmentId} onValueChange={handleDepartmentChange}>
              <SelectTrigger className="h-8 text-sm"><SelectValue placeholder={deptsLoading ? 'Loading…' : 'Select department'} /></SelectTrigger>
              <SelectContent>
                {(departments ?? []).map((d) => <SelectItem key={d.id} value={d.id}>{d.name}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>
          <div className="flex min-w-45 flex-col gap-1">
            <span className="text-xs font-medium text-muted-foreground">Course</span>
            <Select value={courseId} onValueChange={handleCourseChange} isDisabled={!departmentId}>
              <SelectTrigger className="h-8 text-sm"><SelectValue placeholder={coursesLoading ? 'Loading…' : 'Select course'} /></SelectTrigger>
              <SelectContent>
                {(courses ?? []).map((c) => <SelectItem key={c.id} value={c.id}>{c.name}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>
          <div className="flex min-w-45 flex-col gap-1">
            <span className="text-xs font-medium text-muted-foreground">Batch</span>
            <Select value={batchId} onValueChange={setBatchId} isDisabled={!courseId}>
              <SelectTrigger className="h-8 text-sm"><SelectValue placeholder={batchesLoading ? 'Loading…' : 'Select batch'} /></SelectTrigger>
              <SelectContent>
                {(batches ?? []).map((b) => <SelectItem key={b.id} value={b.id}>{b.label}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>
        </div>

        {!batchId ? (
          <EmptyState
            title="No batch selected"
            description="Choose a department, course, and batch above to view its enrolled students."
          />
        ) : (
          <DataTable
            columns={columns}
            data={students}
            searchKeys={['fullName', 'personalEmail', 'enrollmentNumber']}
            searchPlaceholder="Search students…"
            loading={studentsLoading}
            error={isError ? getErrorMessage(error) : null}
            onRetry={refetch}
            emptyTitle="No students enrolled"
            emptyDescription="This batch has no enrolled students yet."
            emptyAction={
              canCreate && (
                <Button size="sm" onClick={() => setAddOpen(true)}>
                  <Plus className="mr-1.5 size-3.5" /> Add Student
                </Button>
              )
            }
          />
        )}
      </PageContainer>

      <FormDialog open={addOpen} onOpenChange={setAddOpen} title="Add Student" description={`Enrol a new student in ${selectedBatch?.label ?? ''}.`}>
        <StudentForm
          mode="create"
          batchLabel={selectedBatch?.label}
          sections={sections ?? []}
          onSubmit={handleAdd}
          onCancel={() => setAddOpen(false)}
          submitLabel="Add Student"
        />
      </FormDialog>

      <FormDialog open={!!editTarget} onOpenChange={(v) => !v && setEditTarget(null)} title="Edit Student" description="Update student information.">
        {editTarget && (
          <StudentForm mode="edit" initial={editTarget} onSubmit={handleEdit} onCancel={() => setEditTarget(null)} submitLabel="Save Changes" />
        )}
      </FormDialog>

      <ConfirmDialog
        open={!!graduateTarget}
        onOpenChange={(v) => !v && setGraduateTarget(null)}
        title="Mark as Graduated"
        description={graduateTarget && `Mark ${graduateTarget.fullName} as graduated? This updates their enrolment status permanently.`}
        actionLabel="Graduate"
        onConfirm={handleGraduate}
      />

      <GuardiansDialog student={guardiansTarget} open={!!guardiansTarget} onOpenChange={(v) => !v && setGuardiansTarget(null)} canAdd={canEdit} />
    </div>
  )
}
