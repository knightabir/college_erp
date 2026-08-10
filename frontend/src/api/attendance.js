import { useMutation, useQuery } from '@tanstack/react-query'
import { apiClient, unwrap } from '@/lib/api-client'

/** 05-api-documentation.md §5 — module/attendance. No "list sessions" endpoint exists; a
 *  session is opened, marked, and submitted in one flow (see AttendanceController). */

export const attendanceApi = {
  openSession: async (body) => unwrap(await apiClient.post('/attendance/sessions', body)),
  mark: async (sessionId, statusByStudentId) =>
    unwrap(await apiClient.post(`/attendance/sessions/${sessionId}/mark`, statusByStudentId)),
  submitSession: async (sessionId) => unwrap(await apiClient.post(`/attendance/sessions/${sessionId}/submit`)),
  getSummary: async (studentId, offeringId) =>
    unwrap(await apiClient.get(`/attendance/summary/student/${studentId}/offering/${offeringId}`)),
  getDefaulters: async (offeringId) => unwrap(await apiClient.get(`/attendance/defaulters/${offeringId}`)),
  applyCondonation: async (body) => unwrap(await apiClient.post('/attendance/condonation', body)),
  reviewCondonation: async (requestId, body) => unwrap(await apiClient.patch(`/attendance/condonation/${requestId}`, body)),
}

export function useOpenSession() {
  return useMutation({ mutationFn: attendanceApi.openSession, meta: { skipGlobalErrorToast: true } })
}

export function useMarkAttendance() {
  return useMutation({ mutationFn: ({ sessionId, marks }) => attendanceApi.mark(sessionId, marks), meta: { skipGlobalErrorToast: true } })
}

export function useSubmitSession() {
  return useMutation({ mutationFn: attendanceApi.submitSession, meta: { skipGlobalErrorToast: true } })
}

export function useDefaulters(offeringId) {
  return useQuery({
    queryKey: ['attendance', 'defaulters', offeringId],
    queryFn: () => attendanceApi.getDefaulters(offeringId),
    enabled: Boolean(offeringId),
  })
}

export function useAttendanceSummary(studentId, offeringId) {
  return useQuery({
    queryKey: ['attendance', 'summary', studentId, offeringId],
    queryFn: () => attendanceApi.getSummary(studentId, offeringId),
    enabled: Boolean(studentId && offeringId),
  })
}

export function useApplyCondonation() {
  return useMutation({ mutationFn: attendanceApi.applyCondonation, meta: { skipGlobalErrorToast: true } })
}

export function useReviewCondonation() {
  return useMutation({
    mutationFn: ({ requestId, ...body }) => attendanceApi.reviewCondonation(requestId, body),
    meta: { skipGlobalErrorToast: true },
  })
}
