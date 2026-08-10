import { useState } from 'react'
import { toast } from 'sonner'
import { Plus, Users, CalendarDays, Check, X, Ban } from 'lucide-react'
import { PageHeader } from '@/components/shared/page-header'
import { PageContainer } from '@/components/shared/page-container'
import { FormDialog } from '@/components/shared/form-dialog'
import { EmptyState } from '@/components/shared/empty-state'
import { TableLoadingState } from '@/components/shared/loading-state'
import { StatusBadge } from '@/components/shared/status-badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Spinner } from '@/components/ui/spinner'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { useCurrentUser } from '@/stores/auth-store'
import { hasAnyRole, RoleGroups, UserRole } from '@/lib/rbac'
import { getErrorMessage, getFieldErrors } from '@/lib/api-error'
import { useActiveDepartments } from '@/api/academic'
import {
  useDesignations, useCreateDesignation, useStaffByDepartment, useCreateStaff,
  useLeaveTypes, useCreateLeaveType, useMyLeaveBalances, useMyLeaveRequests,
  usePendingLeaveRequests, useApplyLeave, useReviewLeaveRequest, useCancelLeaveRequest,
} from '@/api/hr'

const STAFF_TYPES = ['TEACHING', 'NON_TEACHING']
const LEAVE_STATUS_BADGE = {
  PENDING: { status: 'pending', label: 'Pending' },
  APPROVED: { status: 'active', label: 'Approved' },
  REJECTED: { status: 'rejected', label: 'Rejected' },
  CANCELLED: { status: 'inactive', label: 'Cancelled' },
}

function AddStaffForm({ departmentId, designations, onSubmit, onCancel }) {
  const [form, setForm] = useState({
    userId: '', employeeNumber: '', designationId: '', staffType: '', joiningDate: '',
    qualification: '', specialization: '', experienceYears: '',
  })
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
        ...form, departmentId,
        designationId: form.designationId || null,
        experienceYears: form.experienceYears ? Number(form.experienceYears) : null,
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
      <Label htmlFor={id}>{label}</Label>
      <Input id={id} aria-invalid={!!errors[id]} {...props} />
      {errors[id] && <p className="text-xs text-destructive">{errors[id]}</p>}
    </div>
  )

  return (
    <form onSubmit={handleSubmit} noValidate className="grid grid-cols-2 gap-4 py-2">
      {field('userId', 'User ID', { value: form.userId, onChange: (e) => set('userId', e.target.value), className: 'col-span-2', placeholder: 'Existing user account UUID' })}
      {field('employeeNumber', 'Employee Number', { value: form.employeeNumber, onChange: (e) => set('employeeNumber', e.target.value) })}
      <div className="flex flex-col gap-1">
        <Label>Staff Type</Label>
        <Select value={form.staffType} onValueChange={(v) => set('staffType', v)}>
          <SelectTrigger aria-invalid={!!errors.staffType}><SelectValue placeholder="Select…" /></SelectTrigger>
          <SelectContent>{STAFF_TYPES.map((t) => <SelectItem key={t} value={t}>{t.replace('_', '-')}</SelectItem>)}</SelectContent>
        </Select>
      </div>
      <div className="flex flex-col gap-1">
        <Label>Designation</Label>
        <Select value={form.designationId} onValueChange={(v) => set('designationId', v)}>
          <SelectTrigger><SelectValue placeholder="Select…" /></SelectTrigger>
          <SelectContent>{designations.map((d) => <SelectItem key={d.id} value={d.id}>{d.name}</SelectItem>)}</SelectContent>
        </Select>
      </div>
      {field('joiningDate', 'Joining Date', { type: 'date', value: form.joiningDate, onChange: (e) => set('joiningDate', e.target.value) })}
      {field('qualification', 'Qualification', { value: form.qualification, onChange: (e) => set('qualification', e.target.value) })}
      {field('specialization', 'Specialization', { value: form.specialization, onChange: (e) => set('specialization', e.target.value) })}
      {field('experienceYears', 'Experience (years)', { type: 'number', min: 0, value: form.experienceYears, onChange: (e) => set('experienceYears', e.target.value) })}
      {errors._global && <p className="col-span-2 rounded-sm border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">{errors._global}</p>}
      <div className="col-span-2 flex justify-end gap-2 border-t border-border pt-4">
        <Button type="button" variant="outline" onClick={onCancel} disabled={loading}>Cancel</Button>
        <Button type="submit" disabled={loading}>{loading ? (<><Spinner className="mr-2 size-3.5" />Saving…</>) : 'Add Staff'}</Button>
      </div>
    </form>
  )
}

