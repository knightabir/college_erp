import { useState } from 'react'
import { toast } from 'sonner'
import { Plus, IdCard, Send, Award, MoreHorizontal } from 'lucide-react'
import { PageHeader } from '@/components/shared/page-header'
import { PageContainer } from '@/components/shared/page-container'
import { AcademicScopeSelector } from '@/components/shared/academic-scope-selector'
import { EmptyState } from '@/components/shared/empty-state'
import { TableLoadingState } from '@/components/shared/loading-state'
import { FormDialog } from '@/components/shared/form-dialog'
import { StatusBadge } from '@/components/shared/status-badge'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Spinner } from '@/components/ui/spinner'
import { Badge } from '@/components/ui/badge'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import {
  DropdownMenu, DropdownMenuItem, DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { useCurrentUser } from '@/stores/auth-store'
import { hasAnyRole, RoleGroups, UserRole } from '@/lib/rbac'
import { getErrorMessage, getFieldErrors } from '@/lib/api-error'
import { useAcademicScope, useSubjectOfferings } from '@/api/academic'
import { useStudentsByBatch, useOwnStudentProfile } from '@/api/students'
import {
  useExamsByBatch, useScheduleExam, useGenerateHallTickets, usePublishResults,
  useEnterMarks, useSubmitMarks, useStudentResult,
} from '@/api/examination'

const EXAM_TYPES = ['INTERNAL_1', 'INTERNAL_2', 'MID_TERM', 'END_TERM', 'PRACTICAL', 'VIVA', 'PROJECT']
const EXAM_STATUS_BADGE = {
  SCHEDULED: { status: 'pending', label: 'Scheduled' },
  ONGOING: { status: 'processing', label: 'Ongoing' },
  COMPLETED: { status: 'completed', label: 'Completed' },
  CANCELLED: { status: 'rejected', label: 'Cancelled' },
}

function ScheduleExamForm({ batchId, termId, onSubmit, onCancel }) {
  const [form, setForm] = useState({ name: '', examType: '', startDate: '', endDate: '' })
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
      await onSubmit({ ...form, batchId, termId })
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
        <Label htmlFor="exam-name">Exam Name</Label>
        <Input id="exam-name" placeholder="Mid-Term Examination" value={form.name} onChange={(e) => set('name', e.target.value)} aria-invalid={!!errors.name} />
        {errors.name && <p className="text-xs text-destructive">{errors.name}</p>}
      </div>
      <div className="flex flex-col gap-1">
        <Label>Exam Type</Label>
        <Select value={form.examType} onValueChange={(v) => set('examType', v)}>
          <SelectTrigger aria-invalid={!!errors.examType}><SelectValue placeholder="Select…" /></SelectTrigger>
          <SelectContent>
            {EXAM_TYPES.map((t) => <SelectItem key={t} value={t}>{t.replace('_', ' ')}</SelectItem>)}
          </SelectContent>
        </Select>
        {errors.examType && <p className="text-xs text-destructive">{errors.examType}</p>}
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="flex flex-col gap-1">
          <Label htmlFor="exam-start">Start Date</Label>
          <Input id="exam-start" type="date" value={form.startDate} onChange={(e) => set('startDate', e.target.value)} aria-invalid={!!errors.startDate} />
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor="exam-end">End Date</Label>
          <Input id="exam-end" type="date" value={form.endDate} onChange={(e) => set('endDate', e.target.value)} aria-invalid={!!errors.endDate} />
        </div>
      </div>
      {errors._global && <p className="rounded-sm border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">{errors._global}</p>}
      <div className="flex justify-end gap-2 border-t border-border pt-4">
        <Button type="button" variant="outline" onClick={onCancel} disabled={loading}>Cancel</Button>
        <Button type="submit" disabled={loading}>
          {loading ? (<><Spinner className="mr-2 size-3.5" />Scheduling…</>) : 'Schedule Exam'}
        </Button>
      </div>
    </form>
  )
}

