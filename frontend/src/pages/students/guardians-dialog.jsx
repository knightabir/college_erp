import { useState } from 'react'
import { toast } from 'sonner'
import { Plus, Phone, Mail, Star } from 'lucide-react'
import {
  Dialog, DialogHeader, DialogTitle, DialogDescription, DialogFooter,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Spinner } from '@/components/ui/spinner'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { EmptyState } from '@/components/shared/empty-state'
import { TableLoadingState } from '@/components/shared/loading-state'
import { useGuardians, useAddGuardian } from '@/api/students'
import { getFieldErrors, getErrorMessage } from '@/lib/api-error'

const RELATIONSHIPS = ['FATHER', 'MOTHER', 'GUARDIAN']

const EMPTY_FORM = { name: '', relationship: '', phone: '', email: '', occupation: '', primary: false }

/** View + add guardians for a single student — nested inside the Students page's row actions. */
export function GuardiansDialog({ student, open, onOpenChange, canAdd }) {
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
  const [errors, setErrors] = useState({})

  const { data: guardians, isLoading, isError, error } = useGuardians(student?.id)
  const addGuardian = useAddGuardian()

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }))
    setErrors((e) => ({ ...e, [field]: undefined }))
  }

  function resetAndClose(v) {
    setShowForm(false)
    setForm(EMPTY_FORM)
    setErrors({})
    onOpenChange(v)
  }

  async function handleAdd(e) {
    e.preventDefault()
    if (!student) return
    setErrors({})
    try {
      await addGuardian.mutateAsync({ studentId: student.id, ...form })
      toast.success('Guardian added.')
      setShowForm(false)
      setForm(EMPTY_FORM)
    } catch (err) {
      const fieldErrors = getFieldErrors(err)
      setErrors({ ...fieldErrors, _global: Object.keys(fieldErrors).length ? undefined : getErrorMessage(err) })
    }
  }

  return (
    <Dialog isOpen={open} onOpenChange={resetAndClose} className="sm:max-w-[480px]">
      <DialogHeader>
        <DialogTitle>Guardians</DialogTitle>
        <DialogDescription>{student ? `Guardian contacts for ${student.fullName}.` : ''}</DialogDescription>
      </DialogHeader>

      {isLoading ? (
        <TableLoadingState rows={2} cols={1} />
      ) : isError ? (
        <p className="py-6 text-center text-sm text-destructive">{getErrorMessage(error)}</p>
      ) : guardians?.length ? (
        <ul className="flex flex-col gap-2">
          {guardians.map((g) => (
            <li key={g.id} className="rounded-sm border border-border p-3">
              <div className="flex items-center justify-between">
                <p className="flex items-center gap-1.5 text-sm font-medium text-foreground">
                  {g.name}
                  {g.primary && <Star className="size-3 fill-amber-400 text-amber-400" />}
                </p>
                <span className="text-xs text-muted-foreground">{g.relationship}</span>
              </div>
              <div className="mt-1 flex flex-wrap gap-x-4 gap-y-0.5 text-xs text-muted-foreground">
                <span className="flex items-center gap-1"><Phone className="size-3" />{g.phone}</span>
                {g.email && <span className="flex items-center gap-1"><Mail className="size-3" />{g.email}</span>}
              </div>
            </li>
          ))}
        </ul>
      ) : (
        !showForm && <EmptyState title="No guardians on record" description="Add a parent or guardian contact for this student." />
      )}

      {showForm && (
        <form onSubmit={handleAdd} noValidate className="mt-2 flex flex-col gap-3 border-t border-border pt-3">
          <div className="grid grid-cols-2 gap-3">
            <div className="col-span-2 flex flex-col gap-1">
              <Label htmlFor="g-name">Name</Label>
              <Input id="g-name" value={form.name} onChange={(e) => set('name', e.target.value)} aria-invalid={!!errors.name} />
              {errors.name && <p className="text-xs text-destructive">{errors.name}</p>}
            </div>
            <div className="flex flex-col gap-1">
              <Label>Relationship</Label>
              <Select value={form.relationship} onValueChange={(v) => set('relationship', v)}>
                <SelectTrigger aria-invalid={!!errors.relationship}><SelectValue placeholder="Select…" /></SelectTrigger>
                <SelectContent>
                  {RELATIONSHIPS.map((r) => <SelectItem key={r} value={r}>{r[0] + r.slice(1).toLowerCase()}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            <div className="flex flex-col gap-1">
              <Label htmlFor="g-phone">Phone</Label>
              <Input id="g-phone" value={form.phone} onChange={(e) => set('phone', e.target.value)} aria-invalid={!!errors.phone} />
            </div>
            <div className="col-span-2 flex flex-col gap-1">
              <Label htmlFor="g-email">Email (optional)</Label>
              <Input id="g-email" type="email" value={form.email} onChange={(e) => set('email', e.target.value)} />
            </div>
          </div>
          {errors._global && <p className="rounded-sm border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">{errors._global}</p>}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" size="sm" onClick={() => setShowForm(false)} disabled={addGuardian.isPending}>Cancel</Button>
            <Button type="submit" size="sm" disabled={addGuardian.isPending}>
              {addGuardian.isPending ? <><Spinner className="mr-2 size-3.5" />Adding…</> : 'Add Guardian'}
            </Button>
          </div>
        </form>
      )}

      <DialogFooter className="mt-2">
        {canAdd && !showForm && (
          <Button size="sm" variant="outline" onClick={() => setShowForm(true)}>
            <Plus className="mr-1.5 size-3.5" /> Add Guardian
          </Button>
        )}
      </DialogFooter>
    </Dialog>
  )
}