function DesignationsCard({ canWrite }) {
  const [showAdd, setShowAdd] = useState(false)
  const [form, setForm] = useState({ name: '', grade: '' })
  const { data: designations } = useDesignations()
  const createDesignation = useCreateDesignation()

  async function handleAdd(e) {
    e.preventDefault()
    try {
      await createDesignation.mutateAsync(form)
      toast.success('Designation added.')
      setShowAdd(false)
      setForm({ name: '', grade: '' })
    } catch (err) {
      toast.error('Could not add designation', { description: getErrorMessage(err) })
    }
  }

  return (
    <Card className="rounded-sm shadow-none">
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="text-sm font-semibold">Designations</CardTitle>
          {canWrite && <Button size="xs" variant="outline" onClick={() => setShowAdd((v) => !v)}><Plus className="mr-1 size-3" /> Add</Button>}
        </div>
      </CardHeader>
      <CardContent>
        <div className="flex flex-wrap gap-1.5">
          {(designations ?? []).length ? designations.map((d) => (
            <span key={d.id} className="rounded-sm border border-border bg-muted px-2 py-0.5 text-xs text-muted-foreground">
              {d.name}{d.grade ? ` · ${d.grade}` : ''}
            </span>
          )) : <p className="text-xs text-muted-foreground">No designations defined yet.</p>}
        </div>
        {showAdd && (
          <form onSubmit={handleAdd} className="mt-3 flex items-end gap-2 border-t border-border pt-3">
            <Input className="h-8 w-40 text-sm" placeholder="Name" value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} required />
            <Input className="h-8 w-28 text-sm" placeholder="Grade (optional)" value={form.grade} onChange={(e) => setForm((f) => ({ ...f, grade: e.target.value }))} />
            <Button size="sm" type="submit" disabled={createDesignation.isPending}>Save</Button>
          </form>
        )}
      </CardContent>
    </Card>
  )
}