function MarksEntryForm({ exam, batchId, termId, onClose }) {
  const { data: offerings } = useSubjectOfferings(batchId, termId)
  const { data: rosterPage } = useStudentsByBatch(batchId, { size: 200 })
  const [offeringId, setOfferingId] = useState('')
  const [studentId, setStudentId] = useState('')
  const [internalMarks, setInternalMarks] = useState('')
  const [externalMarks, setExternalMarks] = useState('')
  const [errors, setErrors] = useState({})

  const enterMarks = useEnterMarks()
  const submitMarks = useSubmitMarks()

  async function handleSubmit(e) {
    e.preventDefault()
    setErrors({})
    try {
      const entry = await enterMarks.mutateAsync({
        examId: exam.id, offeringId, studentId,
        internalMarks: internalMarks || null, externalMarks: externalMarks || null,
      })
      await submitMarks.mutateAsync(entry.id)
      toast.success('Marks entered and submitted.')
      setStudentId(''); setInternalMarks(''); setExternalMarks('')
    } catch (err) {
      const fieldErrors = getFieldErrors(err)
      setErrors({ ...fieldErrors, _global: Object.keys(fieldErrors).length ? undefined : getErrorMessage(err) })
    }
  }

  const busy = enterMarks.isPending || submitMarks.isPending

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4 py-2">
      <div className="flex flex-col gap-1">
        <Label>Subject</Label>
        <Select value={offeringId} onValueChange={setOfferingId}>
          <SelectTrigger aria-invalid={!!errors.offeringId}><SelectValue placeholder="Select subject…" /></SelectTrigger>
          <SelectContent>
            {(offerings ?? []).map((o) => <SelectItem key={o.id} value={o.id}>{o.subjectName}</SelectItem>)}
          </SelectContent>
        </Select>
      </div>
      <div className="flex flex-col gap-1">
        <Label>Student</Label>
        <Select value={studentId} onValueChange={setStudentId}>
          <SelectTrigger aria-invalid={!!errors.studentId}><SelectValue placeholder="Select student…" /></SelectTrigger>
          <SelectContent>
            {(rosterPage?.content ?? []).map((s) => <SelectItem key={s.id} value={s.id}>{s.fullName} ({s.enrollmentNumber})</SelectItem>)}
          </SelectContent>
        </Select>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="flex flex-col gap-1">
          <Label htmlFor="internal-marks">Internal Marks</Label>
          <Input id="internal-marks" type="number" step="0.5" value={internalMarks} onChange={(e) => setInternalMarks(e.target.value)} aria-invalid={!!errors.internalMarks} />
          {errors.internalMarks && <p className="text-xs text-destructive">{errors.internalMarks}</p>}
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor="external-marks">External Marks</Label>
          <Input id="external-marks" type="number" step="0.5" value={externalMarks} onChange={(e) => setExternalMarks(e.target.value)} aria-invalid={!!errors.externalMarks} />
          {errors.externalMarks && <p className="text-xs text-destructive">{errors.externalMarks}</p>}
        </div>
      </div>
      {errors._global && <p className="rounded-sm border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">{errors._global}</p>}
      <div className="flex justify-end gap-2 border-t border-border pt-4">
        <Button type="button" variant="outline" onClick={onClose} disabled={busy}>Close</Button>
        <Button type="submit" disabled={busy || !offeringId || !studentId}>
          {busy ? (<><Spinner className="mr-2 size-3.5" />Saving…</>) : 'Enter & Submit'}
        </Button>
      </div>
    </form>
  )
}

