import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient, unwrap } from '@/lib/api-client'

/** 05-api-documentation.md §3 — module/academic. */

// ── Raw API calls ────────────────────────────────────────────────────────

export const departmentsApi = {
  list: async ({ page = 0, size = 20 } = {}) => unwrap(await apiClient.get('/academic/departments', { params: { page, size } })),
  listActive: async () => unwrap(await apiClient.get('/academic/departments/active')),
  getById: async (id) => unwrap(await apiClient.get(`/academic/departments/${id}`)),
  create: async (body) => unwrap(await apiClient.post('/academic/departments', body)),
  update: async (id, body) => unwrap(await apiClient.put(`/academic/departments/${id}`, body)),
  deactivate: async (id) => unwrap(await apiClient.patch(`/academic/departments/${id}/deactivate`)),
}

export const coursesApi = {
  list: async ({ page = 0, size = 20 } = {}) => unwrap(await apiClient.get('/academic/courses', { params: { page, size } })),
  getById: async (id) => unwrap(await apiClient.get(`/academic/courses/${id}`)),
  listByDepartment: async (departmentId) => unwrap(await apiClient.get(`/academic/courses/by-department/${departmentId}`)),
  create: async (body) => unwrap(await apiClient.post('/academic/courses', body)),
}

export const batchesApi = {
  getById: async (id) => unwrap(await apiClient.get(`/academic/batches/${id}`)),
  listByCourse: async (courseId) => unwrap(await apiClient.get(`/academic/batches/by-course/${courseId}`)),
  create: async (body) => unwrap(await apiClient.post('/academic/batches', body)),
  promoteTerm: async (id) => unwrap(await apiClient.post(`/academic/batches/${id}/promote-term`)),
  listSections: async (id) => unwrap(await apiClient.get(`/academic/batches/${id}/sections`)),
  addSection: async (id, body) => unwrap(await apiClient.post(`/academic/batches/${id}/sections`, body)),
}

export const sectionsApi = {
  getById: async (id) => unwrap(await apiClient.get(`/academic/sections/${id}`)),
}

export const termsApi = {
  getById: async (id) => unwrap(await apiClient.get(`/academic/terms/${id}`)),
  listByAcademicYear: async (academicYearId) => unwrap(await apiClient.get(`/academic/terms/by-academic-year/${academicYearId}`)),
  create: async (body) => unwrap(await apiClient.post('/academic/terms', body)),
}

export const subjectsApi = {
  list: async ({ page = 0, size = 20 } = {}) => unwrap(await apiClient.get('/academic/subjects', { params: { page, size } })),
  listByDepartment: async (departmentId) => unwrap(await apiClient.get(`/academic/subjects/by-department/${departmentId}`)),
  create: async (body) => unwrap(await apiClient.post('/academic/subjects', body)),
  createOffering: async (body) => unwrap(await apiClient.post('/academic/subjects/offerings', body)),
  listOfferings: async (batchId, termId) =>
    unwrap(await apiClient.get('/academic/subjects/offerings', { params: { batchId, termId } })),
}

export const academicYearsApi = {
  list: async ({ page = 0, size = 50 } = {}) => unwrap(await apiClient.get('/academic/academic-years', { params: { page, size } })),
  create: async (body) => unwrap(await apiClient.post('/academic/academic-years', body)),
  setCurrent: async (id) => unwrap(await apiClient.patch(`/academic/academic-years/${id}/set-current`)),
}

// ── React Query hooks ────────────────────────────────────────────────────

export function useDepartments(params) {
  return useQuery({ queryKey: ['departments', params], queryFn: () => departmentsApi.list(params) })
}

export function useActiveDepartments() {
  return useQuery({ queryKey: ['departments', 'active'], queryFn: departmentsApi.listActive, staleTime: 5 * 60_000 })
}

