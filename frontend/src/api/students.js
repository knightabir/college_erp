import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient, unwrap } from '@/lib/api-client'

/**
 * 05-api-documentation.md §4 — module/student. There is deliberately no "list all students"
 * endpoint (see StudentController) — students are always listed scoped to a batch or a section,
 * matching how `EnrollmentService` actually models enrollment. The Students page picks a batch
 * (via course/department) first, then lists within it.
 */

export const studentsApi = {
  getOwnProfile: async () => unwrap(await apiClient.get('/students/me')),
  getById: async (id) => unwrap(await apiClient.get(`/students/${id}`)),
  getByEnrollmentNumber: async (number) => unwrap(await apiClient.get(`/students/enrollment/${number}`)),
  listByBatch: async (batchId, { page = 0, size = 20 } = {}) =>
    unwrap(await apiClient.get(`/students/by-batch/${batchId}`, { params: { page, size } })),
  listBySection: async (sectionId, { page = 0, size = 20 } = {}) =>
    unwrap(await apiClient.get(`/students/by-section/${sectionId}`, { params: { page, size } })),
  create: async (body) => unwrap(await apiClient.post('/students', body)),
  update: async (id, body) => unwrap(await apiClient.put(`/students/${id}`, body)),
  graduate: async (id) => unwrap(await apiClient.patch(`/students/${id}/graduate`)),
  listGuardians: async (id) => unwrap(await apiClient.get(`/students/${id}/guardians`)),
  addGuardian: async (id, body) => unwrap(await apiClient.post(`/students/${id}/guardians`, body)),
}

export function useStudentsByBatch(batchId, params) {
  return useQuery({
    queryKey: ['students', 'by-batch', batchId, params],
    queryFn: () => studentsApi.listByBatch(batchId, params),
    enabled: Boolean(batchId),
  })
}

export function useOwnStudentProfile(enabled = true) {
  return useQuery({
    queryKey: ['students', 'me'],
    queryFn: studentsApi.getOwnProfile,
    enabled,
    retry: false,
  })
}

export function useStudent(id) {
  return useQuery({
    queryKey: ['students', id],
    queryFn: () => studentsApi.getById(id),
    enabled: Boolean(id),
  })
}

export function useCreateStudent() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: studentsApi.create,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['students'] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useUpdateStudent() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, ...body }) => studentsApi.update(id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['students'] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useGraduateStudent() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: studentsApi.graduate,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['students'] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useGuardians(studentId) {
  return useQuery({
    queryKey: ['guardians', studentId],
    queryFn: () => studentsApi.listGuardians(studentId),
    enabled: Boolean(studentId),
  })
}

export function useAddGuardian() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ studentId, ...body }) => studentsApi.addGuardian(studentId, body),
    onSuccess: (_data, variables) => queryClient.invalidateQueries({ queryKey: ['guardians', variables.studentId] }),
    meta: { skipGlobalErrorToast: true },
  })
}
