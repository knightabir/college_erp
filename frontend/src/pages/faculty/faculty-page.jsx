import { useState } from 'react'
import { GraduationCap } from 'lucide-react'
import { PageHeader } from '@/components/shared/page-header'
import { PageContainer } from '@/components/shared/page-container'
import { EmptyState } from '@/components/shared/empty-state'
import { TableLoadingState } from '@/components/shared/loading-state'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { useActiveDepartments } from '@/api/academic'
import { useStaffByDepartment } from '@/api/hr'

/**
 * Read-only teaching-staff directory — module/hr's StaffProfile is the only backing entity for
 * "faculty" (see CLAUDE.md: no dedicated Faculty schema exists). Staff profile creation lives on
 * the HR page, whose write endpoint is restricted to SUPER_ADMIN/HR_MANAGER — narrower than this
 * page's own viewer roles (also HOD/ADMIN), so this view is deliberately view-only.
 */
export default function FacultyPage() {
  const [departmentId, setDepartmentId] = useState('')
  const { data: departments } = useActiveDepartments()
  const { data: staff, isLoading } = useStaffByDepartment(departmentId)

  const faculty = (staff ?? []).filter((s) => s.staffType === 'TEACHING')

  return (
    <div>
      <PageHeader title="Faculty" description="Teaching staff directory." breadcrumb="Academic" />
      <PageContainer>
        <div className="flex min-w-52 flex-col gap-1">
          <span className="text-xs font-medium text-muted-foreground">Department</span>
          <Select value={departmentId} onValueChange={setDepartmentId}>
            <SelectTrigger className="h-8 text-sm"><SelectValue placeholder="Select department" /></SelectTrigger>
            <SelectContent>{(departments ?? []).map((d) => <SelectItem key={d.id} value={d.id}>{d.name}</SelectItem>)}</SelectContent>
          </Select>
        </div>

        {!departmentId ? (
          <EmptyState icon={GraduationCap} title="Select a department" description="Choose a department to view its faculty." />
        ) : isLoading ? (
          <TableLoadingState rows={4} cols={4} />
        ) : !faculty.length ? (
          <EmptyState icon={GraduationCap} title="No faculty found" description="No teaching staff profiles found for this department." />
        ) : (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {faculty.map((f) => (
              <div key={f.id} className="rounded-sm border border-border bg-card p-4">
                <p className="text-sm font-semibold text-foreground">{f.designationName ?? 'Faculty'}</p>
                <p className="mt-0.5 font-mono text-xs text-muted-foreground">{f.employeeNumber}</p>
                <div className="mt-2 flex flex-col gap-0.5 text-xs text-muted-foreground">
                  {f.specialization && <span>Specialization: {f.specialization}</span>}
                  {f.qualification && <span>Qualification: {f.qualification}</span>}
                  {f.experienceYears != null && <span>{f.experienceYears} yrs experience</span>}
                </div>
              </div>
            ))}
          </div>
        )}
      </PageContainer>
    </div>
  )
}
