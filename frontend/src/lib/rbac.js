/**
 * Frontend RBAC — mirrors `module.auth.domain.UserRole` and each controller's `@PreAuthorize`
 * role list (backend is the source of truth and re-checks every request regardless; this is
 * purely to keep the UI from offering actions a user's role can never actually perform).
 */
export const UserRole = Object.freeze({
  SUPER_ADMIN: 'SUPER_ADMIN',
  ADMIN: 'ADMIN',
  IT_ADMIN: 'IT_ADMIN',
  HOD: 'HOD',
  FACULTY: 'FACULTY',
  LAB_ASSISTANT: 'LAB_ASSISTANT',
  EXAM_CELL: 'EXAM_CELL',
  HEAD_ACCOUNTANT: 'HEAD_ACCOUNTANT',
  ACCOUNTANT: 'ACCOUNTANT',
  FEE_COLLECTION_CLERK: 'FEE_COLLECTION_CLERK',
  AUDITOR: 'AUDITOR',
  CHIEF_LIBRARIAN: 'CHIEF_LIBRARIAN',
  LIBRARY_ASSISTANT: 'LIBRARY_ASSISTANT',
  ASSET_MANAGER: 'ASSET_MANAGER',
  MAINTENANCE_STAFF: 'MAINTENANCE_STAFF',
  HR_MANAGER: 'HR_MANAGER',
  ADMISSIONS_OFFICER: 'ADMISSIONS_OFFICER',
  PLACEMENT_OFFICER: 'PLACEMENT_OFFICER',
  HOSTEL_WARDEN: 'HOSTEL_WARDEN',
  TRANSPORT_MANAGER: 'TRANSPORT_MANAGER',
  STUDENT: 'STUDENT',
  PARENT: 'PARENT',
  ALUMNI: 'ALUMNI',
})

/** Reusable role groups, named after what they can do — not what page they happen to appear on. */
export const RoleGroups = Object.freeze({
  /** RoleTemplate grants both of these an unconditional wildcard ("*") server-side. */
  SYSTEM_ADMINS: [UserRole.SUPER_ADMIN, UserRole.ADMIN],
  ACADEMIC_OFFICE: [UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.HOD],
  TEACHING_STAFF: [UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.HOD, UserRole.FACULTY],
  FINANCE_OFFICE: [UserRole.HEAD_ACCOUNTANT, UserRole.ACCOUNTANT],
  LIBRARY_STAFF: [UserRole.CHIEF_LIBRARIAN, UserRole.LIBRARY_ASSISTANT],
  HR_OFFICE: [UserRole.SUPER_ADMIN, UserRole.HR_MANAGER],
})

export function hasRole(user, role) {
  return Boolean(user?.roles?.includes(role))
}

/** `roles` empty/undefined means "any authenticated user" — matches backend's `isAuthenticated()`. */
export function hasAnyRole(user, roles) {
  if (!roles || roles.length === 0) return Boolean(user)
  return Boolean(user?.roles?.some((r) => roles.includes(r)))
}

/**
 * Which roles see each top-level nav item / may land on each route at all.
 * `null` = every authenticated user. Individual pages still gate create/edit/delete buttons
 * more precisely against the specific controller endpoint's role list (see each page).
 */
export const NAV_ACCESS = {
  dashboard: null,
  students: [
    UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.HOD, UserRole.FACULTY,
    UserRole.HEAD_ACCOUNTANT, UserRole.ACCOUNTANT, UserRole.EXAM_CELL,
  ],
  faculty: [UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.HR_MANAGER, UserRole.HOD],
  departments: [
    UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.HOD, UserRole.FACULTY,
    UserRole.HR_MANAGER, UserRole.EXAM_CELL, UserRole.HEAD_ACCOUNTANT, UserRole.ACCOUNTANT,
  ],
  courses: [
    UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.HOD, UserRole.FACULTY, UserRole.EXAM_CELL,
  ],
  attendance: [
    UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.HOD, UserRole.FACULTY,
    UserRole.LAB_ASSISTANT, UserRole.EXAM_CELL, UserRole.STUDENT,
  ],
  examinations: [
    UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.HOD, UserRole.EXAM_CELL,
    UserRole.FACULTY, UserRole.STUDENT,
  ],
  fees: [
    UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.HEAD_ACCOUNTANT, UserRole.ACCOUNTANT,
    UserRole.FEE_COLLECTION_CLERK, UserRole.AUDITOR, UserRole.STUDENT,
  ],
  library: null,
  hr: [UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.HR_MANAGER, UserRole.HOD],
  notices: null,
  settings: null,
}
