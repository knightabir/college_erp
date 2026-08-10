import { useState } from 'react'
import { toast } from 'sonner'
import { Search, Plus, BookMarked, Barcode, MoreHorizontal, ScanLine } from 'lucide-react'
import { PageHeader } from '@/components/shared/page-header'
import { PageContainer } from '@/components/shared/page-container'
import { DataTable } from '@/components/shared/data-table'
import { FormDialog } from '@/components/shared/form-dialog'
import { EmptyState } from '@/components/shared/empty-state'
import { StatusBadge } from '@/components/shared/status-badge'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Spinner } from '@/components/ui/spinner'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import {
  DropdownMenu, DropdownMenuItem, DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { useCurrentUser } from '@/stores/auth-store'
import { hasAnyRole, RoleGroups } from '@/lib/rbac'
import { getErrorMessage, getFieldErrors } from '@/lib/api-error'
import {
  useSearchTitles, useCreateTitle, useCopies, useAddCopy, useMarkCopyLost,
  useIssueBook, useReturnBook, useRenewBook, useFinesByUser, usePayFine, useWaiveFine,
  useJoinReservation,
} from '@/api/library'

const COPY_CONDITIONS = ['EXCELLENT', 'GOOD', 'FAIR', 'POOR', 'DAMAGED', 'LOST']
const BORROWER_TYPES = ['STUDENT', 'FACULTY', 'STAFF']
const COPY_STATUS_BADGE = {
  AVAILABLE: { status: 'active', label: 'Available' },
  ISSUED: { status: 'pending', label: 'Issued' },
  RESERVED: { status: 'processing', label: 'Reserved' },
  LOST: { status: 'failed', label: 'Lost' },
  RETIRED: { status: 'inactive', label: 'Retired' },
}
const FINE_STATUS_BADGE = {
  UNPAID: { status: 'unpaid', label: 'Unpaid' },
  PARTIAL: { status: 'pending', label: 'Partial' },
  PAID: { status: 'paid', label: 'Paid' },
  WAIVED: { status: 'processing', label: 'Waived' },
}

function AddTitleForm({ onSubmit, onCancel }) {
  const [form, setForm] = useState({ title: '', author: '', isbn: '', publisher: '', publicationYear: '', edition: '', language: '' })
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
      await onSubmit({ ...form, publicationYear: form.publicationYear ? Number(form.publicationYear) : null })
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
      {field('title', 'Title', { value: form.title, onChange: (e) => set('title', e.target.value), className: 'col-span-2' })}
      {field('author', 'Author', { value: form.author, onChange: (e) => set('author', e.target.value) })}
      {field('isbn', 'ISBN', { value: form.isbn, onChange: (e) => set('isbn', e.target.value) })}
      {field('publisher', 'Publisher', { value: form.publisher, onChange: (e) => set('publisher', e.target.value) })}
      {field('publicationYear', 'Year', { type: 'number', value: form.publicationYear, onChange: (e) => set('publicationYear', e.target.value) })}
      {field('edition', 'Edition', { value: form.edition, onChange: (e) => set('edition', e.target.value) })}
      {field('language', 'Language', { value: form.language, onChange: (e) => set('language', e.target.value) })}
      {errors._global && <p className="col-span-2 rounded-sm border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">{errors._global}</p>}
      <div className="col-span-2 flex justify-end gap-2 border-t border-border pt-4">
        <Button type="button" variant="outline" onClick={onCancel} disabled={loading}>Cancel</Button>
        <Button type="submit" disabled={loading}>{loading ? (<><Spinner className="mr-2 size-3.5" />Saving…</>) : 'Add Title'}</Button>
      </div>
    </form>
  )
}

