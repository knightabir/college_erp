import { useState } from 'react'
import { toast } from 'sonner'
import { Plus, Receipt, Settings2 } from 'lucide-react'
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
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { useCurrentUser } from '@/stores/auth-store'
import { hasAnyRole, RoleGroups, UserRole } from '@/lib/rbac'
import { getErrorMessage, getFieldErrors } from '@/lib/api-error'
import { useAcademicScope } from '@/api/academic'
import { useStudentsByBatch, useOwnStudentProfile } from '@/api/students'
import {
  useStudentInvoices, useOutstandingInvoices, useGenerateInvoice, useCollectPayment,
  useFeeHeads, useCreateFeeHead, useFeeStructures, useCreateFeeStructure,
} from '@/api/finance'

const PAYMENT_MODES = ['CASH', 'CHEQUE', 'DEMAND_DRAFT', 'ONLINE_UPI', 'ONLINE_CARD', 'ONLINE_NET_BANKING', 'NEFT', 'RTGS']
const INVOICE_STATUS_BADGE = {
  UNPAID: { status: 'unpaid', label: 'Unpaid' },
  PARTIAL: { status: 'pending', label: 'Partially Paid' },
  PAID: { status: 'paid', label: 'Paid' },
  OVERDUE: { status: 'failed', label: 'Overdue' },
  WAIVED: { status: 'processing', label: 'Waived' },
  CANCELLED: { status: 'inactive', label: 'Cancelled' },
}
const money = (n) => `₹${Number(n ?? 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`

function CollectPaymentForm({ invoice, onSubmit, onCancel }) {
  const [amount, setAmount] = useState(String(invoice.balanceDue ?? ''))
  const [paymentMode, setPaymentMode] = useState('')
  const [transactionReference, setTransactionReference] = useState('')
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setErrors({})
    try {
      await onSubmit({ amount, paymentMode, transactionReference: transactionReference || undefined })
    } catch (err) {
      const fieldErrors = getFieldErrors(err)
      setErrors({ ...fieldErrors, _global: Object.keys(fieldErrors).length ? undefined : getErrorMessage(err) })
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4 py-2">
      <p className="text-sm text-muted-foreground">
        Invoice <span className="font-medium text-foreground">{invoice.invoiceNumber}</span> · balance due {money(invoice.balanceDue)}
      </p>
      <div className="flex flex-col gap-1">
        <Label htmlFor="pay-amount">Amount</Label>
        <Input id="pay-amount" type="number" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} aria-invalid={!!errors.amount} />
        {errors.amount && <p className="text-xs text-destructive">{errors.amount}</p>}
      </div>
      <div className="flex flex-col gap-1">
        <Label>Payment Mode</Label>
        <Select value={paymentMode} onValueChange={setPaymentMode}>
          <SelectTrigger aria-invalid={!!errors.paymentMode}><SelectValue placeholder="Select…" /></SelectTrigger>
          <SelectContent>
            {PAYMENT_MODES.map((m) => <SelectItem key={m} value={m}>{m.replace(/_/g, ' ')}</SelectItem>)}
          </SelectContent>
        </Select>
        {errors.paymentMode && <p className="text-xs text-destructive">{errors.paymentMode}</p>}
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor="pay-ref">Transaction Reference (optional)</Label>
        <Input id="pay-ref" value={transactionReference} onChange={(e) => setTransactionReference(e.target.value)} />
      </div>
      {errors._global && <p className="rounded-sm border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">{errors._global}</p>}
      <div className="flex justify-end gap-2 border-t border-border pt-4">
        <Button type="button" variant="outline" onClick={onCancel} disabled={loading}>Cancel</Button>
        <Button type="submit" disabled={loading || !paymentMode}>
          {loading ? (<><Spinner className="mr-2 size-3.5" />Recording…</>) : 'Record Payment'}
        </Button>
      </div>
    </form>
  )
}

