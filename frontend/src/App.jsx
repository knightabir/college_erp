import { Routes, Route } from 'react-router-dom'
import { AppLayout } from '@/components/layout/app-layout'
import { ProtectedRoute } from '@/components/auth/protected-route'
import { RequireRole } from '@/components/auth/require-role'
import { SessionGuard } from '@/components/auth/session-guard'
import { NAV_ACCESS } from '@/lib/rbac'
import LoginPage from '@/pages/auth/login-page'
import RegisterPage from '@/pages/auth/register-page'
import ForbiddenPage from '@/pages/errors/forbidden-page'
import NotFoundPage from '@/pages/errors/not-found-page'
import DashboardPage from '@/pages/dashboard/dashboard-page'
import StudentsPage from '@/pages/students/students-page'
import FacultyPage from '@/pages/faculty/faculty-page'
import DepartmentsPage from '@/pages/departments/departments-page'
import CoursesPage from '@/pages/courses/courses-page'
import AttendancePage from '@/pages/attendance/attendance-page'
import ExaminationsPage from '@/pages/examinations/examinations-page'
import FeesPage from '@/pages/fees/fees-page'
import LibraryPage from '@/pages/library/library-page'
import HrPage from '@/pages/hr/hr-page'
import NoticesPage from '@/pages/notices/notices-page'
import SettingsPage from '@/pages/settings/settings-page'

export default function App() {
  return (
    <>
      <SessionGuard />
      <Routes>
        {/* Public */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {/* Authenticated shell */}
        <Route element={<ProtectedRoute />}>
          <Route path="/403" element={<ForbiddenPage />} />

          <Route element={<AppLayout />}>
            <Route path="/" element={<DashboardPage />} />

            <Route element={<RequireRole roles={NAV_ACCESS.students} />}>
              <Route path="/students" element={<StudentsPage />} />
            </Route>
            <Route element={<RequireRole roles={NAV_ACCESS.faculty} />}>
              <Route path="/faculty" element={<FacultyPage />} />
            </Route>
            <Route element={<RequireRole roles={NAV_ACCESS.departments} />}>
              <Route path="/departments" element={<DepartmentsPage />} />
            </Route>
            <Route element={<RequireRole roles={NAV_ACCESS.courses} />}>
              <Route path="/courses" element={<CoursesPage />} />
            </Route>
            <Route element={<RequireRole roles={NAV_ACCESS.attendance} />}>
              <Route path="/attendance" element={<AttendancePage />} />
            </Route>
            <Route element={<RequireRole roles={NAV_ACCESS.examinations} />}>
              <Route path="/examinations" element={<ExaminationsPage />} />
            </Route>
            <Route element={<RequireRole roles={NAV_ACCESS.fees} />}>
              <Route path="/fees" element={<FeesPage />} />
            </Route>
            <Route element={<RequireRole roles={NAV_ACCESS.library} />}>
              <Route path="/library" element={<LibraryPage />} />
            </Route>
            <Route element={<RequireRole roles={NAV_ACCESS.hr} />}>
              <Route path="/hr" element={<HrPage />} />
            </Route>
            <Route path="/notices" element={<NoticesPage />} />
            <Route path="/settings" element={<SettingsPage />} />
          </Route>
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </>
  )
}