function CopiesDialog({ title, open, onOpenChange, canManage }) {
  const [showAdd, setShowAdd] = useState(false)
  const [shelfLocation, setShelfLocation] = useState('')
  const [rackNumber, setRackNumber] = useState('')
  const { data: copies, isLoading } = useCopies(title?.id)
  const addCopy = useAddCopy()
  const markLost = useMarkCopyLost()

  async function handleAdd(e) {
    e.preventDefault()
    try {
      const copy = await addCopy.mutateAsync({ titleId: title.id, shelfLocation, rackNumber })
      toast.success('Copy added.', { description: `Barcode ${copy.barcode}` })
      setShowAdd(false); setShelfLocation(''); setRackNumber('')
    } catch (err) {
      toast.error('Could not add copy', { description: getErrorMessage(err) })
    }
  }

  return (
    <FormDialog open={open} onOpenChange={onOpenChange} title="Book Copies" description={title ? `Physical copies of "${title.title}".` : ''} size="lg">
      {isLoading ? <Spinner className="size-4" /> : !copies?.length ? (
        <EmptyState icon={Barcode} title="No copies yet" description="Add the first physical copy of this title." />
      ) : (
        <div className="flex flex-col gap-1.5">
          {copies.map((c) => {
            const badge = COPY_STATUS_BADGE[c.status] ?? { status: 'inactive', label: c.status }
            return (
              <div key={c.id} className="flex items-center justify-between rounded-sm border border-border px-3 py-2">
                <div>
                  <p className="font-mono text-xs text-foreground">{c.barcode}</p>
                  <p className="text-xs text-muted-foreground">{c.shelfLocation || '—'} {c.rackNumber ? `· Rack ${c.rackNumber}` : ''} · {c.condition}</p>
                </div>
                <div className="flex items-center gap-2">
                  <StatusBadge status={badge.status} label={badge.label} />
                  {canManage && c.status !== 'LOST' && (
                    <Button size="xs" variant="outline" onClick={() => markLost.mutate(c.id, { onSuccess: () => toast.success('Copy marked lost.') })}>
                      Mark Lost
                    </Button>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}

      {canManage && (
        showAdd ? (
          <form onSubmit={handleAdd} className="mt-3 flex items-end gap-2 border-t border-border pt-3">
            <div className="flex flex-col gap-1"><Label className="text-xs">Shelf</Label><Input className="h-8 w-28 text-sm" value={shelfLocation} onChange={(e) => setShelfLocation(e.target.value)} /></div>
            <div className="flex flex-col gap-1"><Label className="text-xs">Rack</Label><Input className="h-8 w-24 text-sm" value={rackNumber} onChange={(e) => setRackNumber(e.target.value)} /></div>
            <Button size="sm" type="submit" disabled={addCopy.isPending}>Add</Button>
            <Button size="sm" type="button" variant="outline" onClick={() => setShowAdd(false)}>Cancel</Button>
          </form>
        ) : (
          <Button size="sm" variant="outline" className="mt-3" onClick={() => setShowAdd(true)}><Plus className="mr-1.5 size-3.5" /> Add Copy</Button>
        )
      )}
    </FormDialog>
  )
}

function CatalogTab({ canManage }) {
  const [q, setQ] = useState('')
  const [addOpen, setAddOpen] = useState(false)
  const [copiesTarget, setCopiesTarget] = useState(null)
  const { data, isLoading, isError, error, refetch } = useSearchTitles({ q, size: 50 })
  const createTitle = useCreateTitle()
  const joinReservation = useJoinReservation()

  const titles = data?.content ?? []

  async function handleAdd(form) {
    await createTitle.mutateAsync(form)
    setAddOpen(false)
    toast.success('Title added to catalog.')
  }

  async function handleReserve(title) {
    try {
      const reservation = await joinReservation.mutateAsync(title.id)
      toast.success('Reservation placed.', {
        description: reservation.queuePosition === 0 ? 'A copy is ready for pickup.' : `You are #${reservation.queuePosition} in the queue.`,
      })
    } catch (err) {
      toast.error('Could not reserve title', { description: getErrorMessage(err) })
    }
  }

  const columns = [
    { key: 'title', label: 'Title', render: (t) => <span className="font-medium text-foreground">{t.title}</span> },
    { key: 'author', label: 'Author', className: 'text-muted-foreground' },
    { key: 'isbn', label: 'ISBN', className: 'font-mono text-xs text-muted-foreground' },
    { key: 'availableCopies', label: 'Available', className: 'w-24', render: (t) => `${t.availableCopies} / ${t.totalCopies}` },
    {
      key: 'actions', label: '', className: 'w-10 text-right',
      render: (t) => (
        <DropdownMenuTrigger>
          <Button variant="ghost" size="icon-xs" className="text-muted-foreground"><MoreHorizontal className="size-4" /></Button>
          <DropdownMenu className="w-40">
            {canManage && <DropdownMenuItem onSelect={() => setCopiesTarget(t)}><Barcode className="mr-2 size-3.5" /> View Copies</DropdownMenuItem>}
            <DropdownMenuItem onSelect={() => handleReserve(t)}><BookMarked className="mr-2 size-3.5" /> Reserve</DropdownMenuItem>
          </DropdownMenu>
        </DropdownMenuTrigger>
      ),
    },
  ]

  return (
    <>
      <div className="mb-4 flex items-center justify-between gap-3">
        <div className="relative max-w-xs flex-1">
          <Search className="pointer-events-none absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
          <Input className="h-8 pl-8 text-sm" placeholder="Search by title, author, ISBN…" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
        {canManage && <Button size="sm" onClick={() => setAddOpen(true)}><Plus className="mr-1.5 size-3.5" /> Add Title</Button>}
      </div>

      <DataTable
        columns={columns}
        data={titles}
        loading={isLoading}
        error={isError ? getErrorMessage(error) : null}
        onRetry={refetch}
        emptyTitle="No titles found"
      />

      <FormDialog open={addOpen} onOpenChange={setAddOpen} title="Add Title" description="Add a new book title to the catalog." size="lg">
        <AddTitleForm onSubmit={handleAdd} onCancel={() => setAddOpen(false)} />
      </FormDialog>

      <CopiesDialog title={copiesTarget} open={!!copiesTarget} onOpenChange={(v) => !v && setCopiesTarget(null)} canManage={canManage} />
    </>
  )
}

function CirculationTab() {
  const issueBook = useIssueBook()
  const returnBook = useReturnBook()
  const renewBook = useRenewBook()

  const [issueForm, setIssueForm] = useState({ barcode: '', borrowerUserId: '', borrowerName: '', borrowerType: '' })
  const [returnForm, setReturnForm] = useState({ barcode: '', conditionOnReturn: '' })
  const [renewBarcode, setRenewBarcode] = useState('')

  async function handleIssue(e) {
    e.preventDefault()
    try {
      await issueBook.mutateAsync(issueForm)
      toast.success('Book issued.')
      setIssueForm({ barcode: '', borrowerUserId: '', borrowerName: '', borrowerType: '' })
    } catch (err) {
      toast.error('Could not issue book', { description: getErrorMessage(err) })
    }
  }

  async function handleReturn(e) {
    e.preventDefault()
    try {
      const result = await returnBook.mutateAsync(returnForm)
      toast.success('Book returned.', { description: result.fineAmount > 0 ? `Fine of ₹${result.fineAmount} recorded.` : undefined })
      setReturnForm({ barcode: '', conditionOnReturn: '' })
    } catch (err) {
      toast.error('Could not process return', { description: getErrorMessage(err) })
    }
  }

  async function handleRenew(e) {
    e.preventDefault()
    try {
      const result = await renewBook.mutateAsync({ barcode: renewBarcode })
      toast.success('Loan renewed.', { description: `New due date: ${new Date(result.dueAt).toLocaleDateString()}` })
      setRenewBarcode('')
    } catch (err) {
      toast.error('Could not renew loan', { description: getErrorMessage(err) })
    }
  }

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
      <Card className="rounded-sm shadow-none">
        <CardHeader><CardTitle className="flex items-center gap-2 text-sm font-semibold"><ScanLine className="size-4" /> Issue Book</CardTitle></CardHeader>
        <CardContent>
          <form onSubmit={handleIssue} className="flex flex-col gap-3">
            <Input placeholder="Barcode" value={issueForm.barcode} onChange={(e) => setIssueForm((f) => ({ ...f, barcode: e.target.value }))} required />
            <Input placeholder="Borrower User ID" value={issueForm.borrowerUserId} onChange={(e) => setIssueForm((f) => ({ ...f, borrowerUserId: e.target.value }))} required />
            <Input placeholder="Borrower Name" value={issueForm.borrowerName} onChange={(e) => setIssueForm((f) => ({ ...f, borrowerName: e.target.value }))} required />
            <Select value={issueForm.borrowerType} onValueChange={(v) => setIssueForm((f) => ({ ...f, borrowerType: v }))}>
              <SelectTrigger><SelectValue placeholder="Borrower type" /></SelectTrigger>
              <SelectContent>{BORROWER_TYPES.map((t) => <SelectItem key={t} value={t}>{t}</SelectItem>)}</SelectContent>
            </Select>
            <Button type="submit" size="sm" disabled={issueBook.isPending}>{issueBook.isPending ? <Spinner className="mr-1.5 size-3.5" /> : null}Issue</Button>
          </form>
        </CardContent>
      </Card>

      <Card className="rounded-sm shadow-none">
        <CardHeader><CardTitle className="text-sm font-semibold">Return Book</CardTitle></CardHeader>
        <CardContent>
          <form onSubmit={handleReturn} className="flex flex-col gap-3">
            <Input placeholder="Barcode" value={returnForm.barcode} onChange={(e) => setReturnForm((f) => ({ ...f, barcode: e.target.value }))} required />
            <Select value={returnForm.conditionOnReturn} onValueChange={(v) => setReturnForm((f) => ({ ...f, conditionOnReturn: v }))}>
              <SelectTrigger><SelectValue placeholder="Condition on return" /></SelectTrigger>
              <SelectContent>{COPY_CONDITIONS.map((c) => <SelectItem key={c} value={c}>{c}</SelectItem>)}</SelectContent>
            </Select>
            <Button type="submit" size="sm" disabled={returnBook.isPending}>{returnBook.isPending ? <Spinner className="mr-1.5 size-3.5" /> : null}Return</Button>
          </form>
        </CardContent>
      </Card>

      <Card className="rounded-sm shadow-none">
        <CardHeader><CardTitle className="text-sm font-semibold">Renew Loan</CardTitle></CardHeader>
        <CardContent>
          <form onSubmit={handleRenew} className="flex flex-col gap-3">
            <Input placeholder="Barcode" value={renewBarcode} onChange={(e) => setRenewBarcode(e.target.value)} required />
            <Button type="submit" size="sm" disabled={renewBook.isPending}>{renewBook.isPending ? <Spinner className="mr-1.5 size-3.5" /> : null}Renew</Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}

function FinesTab({ canManage }) {
  const user = useCurrentUser()
  const [userId, setUserId] = useState(canManage ? '' : user?.userId ?? '')
  const { data: fines, isLoading, refetch } = useFinesByUser(userId)
  const payFine = usePayFine()
  const waiveFine = useWaiveFine()

  return (
    <Card className="rounded-sm shadow-none">
      <CardHeader>
        <CardTitle className="text-sm font-semibold">Library Fines</CardTitle>
        <CardDescription className="text-xs">{canManage ? 'Look up fines for any user by their user ID.' : 'Your outstanding library fines.'}</CardDescription>
      </CardHeader>
      <CardContent>
        {canManage && (
          <div className="mb-4 flex items-end gap-2">
            <div className="flex flex-col gap-1"><Label className="text-xs">User ID</Label><Input className="h-8 w-72 text-sm" value={userId} onChange={(e) => setUserId(e.target.value)} /></div>
            <Button size="sm" variant="outline" onClick={refetch} disabled={!userId}>Look up</Button>
          </div>
        )}
        {isLoading ? <Spinner className="size-4" /> : !fines?.length ? (
          <EmptyState title="No fines" description="No fines on record for this user." className="py-8" />
        ) : (
          <div className="flex flex-col gap-2">
            {fines.map((f) => {
              const badge = FINE_STATUS_BADGE[f.status] ?? { status: 'inactive', label: f.status }
              return (
                <div key={f.id} className="flex items-center justify-between rounded-sm border border-border px-3 py-2">
                  <div>
                    <p className="text-sm font-medium text-foreground">₹{f.amount}</p>
                    <p className="text-xs text-muted-foreground">Balance due ₹{f.balanceDue}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <StatusBadge status={badge.status} label={badge.label} />
                    {canManage && f.balanceDue > 0 && (
                      <>
                        <Button size="xs" variant="outline" onClick={() => payFine.mutate({ id: f.id, amount: f.balanceDue }, { onSuccess: () => toast.success('Fine paid.') })}>Pay</Button>
                        <Button size="xs" variant="outline" onClick={() => waiveFine.mutate({ id: f.id, reason: 'Waived by librarian' }, { onSuccess: () => toast.success('Fine waived.') })}>Waive</Button>
                      </>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

export default function LibraryPage() {
  const user = useCurrentUser()
  const canManage = hasAnyRole(user, RoleGroups.LIBRARY_STAFF)

  return (
    <div>
      <PageHeader title="Library" description="Manage library inventory, borrowing, and returns." breadcrumb="Operations" />
      <PageContainer>
        <Tabs defaultSelectedKey="catalog">
          <TabsList>
            <TabsTrigger id="catalog">Catalog</TabsTrigger>
            {canManage && <TabsTrigger id="circulation">Circulation</TabsTrigger>}
            <TabsTrigger id="fines">Fines</TabsTrigger>
          </TabsList>
          <TabsContent id="catalog" className="mt-4"><CatalogTab canManage={canManage} /></TabsContent>
          {canManage && <TabsContent id="circulation" className="mt-4"><CirculationTab /></TabsContent>}
          <TabsContent id="fines" className="mt-4"><FinesTab canManage={canManage} /></TabsContent>
        </Tabs>
      </PageContainer>
    </div>
  )
}
