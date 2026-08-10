import { useState } from 'react'
import { toast } from 'sonner'
import { CalendarPlus, Save, Send, AlertTriangle, ClipboardCheck } from 'lucide-react'
import { PageHeader } from '@/components/shared/page-header'
import { PageContainer } from '@/components/shared/page-container'
import { AcademicScopeSelector } from '@/components/shared/academic-scope-selector'
import { EmptyState } from '@/components/shared/empty-state'
import { ErrorState } from '@/components/shared/error-state'
import { TableLoadingState } from '@/components/shared/loading-state'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Badge } from '@/components/ui/badge'
import { Spinner } from '@/components/ui/spinner'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { useCurrentUser } from '@/stores/auth-store'
import { hasAnyRole, RoleGroups, UserRole } from '@/lib/rbac'
import { getErrorMessage } from '@/lib/api-error'
import { useAcademicScope } from '@/api/academic'
import { useStudentsByBatch, useOwnStudentProfile } from '@/api/students'
import {
  useOpenSession, useMarkAttendance, useSubmitSession, useDefaulters,
  useAttendanceSummary, useApplyCondonation,
} from '@/api/attendance'

const STATUS_OPTIONS = ['PRESENT', 'ABSENT', 'LATE', 'EXCUSED']

function SessionMarking({ offeringId, sectionId, batchId }) {
  const [session, setSession] = useState(null)
  const [sessionDate, setSessionDate] = useState(() => new Date().toISOString().slice(0, 10))
  const [marks, setMarks] = useState({})
  const [submitted, setSubmitted] = useState(false)

  const { data: rosterPage, isLoading: rosterLoading } = useStudentsByBatch(batchId, { size: 200 })
  const roster = (rosterPage?.content ?? [])

  const openSession = useOpenSession()
  const markAttendance = useMarkAttendance()
  const submitSession = useSubmitSession()

  async function handleOpen() {
    try {
      const created = await openSession.mutateAsync({ offeringId, sectionId, sessionDate })
      setSession(created)
      setMarks(Object.fromEntries(roster.map((s) => [s.id, 'PRESENT'])))
      setSubmitted(false)
    } catch (err) {
      toast.error('Could not open session', { description: getErrorMessage(err) })
    }
  }

  async function handleSaveMarks() {
    try {
      await markAttendance.mutateAsync({ sessionId: session.id, marks })
      toast.success('Attendance saved.')
    } catch (err) {
      toast.error('Could not save marks', { description: getErrorMessage(err) })
    }
  }

  async function handleSubmit() {
    try {
      await submitSession.mutateAsync(session.id)
      setSubmitted(true)
      toast.success('Session submitted.')
    } catch (err) {
      toast.error('Could not submit session', { description: getErrorMessage(err) })
    }
  }

  if (!session) {
    return (
      <Card className="rounded-sm shadow-none">
        <CardHeader>
          <CardTitle className="text-sm font-semibold">Open a Session</CardTitle>
          <CardDescription className="text-xs">Start a new attendance session for this subject and section.</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex items-end gap-3">
            <div className="flex flex-col gap-1">
              <Label htmlFor="session-date" className="text-xs">Date</Label>
              <Input id="session-date" type="date" className="h-8 w-40 text-sm" value={sessionDate} onChange={(e) => setSessionDate(e.target.value)} />
            </div>
            <Button size="sm" onClick={handleOpen} disabled={openSession.isPending}>
              {openSession.isPending ? <Spinner className="mr-1.5 size-3.5" /> : <CalendarPlus className="mr-1.5 size-3.5" />}
              Open Session
            </Button>
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className="rounded-sm shadow-none">
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="text-sm font-semibold">Mark Attendance — {sessionDate}</CardTitle>
            <CardDescription className="text-xs">{roster.length} students in this section</CardDescription>
          </div>
          {submitted && <Badge className="text-[10px]">Submitted</Badge>}
        </div>
      </CardHeader>
      <CardContent>
        {rosterLoading ? (
          <TableLoadingState rows={5} cols={2} />
        ) : (
          <div className="flex flex-col divide-y divide-border rounded-sm border border-border">
            {roster.map((s) => (
              <div key={s.id} className="flex items-center justify-between gap-3 px-3 py-2">
                <div>
                  <p className="text-sm font-medium text-foreground">{s.fullName}</p>
                  <p className="text-xs text-muted-foreground">{s.enrollmentNumber}</p>
                </div>
                <Select
                  value={marks[s.id] ?? 'PRESENT'}
                  onValueChange={(v) => setMarks((m) => ({ ...m, [s.id]: v }))}
                  isDisabled={submitted}
                >
                  <SelectTrigger className="h-8 w-32 text-sm"><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {STATUS_OPTIONS.map((o) => <SelectItem key={o} value={o}>{o[0] + o.slice(1).toLowerCase()}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
            ))}
          </div>
        )}

        {!submitted && (
          <div className="mt-4 flex justify-end gap-2">
            <Button variant="outline" size="sm" onClick={handleSaveMarks} disabled={markAttendance.isPending}>
              {markAttendance.isPending ? <Spinner className="mr-1.5 size-3.5" /> : <Save className="mr-1.5 size-3.5" />}
              Save Marks
            </Button>
            <Button size="sm" onClick={handleSubmit} disabled={submitSession.isPending}>
              {submitSession.isPending ? <Spinner className="mr-1.5 size-3.5" /> : <Send className="mr-1.5 size-3.5" />}
              Submit Session
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function DefaultersCard({ offeringId }) {
  const { data: defaulters, isLoading, isError, error, refetch } = useDefaulters(offeringId)
  if (!offeringId) return null

  return (
    <Card className="rounded-sm shadow-none">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-sm font-semibold">
          <AlertTriangle className="size-4 text-amber-600" /> Attendance Defaulters
        </CardTitle>
        <CardDescription className="text-xs">Students below the minimum attendance threshold for this subject.</CardDescription>
      </CardHeader>
      <CardContent className="p-0">
        {isLoading ? (
          <TableLoadingState rows={3} cols={3} />
        ) : isError ? (
          <ErrorState description={getErrorMessage(error)} onRetry={refetch} />
        ) : !defaulters?.length ? (
          <EmptyState icon={ClipboardCheck} title="No defaulters" description="Every student meets the minimum attendance requirement." className="py-10" />
        ) : (
          <div className="divide-y divide-border">
            {defaulters.map((d) => (
              <div key={d.studentId} className="flex items-center justify-between px-5 py-2.5">
                <div>
                  <p className="text-sm font-medium text-foreground">{d.studentName}</p>
                  <p className="text-xs text-muted-foreground">{d.enrollmentNumber}</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-semibold text-destructive">{d.attendancePercentage.toFixed(1)}%</p>
                  <p className="text-xs text-muted-foreground">min. {d.minRequiredPct}%</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

/** Student-facing view: their own attendance % for the selected subject + a condonation form. */
function MyAttendanceCard({ offeringId, termId }) {
  const { data: student } = useOwnStudentProfile()
  const { data: summary, isLoading, isError, error } = useAttendanceSummary(student?.id, offeringId)
  const [reason, setReason] = useState('')
  const applyCondonation = useApplyCondonation()

  async function handleApply(e) {
    e.preventDefault()
    try {
      await applyCondonation.mutateAsync({ offeringId, termId, reason })
      toast.success('Condonation request submitted.')
      setReason('')
    } catch (err) {
      toast.error('Could not submit request', { description: getErrorMessage(err) })
    }
  }

  return (
    <Card className="rounded-sm shadow-none">
      <CardHeader>
        <CardTitle className="text-sm font-semibold">My Attendance</CardTitle>
        <CardDescription className="text-xs">Your attendance record for this subject.</CardDescription>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <Spinner className="size-4" />
        ) : isError ? (
          <p className="text-sm text-destructive">{getErrorMessage(error)}</p>
        ) : summary ? (
          <div className="flex items-center gap-6">
            <div>
              <p className="text-3xl font-semibold text-foreground">{summary.attendancePercentage.toFixed(1)}%</p>
              <p className="text-xs text-muted-foreground">{summary.present} / {summary.totalClasses} classes attended</p>
            </div>
            <Badge variant={summary.eligibleForExam ? 'default' : 'destructive'}>
              {summary.eligibleForExam ? 'Eligible for exam' : 'Not eligible for exam'}
            </Badge>
          </div>
        ) : null}

        <form onSubmit={handleApply} className="mt-5 flex flex-col gap-2 border-t border-border pt-4">
          <Label htmlFor="condonation-reason" className="text-sm">Apply for attendance condonation</Label>
          <Textarea id="condonation-reason" rows={3} placeholder="Reason for shortfall…" value={reason} onChange={(e) => setReason(e.target.value)} required />
          <Button type="submit" size="sm" className="w-fit" disabled={applyCondonation.isPending}>
            {applyCondonation.isPending ? <><Spinner className="mr-1.5 size-3.5" />Submitting…</> : 'Submit Request'}
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}

export default function AttendancePage() {
  const user = useCurrentUser()
  const isStaff = hasAnyRole(user, [...RoleGroups.TEACHING_STAFF, UserRole.LAB_ASSISTANT, UserRole.EXAM_CELL])
  const isStudent = hasAnyRole(user, [UserRole.STUDENT])

  const scope = useAcademicScope()
  const [sectionId, setSectionId] = useState('')

  return (
    <div>
      <PageHeader title="Attendance" description="Track and manage student attendance." breadcrumb="Operations" />
      <PageContainer>
        <AcademicScopeSelector scope={scope} />

        {scope.termId && (
          <div className="flex min-w-40 flex-col gap-1">
            <span className="text-xs font-medium text-muted-foreground">Section</span>
            <Select value={sectionId} onValueChange={setSectionId}>
              <SelectTrigger className="h-8 w-52 text-sm"><SelectValue placeholder="Select section" /></SelectTrigger>
              <SelectContent>
                {scope.sections.map((s) => <SelectItem key={s.id} value={s.id}>{s.name}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>
        )}

        {!scope.offeringId || (isStaff && !sectionId) ? (
          <EmptyState title="Select a subject and section" description="Choose a department, course, batch, term, subject and section to continue." />
        ) : (
          <>
            {isStaff && <SessionMarking offeringId={scope.offeringId} sectionId={sectionId} batchId={scope.batchId} />}
            {isStaff && <DefaultersCard offeringId={scope.offeringId} />}
            {isStudent && <MyAttendanceCard offeringId={scope.offeringId} termId={scope.termId} />}
          </>
        )}
      </PageContainer>
    </div>
  )
}