export function useCreateDepartment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: departmentsApi.create,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['departments'] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useUpdateDepartment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, ...body }) => departmentsApi.update(id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['departments'] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useDeactivateDepartment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: departmentsApi.deactivate,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['departments'] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useCourses(params) {
  return useQuery({ queryKey: ['courses', params], queryFn: () => coursesApi.list(params) })
}

export function useCoursesByDepartment(departmentId) {
  return useQuery({
    queryKey: ['courses', 'by-department', departmentId],
    queryFn: () => coursesApi.listByDepartment(departmentId),
    enabled: Boolean(departmentId),
  })
}

export function useCreateCourse() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: coursesApi.create,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['courses'] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useBatchesByCourse(courseId) {
  return useQuery({
    queryKey: ['batches', 'by-course', courseId],
    queryFn: () => batchesApi.listByCourse(courseId),
    enabled: Boolean(courseId),
  })
}

export function useCreateBatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: batchesApi.create,
    onSuccess: (_data, variables) =>
      queryClient.invalidateQueries({ queryKey: ['batches', 'by-course', variables.courseId] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useSectionsByBatch(batchId) {
  return useQuery({
    queryKey: ['sections', 'by-batch', batchId],
    queryFn: () => batchesApi.listSections(batchId),
    enabled: Boolean(batchId),
  })
}

export function useCreateSection() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ batchId, ...body }) => batchesApi.addSection(batchId, body),
    onSuccess: (_data, variables) =>
      queryClient.invalidateQueries({ queryKey: ['sections', 'by-batch', variables.batchId] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useAcademicYears(params) {
  return useQuery({ queryKey: ['academic-years', params], queryFn: () => academicYearsApi.list(params), staleTime: 5 * 60_000 })
}

export function useCreateAcademicYear() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: academicYearsApi.create,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['academic-years'] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useTermsByAcademicYear(academicYearId) {
  return useQuery({
    queryKey: ['terms', 'by-academic-year', academicYearId],
    queryFn: () => termsApi.listByAcademicYear(academicYearId),
    enabled: Boolean(academicYearId),
  })
}

export function useSubjectOfferings(batchId, termId) {
  return useQuery({
    queryKey: ['subject-offerings', batchId, termId],
    queryFn: () => subjectsApi.listOfferings(batchId, termId),
    enabled: Boolean(batchId && termId),
  })
}

/**
 * Shared Department → Course → Batch → Term → Offering cascade — every operations module
 * (attendance, examinations) scopes its data through a subject offering, which can only be
 * reached by walking down this chain (there's no flat "list offerings" endpoint).
 */
export function useAcademicScope() {
  const [departmentId, setDepartmentId] = useState('')
  const [courseId, setCourseId] = useState('')
  const [batchId, setBatchId] = useState('')
  const [termId, setTermId] = useState('')
  const [offeringId, setOfferingId] = useState('')

  const { data: departments, isLoading: departmentsLoading } = useActiveDepartments()
  const { data: courses, isLoading: coursesLoading } = useCoursesByDepartment(departmentId)
  const { data: batches, isLoading: batchesLoading } = useBatchesByCourse(courseId)
  const selectedBatch = batches?.find((b) => b.id === batchId)
  const { data: terms, isLoading: termsLoading } = useTermsByAcademicYear(selectedBatch?.academicYearId)
  const { data: offerings, isLoading: offeringsLoading } = useSubjectOfferings(batchId, termId)
  const { data: sections } = useSectionsByBatch(batchId)

  function selectDepartment(v) { setDepartmentId(v); setCourseId(''); setBatchId(''); setTermId(''); setOfferingId('') }
  function selectCourse(v) { setCourseId(v); setBatchId(''); setTermId(''); setOfferingId('') }
  function selectBatch(v) { setBatchId(v); setTermId(''); setOfferingId('') }
  function selectTerm(v) { setTermId(v); setOfferingId('') }

  return {
    departmentId, courseId, batchId, termId, offeringId,
    selectDepartment, selectCourse, selectBatch, selectTerm, setOfferingId,
    departments: departments ?? [], courses: courses ?? [], batches: batches ?? [],
    terms: terms ?? [], offerings: offerings ?? [], sections: sections ?? [],
    selectedBatch,
    loading: { departmentsLoading, coursesLoading, batchesLoading, termsLoading, offeringsLoading },
  }
}
