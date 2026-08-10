import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'

/**
 * Renders the Department → Course → Batch → Term → Offering picker driven by
 * `useAcademicScope()` (see api/academic.js). `showOffering=false` stops at Term (e.g. a page
 * that only needs a section's roster, not one specific subject).
 */
export function AcademicScopeSelector({ scope, showTerm = true, showOffering = true }) {
  const {
    departmentId, courseId, batchId, termId, offeringId,
    selectDepartment, selectCourse, selectBatch, selectTerm, setOfferingId,
    departments, courses, batches, terms, offerings, loading,
  } = scope

  return (
    <div className="flex flex-wrap items-end gap-3 rounded-sm border border-border bg-card p-4">
      <Field label="Department" placeholder={loading.departmentsLoading ? 'Loading…' : 'Select department'}>
        <Select value={departmentId} onValueChange={selectDepartment}>
          <SelectTrigger className="h-8 text-sm"><SelectValue placeholder={loading.departmentsLoading ? 'Loading…' : 'Select department'} /></SelectTrigger>
          <SelectContent>
            {departments.map((d) => <SelectItem key={d.id} value={d.id}>{d.name}</SelectItem>)}
          </SelectContent>
        </Select>
      </Field>
      <Field label="Course">
        <Select value={courseId} onValueChange={selectCourse} isDisabled={!departmentId}>
          <SelectTrigger className="h-8 text-sm"><SelectValue placeholder={loading.coursesLoading ? 'Loading…' : 'Select course'} /></SelectTrigger>
          <SelectContent>
            {courses.map((c) => <SelectItem key={c.id} value={c.id}>{c.name}</SelectItem>)}
          </SelectContent>
        </Select>
      </Field>
      <Field label="Batch">
        <Select value={batchId} onValueChange={selectBatch} isDisabled={!courseId}>
          <SelectTrigger className="h-8 text-sm"><SelectValue placeholder={loading.batchesLoading ? 'Loading…' : 'Select batch'} /></SelectTrigger>
          <SelectContent>
            {batches.map((b) => <SelectItem key={b.id} value={b.id}>{b.label}</SelectItem>)}
          </SelectContent>
        </Select>
      </Field>
      {showTerm && (
        <Field label="Term">
          <Select value={termId} onValueChange={selectTerm} isDisabled={!batchId}>
            <SelectTrigger className="h-8 text-sm"><SelectValue placeholder={loading.termsLoading ? 'Loading…' : 'Select term'} /></SelectTrigger>
            <SelectContent>
              {terms.map((t) => <SelectItem key={t.id} value={t.id}>{t.name}</SelectItem>)}
            </SelectContent>
          </Select>
        </Field>
      )}
      {showTerm && showOffering && (
        <Field label="Subject">
          <Select value={offeringId} onValueChange={setOfferingId} isDisabled={!termId}>
            <SelectTrigger className="h-8 text-sm"><SelectValue placeholder={loading.offeringsLoading ? 'Loading…' : 'Select subject'} /></SelectTrigger>
            <SelectContent>
              {offerings.map((o) => <SelectItem key={o.id} value={o.id}>{o.subjectName}</SelectItem>)}
            </SelectContent>
          </Select>
        </Field>
      )}
    </div>
  )
}

function Field({ label, children }) {
  return (
    <div className="flex min-w-40 flex-col gap-1">
      <span className="text-xs font-medium text-muted-foreground">{label}</span>
      {children}
    </div>
  )
}
