import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient, unwrap } from '@/lib/api-client'

/** 05-api-documentation.md §10 — module/hr. */

export const staffApi = {
  createDesignation: async (body) => unwrap(await apiClient.post('/hr/staff/designations', body)),
  listDesignations: async () => unwrap(await apiClient.get('/hr/staff/designations')),
  create: async (body) => unwrap(await apiClient.post('/hr/staff', body)),
  getById: async (id) => unwrap(await apiClient.get(`/hr/staff/${id}`)),
  listByDepartment: async (departmentId) => unwrap(await apiClient.get(`/hr/staff/by-department/${departmentId}`)),
}

export const leaveApi = {
  createType: async (body) => unwrap(await apiClient.post('/hr/leave/types', body)),
  listTypes: async () => unwrap(await apiClient.get('/hr/leave/types')),
  initializeBalances: async (year) => unwrap(await apiClient.post('/hr/leave/balances/initialize', null, { params: { year } })),
  myBalances: async (year) => unwrap(await apiClient.get('/hr/leave/balances', { params: { year } })),
  apply: async (body) => unwrap(await apiClient.post('/hr/leave/apply', body)),
  myRequests: async () => unwrap(await apiClient.get('/hr/leave/my-requests')),
  pending: async () => unwrap(await apiClient.get('/hr/leave/pending')),
  approve: async (requestId, remarks) => unwrap(await apiClient.patch(`/hr/leave/${requestId}/approve`, { remarks })),
  reject: async (requestId, remarks) => unwrap(await apiClient.patch(`/hr/leave/${requestId}/reject`, { remarks })),
  cancel: async (requestId) => unwrap(await apiClient.patch(`/hr/leave/${requestId}/cancel`)),
}

export function useDesignations() {
  return useQuery({ queryKey: ['hr', 'designations'], queryFn: staffApi.listDesignations, staleTime: 5 * 60_000 })
}

export function useCreateDesignation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: staffApi.createDesignation,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['hr', 'designations'] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useStaffByDepartment(departmentId) {
  return useQuery({
    queryKey: ['hr', 'staff', 'by-department', departmentId],
    queryFn: () => staffApi.listByDepartment(departmentId),
    enabled: Boolean(departmentId),
  })
}

export function useCreateStaff() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: staffApi.create,
    onSuccess: (_data, variables) => queryClient.invalidateQueries({ queryKey: ['hr', 'staff', 'by-department', variables.departmentId] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useLeaveTypes() {
  return useQuery({ queryKey: ['hr', 'leave-types'], queryFn: leaveApi.listTypes, staleTime: 5 * 60_000 })
}

export function useCreateLeaveType() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: leaveApi.createType,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['hr', 'leave-types'] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useMyLeaveBalances(year) {
  return useQuery({ queryKey: ['hr', 'leave-balances', year], queryFn: () => leaveApi.myBalances(year) })
}

export function useMyLeaveRequests() {
  return useQuery({ queryKey: ['hr', 'leave-requests', 'mine'], queryFn: leaveApi.myRequests })
}

export function usePendingLeaveRequests() {
  return useQuery({ queryKey: ['hr', 'leave-requests', 'pending'], queryFn: leaveApi.pending })
}

export function useApplyLeave() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: leaveApi.apply,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['hr', 'leave-requests'] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useReviewLeaveRequest() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ requestId, decision, remarks }) =>
      decision === 'APPROVED' ? leaveApi.approve(requestId, remarks) : leaveApi.reject(requestId, remarks),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['hr', 'leave-requests'] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useCancelLeaveRequest() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: leaveApi.cancel,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['hr', 'leave-requests'] }),
    meta: { skipGlobalErrorToast: true },
  })
}