function ExamsPanel({ scope, canWrite }) {
  const { data: exams, isLoading } = useExamsByBatch(scope.batchId)
  const [scheduleOpen, setScheduleOpen] = useState(false)
  const [marksTarget, setMarksTarget] = useState(null)
  const scheduleExam = useScheduleExam()
  const generateHallTickets = useGenerateHallTickets()
  const publishResults = usePublishResults()

  const filtered = (exams ?? []).filter((e) => !scope.termId || e.termId === scope.termId)

  async function handleSchedule(body) {
    await scheduleExam.mutateAsync(body)
    setScheduleOpen(false)
    toast.success('Exam scheduled.')
  }

  async function handleGenerateHallTickets(exam) {
    try {
      const result = await generateHallTickets.mutateAsync(exam.id)
      toast.success('Hall tickets generated.', {
        description: `${result?.generated ?? 0} issued${result?.ineligible ? `, ${result.ineligible} ineligible` : ''}.`,
      })
    } catch (err) {
      toast.error('Could not generate hall tickets', { description: getErrorMessage(err) })
    }
  }

  async function handlePublish(exam) {
    try {
      const count = await publishResults.mutateAsync(exam.id)
      toast.success('Results published.', { description: `${count} result${count === 1 ? '' : 's'} processed.` })
    } catch (err) {
      toast.error('Could not publish results', { description: getErrorMessage(err) })
    }
  }

  return (
    <Card className="rounded-sm shadow-none">
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="text-sm font-semibold">Exams</CardTitle>
            <CardDescription className="text-xs">Scheduled examinations for this batch{scope.termId ? ' and term' : ''}.</CardDescription>
          </div>
          {canWrite && (
            <Button size="sm" onClick={() => setScheduleOpen(true)} disabled={!scope.termId}>
              <Plus className="mr-1.5 size-3.5" /> Schedule Exam
            </Button>
          )}
        </div>
      </CardHeader>
      <CardContent className="p-0">
        {isLoading ? (
          <TableLoadingState rows={3} cols={4} />
        ) : !filtered.length ? (
          <EmptyState title="No exams scheduled" description="Schedule an exam for this batch and term." className="py-10" />
        ) : (
          <div className="divide-y divide-border">
            {filtered.map((exam) => {
              const badge = EXAM_STATUS_BADGE[exam.status] ?? { status: 'inactive', label: exam.status }
              return (
                <div key={exam.id} className="flex items-center justify-between px-5 py-3">
                  <div>
                    <p className="text-sm font-medium text-foreground">{exam.name}</p>
                    <p className="text-xs text-muted-foreground">
                      {exam.examType.replace('_', ' ')} · {exam.startDate} → {exam.endDate}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <StatusBadge status={badge.status} label={badge.label} />
                    {canWrite && (
                      <DropdownMenuTrigger>
                        <Button variant="ghost" size="icon-xs" className="text-muted-foreground" aria-label="Exam actions">
                          <MoreHorizontal className="size-4" />
                        </Button>
                        <DropdownMenu className="w-48">
                          <DropdownMenuItem onSelect={() => setMarksTarget(exam)}>
                            <Award className="mr-2 size-3.5" /> Enter Marks
                          </DropdownMenuItem>
                          <DropdownMenuItem onSelect={() => handleGenerateHallTickets(exam)}>
                            <IdCard className="mr-2 size-3.5" /> Generate Hall Tickets
                          </DropdownMenuItem>
                          <DropdownMenuItem onSelect={() => handlePublish(exam)}>
                            <Send className="mr-2 size-3.5" /> Publish Results
                          </DropdownMenuItem>
                        </DropdownMenu>
                      </DropdownMenuTrigger>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </CardContent>

      <FormDialog open={scheduleOpen} onOpenChange={setScheduleOpen} title="Schedule Exam" description="Create a new exam for the selected batch and term.">
        <ScheduleExamForm batchId={scope.batchId} termId={scope.termId} onSubmit={handleSchedule} onCancel={() => setScheduleOpen(false)} />
      </FormDialog>

      <FormDialog open={!!marksTarget} onOpenChange={(v) => !v && setMarksTarget(null)} title="Enter Marks" description={marksTarget ? `Record a student's marks for ${marksTarget.name}.` : ''} size="lg">
        {marksTarget && <MarksEntryForm exam={marksTarget} batchId={scope.batchId} termId={scope.termId} onClose={() => setMarksTarget(null)} />}
      </FormDialog>
    </Card>
  )
}

function MyResultsCard({ termId }) {
  const { data: student } = useOwnStudentProfile()
  const { data: result, isLoading, isError, error } = useStudentResult(student?.id, termId)

  if (!termId) return null

  return (
    <Card className="rounded-sm shadow-none">
      <CardHeader>
        <CardTitle className="text-sm font-semibold">My Result</CardTitle>
        <CardDescription className="text-xs">Your grade card for the selected term.</CardDescription>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <Spinner className="size-4" />
        ) : isError ? (
          <p className="text-sm text-muted-foreground">{getErrorMessage(error)}</p>
        ) : result ? (
          <div className="flex flex-col gap-4">
            <div className="flex flex-wrap items-center gap-6">
              <div>
                <p className="text-2xl font-semibold text-foreground">{result.sgpa ?? '—'}</p>
                <p className="text-xs text-muted-foreground">SGPA</p>
              </div>
              <div>
                <p className="text-2xl font-semibold text-foreground">{result.cgpa ?? '—'}</p>
                <p className="text-xs text-muted-foreground">CGPA</p>
              </div>
              <Badge variant={result.resultStatus === 'PASS' ? 'default' : 'destructive'}>{result.resultStatus}</Badge>
            </div>
            <div className="overflow-x-auto rounded-sm border border-border">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border bg-muted/40 text-left text-xs uppercase text-muted-foreground">
                    <th className="px-3 py-2">Subject</th>
                    <th className="px-3 py-2">Internal</th>
                    <th className="px-3 py-2">External</th>
                    <th className="px-3 py-2">Total</th>
                    <th className="px-3 py-2">Grade</th>
                  </tr>
                </thead>
                <tbody>
                  {(result.subjects ?? []).map((s) => (
                    <tr key={s.subjectCode} className="border-b border-border last:border-0">
                      <td className="px-3 py-2">{s.subjectName}</td>
                      <td className="px-3 py-2 text-muted-foreground">{s.internalMarks ?? '—'}</td>
                      <td className="px-3 py-2 text-muted-foreground">{s.externalMarks ?? '—'}</td>
                      <td className="px-3 py-2 font-medium">{s.total ?? '—'}</td>
                      <td className="px-3 py-2">{s.grade ?? '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ) : (
          <EmptyState title="No result yet" description="Your result for this term hasn't been published." className="py-6" />
        )}
      </CardContent>
    </Card>
  )
}

export default function ExaminationsPage() {
  const user = useCurrentUser()
  const canWrite = hasAnyRole(user, [UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.EXAM_CELL])
  const isStaff = hasAnyRole(user, [...RoleGroups.TEACHING_STAFF, UserRole.EXAM_CELL])
  const isStudent = hasAnyRole(user, [UserRole.STUDENT])

  const scope = useAcademicScope()

  return (
    <div>
      <PageHeader title="Examinations" description="Manage examination schedules, results, and grades." breadcrumb="Operations" />
      <PageContainer>
        <AcademicScopeSelector scope={scope} showOffering={false} />

        {!scope.batchId ? (
          <EmptyState title="Select a batch" description="Choose a department, course and batch to view its exams." />
        ) : (
          <>
            {isStaff && <ExamsPanel scope={scope} canWrite={canWrite} />}
            {isStudent && <MyResultsCard termId={scope.termId} />}
          </>
        )}
      </PageContainer>
    </div>
  )
}
