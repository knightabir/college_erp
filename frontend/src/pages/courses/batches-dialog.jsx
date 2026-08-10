import { useState } from 'react'
import { toast } from 'sonner'
import { Plus, Layers } from 'lucide-react'
import {
  Dialog, DialogHeader, DialogTitle, DialogDescription,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Spinner } from '@/components/ui/spinner'
import { Badge } from '@/components/ui/badge'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { EmptyState } from '@/components/shared/empty-state'
import { TableLoadingState } from '@/components/shared/loading-state'
import { useBatchesByCourse, useCreateBatch, useAcademicYears, useSectionsByBatch, useCreateSection } from '@/api/academic'
import { getErrorMessage, getFieldErrors } from '@/lib/api-error'

/** Batches (+ their sections) for one course — nested management, opened from the Courses page. */
export function BatchesDialog({ course, open, onOpenChange, canWrite }) {
  const [showAddBatch, setShowAddBatch] = useState(false)
  const [form, setForm] = useState({ label: '', academicYearId: '', intakeYear: new Date().getFullYear() })
  const [errors, setErrors] = useState({})
  const [expandedBatchId, setExpandedBatchId] = useState(null)

  const { data: batches, isLoading } = useBatchesByCourse(course?.id)
  const { data: academicYearsPage } = useAcademicYears({ size: 50 })
  const createBatch = useCreateBatch()

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }))
    setErrors((e) => ({ ...e, [field]: undefined }))
  }

  function resetAndClose(v) {
    setShowAddBatch(false)
    setForm({ label: '', academicYearId: '', intakeYear: new Date().getFullYear() })
    setErrors({})
    onOpenChange(v)
  }

  async function handleAddBatch(e) {
    e.preventDefault()
    setErrors({})
    try {
      await createBatch.mutateAsync({ ...form, courseId: course.id, intakeYear: Number(form.intakeYear) })
      toast.success('Batch created.')
      setShowAddBatch(false)
      setForm({ label: '', academicYearId: '', intakeYear: new Date().getFullYear() })
    } catch (err) {
      const fieldErrors = getFieldErrors(err)
      setErrors({ ...fieldErrors, _global: Object.keys(fieldErrors).length ? undefined : getErrorMessage(err) })
    }
  }

  return (
    <Dialog isOpen={open} onOpenChange={resetAndClose} className="sm:max-w-[560px]">
      <DialogHeader>
        <DialogTitle>Batches</DialogTitle>
        <DialogDescription>{course ? `Intake batches for ${course.name}.` : ''}</DialogDescription>
      </DialogHeader>

      {isLoading ? (
        <TableLoadingState rows={3} cols={1} />
      ) : batches?.length ? (
        <ul className="flex flex-col gap-2">
          {batches.map((b) => (
            <li key={b.id} className="rounded-sm border border-border">
              <button
                type="button"
                className="flex w-full items-center justify-between p-3 text-left"
                onClick={() => setExpandedBatchId((id) => (id === b.id ? null : b.id))}
              >
                <div>
                  <p className="text-sm font-medium text-foreground">{b.label}</p>
                  <p className="text-xs text-muted-foreground">Intake {b.intakeYear} · Term {b.currentTermNumber}</p>
                </div>
                <Badge variant={b.active ? 'default' : 'secondary'} className="text-[10px]">
                  {b.active ? 'Active' : 'Inactive'}
                </Badge>
              </button>
              {expandedBatchId === b.id && <BatchSections batchId={b.id} canWrite={canWrite} />}
            </li>
          ))}
        </ul>
      ) : (
        !showAddBatch && <EmptyState icon={Layers} title="No batches yet" description="Create the first intake batch for this course." />
      )}

      {showAddBatch && (
        <form onSubmit={handleAddBatch} noValidate className="mt-2 flex flex-col gap-3 border-t border-border pt-3">
          <div className="grid grid-cols-2 gap-3">
            <div className="col-span-2 flex flex-col gap-1">
              <Label htmlFor="b-label">Label</Label>
              <Input id="b-label" placeholder="B.Tech CS 2026" value={form.label} onChange={(e) => set('label', e.target.value)} aria-invalid={!!errors.label} />
              {errors.label && <p className="text-xs text-destructive">{errors.label}</p>}
            </div>
            <div className="flex flex-col gap-1">
              <Label>Academic Year</Label>
              <Select value={form.academicYearId} onValueChange={(v) => set('academicYearId', v)}>
                <SelectTrigger aria-invalid={!!errors.academicYearId}><SelectValue placeholder="Select…" /></SelectTrigger>
                <SelectContent>
                  {(academicYearsPage?.content ?? []).map((y) => <SelectItem key={y.id} value={y.id}>{y.label}</SelectItem>)}
                </SelectContent>
              </Select>
              {errors.academicYearId && <p className="text-xs text-destructive">{errors.academicYearId}</p>}
            </div>
            <div className="flex flex-col gap-1">
              <Label htmlFor="b-intake">Intake Year</Label>
              <Input id="b-intake" type="number" value={form.intakeYear} onChange={(e) => set('intakeYear', e.target.value)} />
            </div>
          </div>
          {errors._global && <p className="rounded-sm border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">{errors._global}</p>}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" size="sm" onClick={() => setShowAddBatch(false)} disabled={createBatch.isPending}>Cancel</Button>
            <Button type="submit" size="sm" disabled={createBatch.isPending}>
              {createBatch.isPending ? <><Spinner className="mr-2 size-3.5" />Creating…</> : 'Create Batch'}
            </Button>
          </div>
        </form>
      )}

      {canWrite && !showAddBatch && (
        <div className="border-t border-border pt-3">
          <Button size="sm" variant="outline" onClick={() => setShowAddBatch(true)}>
            <Plus className="mr-1.5 size-3.5" /> Add Batch
          </Button>
        </div>
      )}
    </Dialog>
  )
}