function StaffDirectoryTab({ canWrite }) {
  const [departmentId, setDepartmentId] = useState('')
  const [addOpen, setAddOpen] = useState(false)
  const { data: departments } = useActiveDepartments()
  const { data: staff, isLoading } = useStaffByDepartment(departmentId)
  const { data: designations } = useDesignations()
  const createStaff = useCreateStaff()

  async function handleAdd(body) {
    await createStaff.mutateAsync(body)
    setAddOpen(false)
    toast.success('Staff profile created.')
  }

  return (
    <div className="flex flex-col gap-4">
      <DesignationsCard canWrite={canWrite} />

      <div className="flex items-end justify-between gap-3">
        <div className="flex min-w-52 flex-col gap-1">
          <span className="text-xs font-medium text-muted-foreground">Department</span>
          <Select value={departmentId} onValueChange={setDepartmentId}>
            <SelectTrigger className="h-8 text-sm"><SelectValue placeholder="Select department" /></SelectTrigger>
            <SelectContent>{(departments ?? []).map((d) => <SelectItem key={d.id} value={d.id}>{d.name}</SelectItem>)}</SelectContent>
          </Select>
        </div>
        {canWrite && <Button size="sm" onClick={() => setAddOpen(true)} disabled={!departmentId}><Plus className="mr-1.5 size-3.5" /> Add Staff</Button>}
      </div>

      {!departmentId ? (
        <EmptyState icon={Users} title="Select a department" description="Choose a department to view its staff." />
      ) : isLoading ? (
        <TableLoadingState rows={4} cols={4} />
      ) : !staff?.length ? (
        <EmptyState icon={Users} title="No staff yet" description="No staff profiles found for this department." />
      ) : (
        <div className="overflow-x-auto rounded-sm border border-border">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-muted/40 text-left text-xs uppercase text-muted-foreground">
                <th className="px-3 py-2">Employee #</th>
                <th className="px-3 py-2">Designation</th>
                <th className="px-3 py-2">Type</th>
                <th className="px-3 py-2">Joined</th>
                <th className="px-3 py-2">Qualification</th>
              </tr>
            </thead>
            <tbody>
              {staff.map((s) => (
                <tr key={s.id} className="border-b border-border last:border-0">
                  <td className="px-3 py-2 font-mono text-xs">{s.employeeNumber}</td>
                  <td className="px-3 py-2">{s.designationName ?? '—'}</td>
                  <td className="px-3 py-2 text-muted-foreground">{s.staffType?.replace('_', '-')}</td>
                  <td className="px-3 py-2 text-muted-foreground">{s.joiningDate}</td>
                  <td className="px-3 py-2 text-muted-foreground">{s.qualification ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <FormDialog open={addOpen} onOpenChange={setAddOpen} title="Add Staff" description="Create a staff profile for an existing user account." size="lg">
        <AddStaffForm departmentId={departmentId} designations={designations ?? []} onSubmit={handleAdd} onCancel={() => setAddOpen(false)} />
      </FormDialog>
    </div>
  )
}

function ApplyLeaveForm({ leaveTypes, onSubmit, onCancel }) {
  const [form, setForm] = useState({ leaveTypeId: '', fromDate: '', toDate: '', reason: '' })
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)

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

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4 py-2">
      <div className="flex flex-col gap-1">
        <Label>Leave Type</Label>
        <Select value={form.leaveTypeId} onValueChange={(v) => setForm((f) => ({ ...f, leaveTypeId: v }))}>
          <SelectTrigger aria-invalid={!!errors.leaveTypeId}><SelectValue placeholder="Select…" /></SelectTrigger>
          <SelectContent>{leaveTypes.map((t) => <SelectItem key={t.id} value={t.id}>{t.name} ({t.daysPerYear} days/yr)</SelectItem>)}</SelectContent>
        </Select>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="flex flex-col gap-1"><Label htmlFor="from-date">From</Label><Input id="from-date" type="date" value={form.fromDate} onChange={(e) => setForm((f) => ({ ...f, fromDate: e.target.value }))} /></div>
        <div className="flex flex-col gap-1"><Label htmlFor="to-date">To</Label><Input id="to-date" type="date" value={form.toDate} onChange={(e) => setForm((f) => ({ ...f, toDate: e.target.value }))} /></div>
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor="reason">Reason</Label>
        <Textarea id="reason" rows={3} value={form.reason} onChange={(e) => setForm((f) => ({ ...f, reason: e.target.value }))} />
      </div>
      {errors._global && <p className="rounded-sm border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">{errors._global}</p>}
      <div className="flex justify-end gap-2 border-t border-border pt-4">
        <Button type="button" variant="outline" onClick={onCancel} disabled={loading}>Cancel</Button>
        <Button type="submit" disabled={loading}>{loading ? (<><Spinner className="mr-2 size-3.5" />Submitting…</>) : 'Apply for Leave'}</Button>
      </div>
    </form>
  )
}

function LeaveTypesCard({ canManage }) {
  const [showAdd, setShowAdd] = useState(false)
  const [form, setForm] = useState({ code: '', name: '', daysPerYear: 12, paid: true })
  const { data: leaveTypes } = useLeaveTypes()
  const createLeaveType = useCreateLeaveType()

  async function handleAdd(e) {
    e.preventDefault()
    try {
      await createLeaveType.mutateAsync({ ...form, daysPerYear: Number(form.daysPerYear) })
      toast.success('Leave type added.')
      setShowAdd(false)
      setForm({ code: '', name: '', daysPerYear: 12, paid: true })
    } catch (err) {
      toast.error('Could not add leave type', { description: getErrorMessage(err) })
    }
  }

  if (!canManage && !leaveTypes?.length) return null

  return (
    <Card className="rounded-sm shadow-none">
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="text-sm font-semibold">Leave Types</CardTitle>
          {canManage && <Button size="xs" variant="outline" onClick={() => setShowAdd((v) => !v)}><Plus className="mr-1 size-3" /> Add</Button>}
        </div>
      </CardHeader>
      <CardContent>
        <div className="flex flex-wrap gap-1.5">
          {(leaveTypes ?? []).map((t) => (
            <span key={t.id} className="rounded-sm border border-border bg-muted px-2 py-0.5 text-xs text-muted-foreground">
              {t.name} · {t.daysPerYear}d/yr{t.paid ? '' : ' · unpaid'}
            </span>
          ))}
        </div>
        {showAdd && (
          <form onSubmit={handleAdd} className="mt-3 flex flex-wrap items-end gap-2 border-t border-border pt-3">
            <Input className="h-8 w-24 text-sm" placeholder="Code" value={form.code} onChange={(e) => setForm((f) => ({ ...f, code: e.target.value.toUpperCase() }))} required />
            <Input className="h-8 w-32 text-sm" placeholder="Name" value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} required />
            <Input className="h-8 w-24 text-sm" type="number" min={0} placeholder="Days/yr" value={form.daysPerYear} onChange={(e) => setForm((f) => ({ ...f, daysPerYear: e.target.value }))} />
            <Button size="sm" type="submit" disabled={createLeaveType.isPending}>Save</Button>
          </form>
        )}
      </CardContent>
    </Card>
  )
}

function LeaveManagementTab({ canManage }) {
  const year = new Date().getFullYear()
  const [applyOpen, setApplyOpen] = useState(false)

  const { data: leaveTypes } = useLeaveTypes()
  const { data: balances, isLoading: balancesLoading } = useMyLeaveBalances(year)
  const { data: myRequests, isLoading: myRequestsLoading } = useMyLeaveRequests()
  const { data: pending, isLoading: pendingLoading } = usePendingLeaveRequests()

  const applyLeave = useApplyLeave()
  const reviewLeave = useReviewLeaveRequest()
  const cancelLeave = useCancelLeaveRequest()

  async function handleApply(form) {
    await applyLeave.mutateAsync(form)
    setApplyOpen(false)
    toast.success('Leave request submitted.')
  }

  async function handleReview(requestId, decision) {
    try {
      await reviewLeave.mutateAsync({ requestId, decision, remarks: '' })
      toast.success(decision === 'APPROVED' ? 'Leave approved.' : 'Leave rejected.')
    } catch (err) {
      toast.error('Could not update request', { description: getErrorMessage(err) })
    }
  }

  async function handleCancel(requestId) {
    try {
      await cancelLeave.mutateAsync(requestId)
      toast.success('Leave request cancelled.')
    } catch (err) {
      toast.error('Could not cancel request', { description: getErrorMessage(err) })
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <LeaveTypesCard canManage={canManage} />

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card className="rounded-sm shadow-none">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="flex items-center gap-2 text-sm font-semibold"><CalendarDays className="size-4" /> My Leave Balances ({year})</CardTitle>
              </div>
              <Button size="sm" onClick={() => setApplyOpen(true)}><Plus className="mr-1.5 size-3.5" /> Apply</Button>
            </div>
          </CardHeader>
          <CardContent>
            {balancesLoading ? <Spinner className="size-4" /> : !balances?.length ? (
              <p className="text-sm text-muted-foreground">No leave balance initialized for this year yet.</p>
            ) : (
              <div className="flex flex-wrap gap-2">
                {balances.map((b) => (
                  <div key={b.id} className="rounded-sm border border-border px-3 py-2 text-center">
                    <p className="text-lg font-semibold text-foreground">{b.remainingDays}</p>
                    <p className="text-xs text-muted-foreground">{b.leaveTypeName}</p>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        <Card className="rounded-sm shadow-none">
          <CardHeader><CardTitle className="text-sm font-semibold">My Leave Requests</CardTitle></CardHeader>
          <CardContent className="p-0">
            {myRequestsLoading ? <TableLoadingState rows={3} cols={1} /> : !myRequests?.length ? (
              <EmptyState title="No requests" description="You haven't applied for leave yet." className="py-8" />
            ) : (
              <div className="divide-y divide-border">
                {myRequests.map((r) => {
                  const badge = LEAVE_STATUS_BADGE[r.status] ?? { status: 'inactive', label: r.status }
                  return (
                    <div key={r.id} className="flex items-center justify-between px-5 py-2.5">
                      <div>
                        <p className="text-sm font-medium text-foreground">{r.leaveTypeName} · {r.daysRequested}d</p>
                        <p className="text-xs text-muted-foreground">{r.fromDate} → {r.toDate}</p>
                      </div>
                      <div className="flex items-center gap-2">
                        <StatusBadge status={badge.status} label={badge.label} />
                        {r.status === 'PENDING' && (
                          <Button size="icon-xs" variant="ghost" onClick={() => handleCancel(r.id)} aria-label="Cancel"><Ban className="size-3.5" /></Button>
                        )}
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {canManage && (
        <Card className="rounded-sm shadow-none">
          <CardHeader><CardTitle className="text-sm font-semibold">Pending Approvals</CardTitle></CardHeader>
          <CardContent className="p-0">
            {pendingLoading ? <TableLoadingState rows={3} cols={2} /> : !pending?.length ? (
              <EmptyState title="Nothing pending" description="No leave requests awaiting review." className="py-8" />
            ) : (
              <div className="divide-y divide-border">
                {pending.map((r) => (
                  <div key={r.id} className="flex items-center justify-between px-5 py-2.5">
                    <div>
                      <p className="text-sm font-medium text-foreground">{r.leaveTypeName} · {r.daysRequested}d</p>
                      <p className="text-xs text-muted-foreground">{r.fromDate} → {r.toDate} · {r.reason}</p>
                    </div>
                    <div className="flex items-center gap-2">
                      <Button size="xs" variant="outline" onClick={() => handleReview(r.id, 'APPROVED')}><Check className="mr-1 size-3" /> Approve</Button>
                      <Button size="xs" variant="outline" className="text-destructive" onClick={() => handleReview(r.id, 'REJECTED')}><X className="mr-1 size-3" /> Reject</Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      )}

      <FormDialog open={applyOpen} onOpenChange={setApplyOpen} title="Apply for Leave" description="Submit a new leave request.">
        <ApplyLeaveForm leaveTypes={leaveTypes ?? []} onSubmit={handleApply} onCancel={() => setApplyOpen(false)} />
      </FormDialog>
    </div>
  )
}

export default function HRPayrollPage() {
  const user = useCurrentUser()
  const canWriteStaff = hasAnyRole(user, [UserRole.SUPER_ADMIN, UserRole.HR_MANAGER])
  const canManageLeave = hasAnyRole(user, RoleGroups.HR_OFFICE.concat([UserRole.HOD, UserRole.ADMIN]))

  return (
    <div>
      <PageHeader title="HR & Leave" description="Manage staff records and leave requests." breadcrumb="Finance & HR" />
      <PageContainer>
        <Tabs defaultSelectedKey="staff">
          <TabsList>
            <TabsTrigger id="staff">Staff Directory</TabsTrigger>
            <TabsTrigger id="leave">Leave Management</TabsTrigger>
          </TabsList>
          <TabsContent id="staff" className="mt-4"><StaffDirectoryTab canWrite={canWriteStaff} /></TabsContent>
          <TabsContent id="leave" className="mt-4"><LeaveManagementTab canManage={canManageLeave} /></TabsContent>
        </Tabs>
      </PageContainer>
    </div>
  )
}