function GenerateInvoiceForm({ studentId, courseId, batchId, academicYearId, onSubmit, onCancel }) {
  const [termNumber, setTermNumber] = useState(1)
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setErrors({})
    try {
      await onSubmit({ studentId, courseId, batchId, academicYearId, termNumber: Number(termNumber) })
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
        <Label htmlFor="term-number">Term Number</Label>
        <Input id="term-number" type="number" min={1} value={termNumber} onChange={(e) => setTermNumber(e.target.value)} aria-invalid={!!errors.termNumber} />
        {errors.termNumber && <p className="text-xs text-destructive">{errors.termNumber}</p>}
      </div>
      {errors._global && <p className="rounded-sm border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">{errors._global}</p>}
      <div className="flex justify-end gap-2 border-t border-border pt-4">
        <Button type="button" variant="outline" onClick={onCancel} disabled={loading}>Cancel</Button>
        <Button type="submit" disabled={loading}>
          {loading ? (<><Spinner className="mr-2 size-3.5" />Generating…</>) : 'Generate Invoice'}
        </Button>
      </div>
    </form>
  )
}

function InvoiceTable({ invoices, isLoading, canCollect, onCollect }) {
  if (isLoading) return <TableLoadingState rows={4} cols={5} />
  if (!invoices?.length) return <EmptyState icon={Receipt} title="No invoices" description="No fee invoices found for this student yet." className="py-10" />

  return (
    <div className="overflow-x-auto rounded-sm border border-border">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-border bg-muted/40 text-left text-xs uppercase text-muted-foreground">
            <th className="px-3 py-2">Invoice</th>
            <th className="px-3 py-2">Term</th>
            <th className="px-3 py-2">Net Amount</th>
            <th className="px-3 py-2">Paid</th>
            <th className="px-3 py-2">Balance</th>
            <th className="px-3 py-2">Due</th>
            <th className="px-3 py-2">Status</th>
            {canCollect && <th className="px-3 py-2" />}
          </tr>
        </thead>
        <tbody>
          {invoices.map((inv) => {
            const badge = INVOICE_STATUS_BADGE[inv.status] ?? { status: 'inactive', label: inv.status }
            return (
              <tr key={inv.id} className="border-b border-border last:border-0">
                <td className="px-3 py-2 font-mono text-xs">{inv.invoiceNumber}</td>
                <td className="px-3 py-2 text-muted-foreground">Term {inv.termNumber}</td>
                <td className="px-3 py-2">{money(inv.netAmount)}</td>
                <td className="px-3 py-2 text-muted-foreground">{money(inv.paidAmount)}</td>
                <td className="px-3 py-2 font-medium">{money(inv.balanceDue)}</td>
                <td className="px-3 py-2 text-muted-foreground">{inv.dueDate}</td>
                <td className="px-3 py-2"><StatusBadge status={badge.status} label={badge.label} /></td>
                {canCollect && (
                  <td className="px-3 py-2 text-right">
                    {inv.balanceDue > 0 && (
                      <Button size="xs" variant="outline" onClick={() => onCollect(inv)}>Collect</Button>
                    )}
                  </td>
                )}
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

function FeeConfigCard({ courseId }) {
  const [showAddHead, setShowAddHead] = useState(false)
  const [headForm, setHeadForm] = useState({ code: '', name: '', description: '' })
  const [showAddStructure, setShowAddStructure] = useState(false)
  const [structForm, setStructForm] = useState({ feeHeadId: '', termNumber: 1, amount: '', dueDateOffsetDays: 30, lateFeePerDay: 0 })

  const { data: feeHeads } = useFeeHeads()
  const { data: structures } = useFeeStructures(courseId)
  const createFeeHead = useCreateFeeHead()
  const createStructure = useCreateFeeStructure()

  async function handleAddHead(e) {
    e.preventDefault()
    try {
      await createFeeHead.mutateAsync(headForm)
      toast.success('Fee head created.')
      setShowAddHead(false)
      setHeadForm({ code: '', name: '', description: '' })
    } catch (err) {
      toast.error('Could not create fee head', { description: getErrorMessage(err) })
    }
  }

  async function handleAddStructure(e) {
    e.preventDefault()
    try {
      await createStructure.mutateAsync({
        courseId, feeHeadId: structForm.feeHeadId, termNumber: Number(structForm.termNumber),
        amount: structForm.amount, dueDateOffsetDays: Number(structForm.dueDateOffsetDays),
        lateFeePerDay: structForm.lateFeePerDay || 0,
      })
      toast.success('Fee structure created.')
      setShowAddStructure(false)
    } catch (err) {
      toast.error('Could not create fee structure', { description: getErrorMessage(err) })
    }
  }

  return (
    <Card className="rounded-sm shadow-none">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-sm font-semibold"><Settings2 className="size-4" /> Fee Configuration</CardTitle>
        <CardDescription className="text-xs">Fee heads and per-course fee structures.</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-5">
        <div>
          <div className="mb-2 flex items-center justify-between">
            <p className="text-xs font-semibold uppercase text-muted-foreground">Fee Heads</p>
            <Button size="xs" variant="outline" onClick={() => setShowAddHead((v) => !v)}><Plus className="mr-1 size-3" /> Add</Button>
          </div>
          <div className="flex flex-wrap gap-1.5">
            {(feeHeads ?? []).map((h) => <span key={h.id} className="rounded-sm border border-border bg-muted px-2 py-0.5 text-xs text-muted-foreground">{h.name}</span>)}
          </div>
          {showAddHead && (
            <form onSubmit={handleAddHead} className="mt-2 flex flex-wrap items-end gap-2">
              <Input className="h-7 w-24 text-xs" placeholder="Code" value={headForm.code} onChange={(e) => setHeadForm((f) => ({ ...f, code: e.target.value.toUpperCase() }))} required />
              <Input className="h-7 w-40 text-xs" placeholder="Name" value={headForm.name} onChange={(e) => setHeadForm((f) => ({ ...f, name: e.target.value }))} required />
              <Button type="submit" size="xs" disabled={createFeeHead.isPending}>Save</Button>
            </form>
          )}
        </div>

        {courseId && (
          <div>
            <div className="mb-2 flex items-center justify-between">
              <p className="text-xs font-semibold uppercase text-muted-foreground">Fee Structure (selected course)</p>
              <Button size="xs" variant="outline" onClick={() => setShowAddStructure((v) => !v)}><Plus className="mr-1 size-3" /> Add</Button>
            </div>
            {(structures ?? []).length ? (
              <ul className="flex flex-col gap-1">
                {structures.map((s) => (
                  <li key={s.id} className="flex items-center justify-between rounded-sm border border-border px-2.5 py-1.5 text-xs">
                    <span>{s.feeHeadName} · Term {s.termNumber}{s.batchId ? '' : ' (all batches)'}</span>
                    <span className="font-medium">{money(s.amount)}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-xs text-muted-foreground">No fee structure defined for this course yet.</p>
            )}
            {showAddStructure && (
              <form onSubmit={handleAddStructure} className="mt-2 flex flex-wrap items-end gap-2">
                <Select value={structForm.feeHeadId} onValueChange={(v) => setStructForm((f) => ({ ...f, feeHeadId: v }))}>
                  <SelectTrigger className="h-7 w-36 text-xs"><SelectValue placeholder="Fee head" /></SelectTrigger>
                  <SelectContent>{(feeHeads ?? []).map((h) => <SelectItem key={h.id} value={h.id}>{h.name}</SelectItem>)}</SelectContent>
                </Select>
                <Input className="h-7 w-20 text-xs" type="number" min={1} placeholder="Term" value={structForm.termNumber} onChange={(e) => setStructForm((f) => ({ ...f, termNumber: e.target.value }))} />
                <Input className="h-7 w-28 text-xs" type="number" step="0.01" placeholder="Amount" value={structForm.amount} onChange={(e) => setStructForm((f) => ({ ...f, amount: e.target.value }))} required />
                <Button type="submit" size="xs" disabled={createStructure.isPending || !structForm.feeHeadId}>Save</Button>
              </form>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function StaffFeesView({ user }) {
  const canGenerate = hasAnyRole(user, RoleGroups.FINANCE_OFFICE)
  const canCollect = hasAnyRole(user, [...RoleGroups.FINANCE_OFFICE, UserRole.FEE_COLLECTION_CLERK])
  const canConfigure = hasAnyRole(user, [UserRole.HEAD_ACCOUNTANT])

  const scope = useAcademicScope()
  const [studentId, setStudentId] = useState('')
  const [generateOpen, setGenerateOpen] = useState(false)
  const [payTarget, setPayTarget] = useState(null)

  const { data: rosterPage } = useStudentsByBatch(scope.batchId, { size: 200 })
  const { data: invoices, isLoading } = useStudentInvoices(studentId)
  const generateInvoice = useGenerateInvoice()
  const collectPayment = useCollectPayment()

  async function handleGenerate(body) {
    await generateInvoice.mutateAsync(body)
    setGenerateOpen(false)
    toast.success('Invoice generated.')
  }

  async function handleCollect(body) {
    // Guard against a stale/late invocation racing the dialog closing — never trust that a
    // closed-over "which invoice" state is still set by the time this callback actually runs.
    if (!payTarget) return
    await collectPayment.mutateAsync({ invoiceId: payTarget.id, ...body })
    setPayTarget(null)
    toast.success('Payment recorded.')
  }

  return (
    <>
      <AcademicScopeSelector scope={scope} showTerm={false} showOffering={false} />

      <div className="flex min-w-52 flex-col gap-1">
        <span className="text-xs font-medium text-muted-foreground">Student</span>
        <Select value={studentId} onValueChange={setStudentId} isDisabled={!scope.batchId}>
          <SelectTrigger className="h-8 w-64 text-sm"><SelectValue placeholder="Select student" /></SelectTrigger>
          <SelectContent>
            {(rosterPage?.content ?? []).map((s) => <SelectItem key={s.id} value={s.id}>{s.fullName} ({s.enrollmentNumber})</SelectItem>)}
          </SelectContent>
        </Select>
      </div>

      {studentId ? (
        <Card className="rounded-sm shadow-none">
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="text-sm font-semibold">Fee Invoices</CardTitle>
              {canGenerate && (
                <Button size="sm" onClick={() => setGenerateOpen(true)}><Plus className="mr-1.5 size-3.5" /> Generate Invoice</Button>
              )}
            </div>
          </CardHeader>
          <CardContent>
            <InvoiceTable invoices={invoices} isLoading={isLoading} canCollect={canCollect} onCollect={setPayTarget} />
          </CardContent>
        </Card>
      ) : (
        <EmptyState title="Select a student" description="Choose a batch and student to view fee invoices." />
      )}

      {canConfigure && <FeeConfigCard courseId={scope.courseId} />}

      <FormDialog open={generateOpen} onOpenChange={setGenerateOpen} title="Generate Invoice" description="Create a new fee invoice for this student.">
        <GenerateInvoiceForm
          studentId={studentId} courseId={scope.courseId} batchId={scope.batchId}
          academicYearId={scope.selectedBatch?.academicYearId}
          onSubmit={handleGenerate} onCancel={() => setGenerateOpen(false)}
        />
      </FormDialog>

      <FormDialog open={!!payTarget} onOpenChange={(v) => !v && setPayTarget(null)} title="Collect Payment" description="Record a payment against this invoice.">
        {payTarget && <CollectPaymentForm invoice={payTarget} onSubmit={handleCollect} onCancel={() => setPayTarget(null)} />}
      </FormDialog>
    </>
  )
}

function StudentFeesView() {
  const { data: student } = useOwnStudentProfile()
  const { data: outstanding, isLoading: outstandingLoading } = useOutstandingInvoices(student?.id)
  const { data: allInvoices, isLoading: allLoading } = useStudentInvoices(student?.id)

  return (
    <>
      <Card className="rounded-sm shadow-none">
        <CardHeader>
          <CardTitle className="text-sm font-semibold">Outstanding Dues</CardTitle>
          <CardDescription className="text-xs">Fees currently due for payment.</CardDescription>
        </CardHeader>
        <CardContent>
          <InvoiceTable invoices={outstanding} isLoading={outstandingLoading} canCollect={false} />
        </CardContent>
      </Card>
      <Card className="rounded-sm shadow-none">
        <CardHeader>
          <CardTitle className="text-sm font-semibold">All Invoices</CardTitle>
          <CardDescription className="text-xs">Your full fee invoice history. Contact the accounts office to make a payment.</CardDescription>
        </CardHeader>
        <CardContent>
          <InvoiceTable invoices={allInvoices} isLoading={allLoading} canCollect={false} />
        </CardContent>
      </Card>
    </>
  )
}

export default function FeeManagementPage() {
  const user = useCurrentUser()
  const isFinanceStaff = hasAnyRole(user, [...RoleGroups.FINANCE_OFFICE, UserRole.FEE_COLLECTION_CLERK, UserRole.AUDITOR, UserRole.ADMIN, UserRole.SUPER_ADMIN])
  const isStudent = hasAnyRole(user, [UserRole.STUDENT])

  return (
    <div>
      <PageHeader title="Fee Management" description="Manage fee structures, payments, and dues." breadcrumb="Finance & HR" />
      <PageContainer>
        {isFinanceStaff && <StaffFeesView user={user} />}
        {isStudent && <StudentFeesView />}
      </PageContainer>
    </div>
  )
}
