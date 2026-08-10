import {
  Building2, BookOpen, Bell, CalendarClock, Pin,
} from 'lucide-react'
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts'
import { PageHeader } from '@/components/shared/page-header'
import { PageContainer } from '@/components/shared/page-container'
import { CardLoadingState } from '@/components/shared/loading-state'
import { EmptyState } from '@/components/shared/empty-state'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { useCurrentUser } from '@/stores/auth-store'
import { useActiveDepartments, useCourses, useAcademicYears } from '@/api/academic'
import { useNotices } from '@/api/communication'

function MetricCard({ label, value, icon: Icon, color, bg, loading }) {
  return (
    <Card className="rounded-sm shadow-none">
      <CardContent className="p-5">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{label}</p>
            {loading ? (
              <div className="mt-2 h-8 w-16 animate-pulse rounded-sm bg-muted" />
            ) : (
              <p className="mt-2 text-3xl font-semibold text-foreground">{value}</p>
            )}
          </div>
          <div className={`rounded-sm p-2.5 ${bg}`}>
            <Icon className={`size-5 ${color}`} />
          </div>
        </div>
      </CardContent>
    </Card>
  )
}

function CoursesByDepartmentChart({ courses }) {
  const grouped = Object.values(
    (courses ?? []).reduce((acc, c) => {
      const key = c.departmentName ?? 'Unassigned'
      acc[key] ??= { dept: key, courses: 0 }
      acc[key].courses += 1
      return acc
    }, {})
  )

  return (
    <Card className="rounded-sm shadow-none">
      <CardHeader>
        <CardTitle className="text-sm font-semibold">Courses by Department</CardTitle>
        <CardDescription className="text-xs">Active course offerings across departments</CardDescription>
      </CardHeader>
      <CardContent className="pb-4">
        {grouped.length === 0 ? (
          <EmptyState title="No courses yet" className="py-10" />
        ) : (
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={grouped} margin={{ top: 4, right: 4, bottom: 0, left: -20 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
              <XAxis dataKey="dept" tick={{ fontSize: 11, fill: 'var(--muted-foreground)' }} interval={0} angle={-15} textAnchor="end" height={50} />
              <YAxis tick={{ fontSize: 11, fill: 'var(--muted-foreground)' }} allowDecimals={false} />
              <Tooltip contentStyle={{ background: 'var(--card)', border: '1px solid var(--border)', borderRadius: '4px', fontSize: 12 }} />
              <Bar dataKey="courses" name="Courses" fill="#0F766E" radius={[2, 2, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </CardContent>
    </Card>
  )
}

function RecentNotices({ notices, isLoading }) {
  return (
    <Card className="rounded-sm shadow-none">
      <CardHeader>
        <CardTitle className="text-sm font-semibold">Recent Notices</CardTitle>
        <CardDescription className="text-xs">Latest announcements relevant to you</CardDescription>
      </CardHeader>
      <CardContent className="p-0">
        {isLoading ? (
          <div className="space-y-2 px-5 py-3">
            {Array.from({ length: 3 }).map((_, i) => <div key={i} className="h-10 animate-pulse rounded-sm bg-muted" />)}
          </div>
        ) : notices?.length ? (
          <ul>
            {notices.slice(0, 6).map((n, idx) => (
              <li key={n.id}>
                <div className="flex items-start gap-3 px-5 py-3">
                  {n.pinned && <Pin className="mt-0.5 size-3.5 shrink-0 text-primary" />}
                  <div className="flex-1">
                    <p className="text-sm font-medium text-foreground">{n.title}</p>
                    <p className="line-clamp-1 text-xs text-muted-foreground">{n.body}</p>
                  </div>
                  <span className="shrink-0 text-xs text-muted-foreground">
                    {n.publishedAt ? new Date(n.publishedAt).toLocaleDateString() : ''}
                  </span>
                </div>
                {idx < Math.min(notices.length, 6) - 1 && <Separator />}
              </li>
            ))}
          </ul>
        ) : (
          <EmptyState icon={Bell} title="No notices" description="Nothing has been published yet." className="py-10" />
        )}
      </CardContent>
    </Card>
  )
}

export default function DashboardPage() {
  const user = useCurrentUser()
  const now = new Date()
  const greeting = now.getHours() < 12 ? 'Good morning' : now.getHours() < 17 ? 'Good afternoon' : 'Good evening'

  const { data: departments, isLoading: deptLoading } = useActiveDepartments()
  const { data: coursesPage, isLoading: coursesLoading } = useCourses({ size: 200 })
  const { data: yearsPage, isLoading: yearsLoading } = useAcademicYears({ size: 50 })
  const { data: notices, isLoading: noticesLoading } = useNotices()

  const courses = coursesPage?.content ?? []
  const currentYear = (yearsPage?.content ?? []).find((y) => y.current)

  return (
    <div>
      <PageHeader
        title="Dashboard"
        description={`${greeting}${user?.fullName ? `, ${user.fullName.split(' ')[0]}` : ''}. Here's an overview of your institution.`}
      />
      <PageContainer>
        {deptLoading && coursesLoading ? (
          <CardLoadingState count={4} />
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <MetricCard
              label="Departments" value={departments?.length ?? 0} icon={Building2}
              color="text-brand-600" bg="bg-brand-50 dark:bg-brand-900/20" loading={deptLoading}
            />
            <MetricCard
              label="Courses" value={coursesPage?.totalElements ?? 0} icon={BookOpen}
              color="text-academic-700" bg="bg-academic-50 dark:bg-academic-900/20" loading={coursesLoading}
            />
            <MetricCard
              label="Current Academic Year" value={currentYear?.label ?? '—'} icon={CalendarClock}
              color="text-neutral-600" bg="bg-neutral-100 dark:bg-neutral-800/30" loading={yearsLoading}
            />
            <MetricCard
              label="Active Notices" value={notices?.length ?? 0} icon={Bell}
              color="text-amber-600" bg="bg-amber-50 dark:bg-amber-900/20" loading={noticesLoading}
            />
          </div>
        )}

        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          <CoursesByDepartmentChart courses={courses} />
          <RecentNotices notices={notices} isLoading={noticesLoading} />
        </div>
      </PageContainer>
    </div>
  )
}
