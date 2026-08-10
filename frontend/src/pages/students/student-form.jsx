import { useState } from 'react'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'
import { Spinner } from '@/components/ui/spinner'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { getFieldErrors, getErrorMessage } from '@/lib/api-error'

const GENDERS = ['MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY']
const CATEGORIES = ['GENERAL', 'OBC', 'SC', 'ST', 'EWS']

function titleCase(s) {
  return s.toLowerCase().split('_').map((w) => w[0].toUpperCase() + w.slice(1)).join(' ')
}

const EMPTY_CREATE = {
  enrollmentNumber: '', firstName: '', lastName: '', personalEmail: '', phone: '',
  dateOfBirth: '', gender: '', bloodGroup: '', aadharNumber: '', category: '',
  address: '', city: '', state: '', pincode: '', sectionId: '',
}

/**
 * Handles both create (needs the full CreateStudentRequest shape, incl. batch/section) and edit
 * (UpdateStudentRequest — a narrower field set; see StudentController's own doc comment on why).
 */
export function StudentForm({
  mode = 'create', // 'create' | 'edit'
  initial,
  batchLabel,
  sections = [],
  onSubmit,
  onCancel,
  submitLabel = 'Add Student',
}) {
  const [form, setForm] = useState(() => (mode === 'edit' ? { ...initial } : { ...EMPTY_CREATE }))
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
      <div className="grid max-h-[60vh] grid-cols-2 gap-4 overflow-y-auto py-2 pr-1">
        {mode === 'create' && (
          <>
            {field('enrollmentNumber', 'Enrollment Number', {
              placeholder: 'CS2026001',
              value: form.enrollmentNumber,
              onChange: (e) => set('enrollmentNumber', e.target.value.toUpperCase()),
              className: 'col-span-2',
            })}
          </>
        )}

        {field('firstName', 'First Name', {
          value: form.firstName,
          onChange: (e) => set('firstName', e.target.value),
        })}
        {field('lastName', 'Last Name', {
          value: form.lastName,
          onChange: (e) => set('lastName', e.target.value),
        })}

        {mode === 'create' && field('personalEmail', 'Personal Email', {
          type: 'email',
          value: form.personalEmail,
          onChange: (e) => set('personalEmail', e.target.value),
          className: 'col-span-2',
        })}

        {field('phone', 'Phone', {
          type: 'tel',
          value: form.phone,
          onChange: (e) => set('phone', e.target.value),
        })}
        {field('bloodGroup', 'Blood Group', {
          placeholder: 'O+',
          value: form.bloodGroup ?? '',
          onChange: (e) => set('bloodGroup', e.target.value),
        })}

        {mode === 'create' && (
          <>
            {field('dateOfBirth', 'Date of Birth', {
              type: 'date',
              value: form.dateOfBirth,
              onChange: (e) => set('dateOfBirth', e.target.value),
            })}

            <div className="flex flex-col gap-1">
              <Label className="text-sm font-medium">Gender</Label>
              <Select value={form.gender} onValueChange={(v) => set('gender', v)}>
                <SelectTrigger aria-invalid={!!errors.gender}><SelectValue placeholder="Select…" /></SelectTrigger>
                <SelectContent>
                  {GENDERS.map((g) => <SelectItem key={g} value={g}>{titleCase(g)}</SelectItem>)}
                </SelectContent>
              </Select>
              {errors.gender && <p className="text-xs text-destructive">{errors.gender}</p>}
            </div>

            <div className="flex flex-col gap-1">
              <Label className="text-sm font-medium">Category</Label>
              <Select value={form.category} onValueChange={(v) => set('category', v)}>
                <SelectTrigger><SelectValue placeholder="Select…" /></SelectTrigger>
                <SelectContent>
                  {CATEGORIES.map((c) => <SelectItem key={c} value={c}>{c}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>

            {field('aadharNumber', 'Aadhar Number (optional)', {
              placeholder: '12-digit number',
              value: form.aadharNumber,
              onChange: (e) => set('aadharNumber', e.target.value),
            })}
          </>
        )}

        {field('address', 'Address', {
          value: form.address ?? '',
          onChange: (e) => set('address', e.target.value),
          className: 'col-span-2',
        })}
        {field('city', 'City', { value: form.city ?? '', onChange: (e) => set('city', e.target.value) })}
        {field('state', 'State', { value: form.state ?? '', onChange: (e) => set('state', e.target.value) })}
        {field('pincode', 'Pincode', { value: form.pincode ?? '', onChange: (e) => set('pincode', e.target.value) })}

        {mode === 'create' && (
          <>
            <div className="col-span-2 flex flex-col gap-1">
              <Label className="text-sm font-medium">Batch</Label>
              <Input value={batchLabel ?? ''} disabled className="text-muted-foreground" />
            </div>
            <div className="col-span-2 flex flex-col gap-1">
              <Label className="text-sm font-medium">Section</Label>
              <Select value={form.sectionId} onValueChange={(v) => set('sectionId', v)}>
                <SelectTrigger aria-invalid={!!errors.sectionId}><SelectValue placeholder="Select a section…" /></SelectTrigger>
                <SelectContent>
                  {sections.map((s) => <SelectItem key={s.id} value={s.id}>{s.name}</SelectItem>)}
                </SelectContent>
              </Select>
              {errors.sectionId && <p className="text-xs text-destructive">{errors.sectionId}</p>}
            </div>
          </>
        )}
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