function BatchSections({ batchId, canWrite }) {
  const [showAdd, setShowAdd] = useState(false)
  const [name, setName] = useState('')
  const [capacity, setCapacity] = useState(60)
  const { data: sections, isLoading } = useSectionsByBatch(batchId)
  const createSection = useCreateSection()

  async function handleAdd(e) {
    e.preventDefault()
    try {
      await createSection.mutateAsync({ batchId, name, capacity: Number(capacity) })
      toast.success('Section added.')
      setShowAdd(false)
      setName('')
    } catch (err) {
      toast.error('Could not add section', { description: getErrorMessage(err) })
    }
  }

  return (
    <div className="border-t border-border bg-muted/30 p-3">
      {isLoading ? (
        <Spinner className="size-3.5" />
      ) : sections?.length ? (
        <div className="flex flex-wrap gap-1.5">
          {sections.map((s) => (
            <Badge key={s.id} variant="outline" className="text-[10px]">
              Section {s.name} · cap. {s.capacity}
            </Badge>
          ))}
        </div>
      ) : (
        <p className="text-xs text-muted-foreground">No sections yet.</p>
      )}

      {canWrite && (
        showAdd ? (
          <form onSubmit={handleAdd} className="mt-2 flex items-end gap-2">
            <div className="flex flex-col gap-1">
              <Label htmlFor="s-name" className="text-xs">Name</Label>
              <Input id="s-name" className="h-7 w-16 text-xs" value={name} onChange={(e) => setName(e.target.value)} required />
            </div>
            <div className="flex flex-col gap-1">
              <Label htmlFor="s-cap" className="text-xs">Capacity</Label>
              <Input id="s-cap" type="number" className="h-7 w-20 text-xs" value={capacity} onChange={(e) => setCapacity(e.target.value)} />
            </div>
            <Button type="submit" size="xs" disabled={createSection.isPending}>Add</Button>
            <Button type="button" size="xs" variant="outline" onClick={() => setShowAdd(false)}>Cancel</Button>
          </form>
        ) : (
          <Button size="xs" variant="outline" className="mt-2" onClick={() => setShowAdd(true)}>
            <Plus className="mr-1 size-3" /> Add Section
          </Button>
        )
      )}
    </div>
  )
}
