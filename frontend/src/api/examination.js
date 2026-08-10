import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient, unwrap } from '@/lib/api-client'

/** 05-api-documentation.md §6 — module/examination. */

export const examsApi = {
  schedule: async (body) => unwrap(await apiClient.post('/exams', body)),
  getById: async (id) => unwrap(await apiClient.get(`/exams/${id}`)),
  listByBatch: async (batchId) => unwrap(await apiClient.get(`/exams/by-batch/${batchId}`)),
  generateHallTickets: async (examId) => unwrap(await apiClient.post(`/exams/${examId}/hall-tickets/generate`)),
  getHallTicket: async (examId, studentId) => unwrap(await apiClient.get(`/exams/${examId}/hall-tickets/${studentId}`)),
  publishResults: async (examId) => unwrap(await apiClient.post(`/exams/${examId}/results/publish`)),
}

export const marksApi = {
  enter: async (body) => unwrap(await apiClient.post('/exams/marks', body)),
  submit: async (marksEntryId) => unwrap(await apiClient.patch(`/exams/marks/${marksEntryId}/submit`)),
  moderate: async (examId) => unwrap(await apiClient.patch(`/exams/marks/${examId}/moderate`)),
}

export const resultsApi = {
  getResult: async (studentId, termId) => unwrap(await apiClient.get(`/exams/results/student/${studentId}/term/${termId}`)),
  getGradeCard: async (studentId, termId) => unwrap(await apiClient.get(`/exams/grade-card/student/${studentId}/term/${termId}`)),
}

export function useExamsByBatch(batchId) {
  return useQuery({
    queryKey: ['exams', 'by-batch', batchId],
    queryFn: () => examsApi.listByBatch(batchId),
    enabled: Boolean(batchId),
  })
}

export function useScheduleExam() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: examsApi.schedule,
    onSuccess: (_data, variables) => queryClient.invalidateQueries({ queryKey: ['exams', 'by-batch', variables.batchId] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useGenerateHallTickets() {
  return useMutation({ mutationFn: examsApi.generateHallTickets, meta: { skipGlobalErrorToast: true } })
}

export function usePublishResults() {
  return useMutation({ mutationFn: examsApi.publishResults, meta: { skipGlobalErrorToast: true } })
}

export function useEnterMarks() {
  return useMutation({ mutationFn: marksApi.enter, meta: { skipGlobalErrorToast: true } })
}

export function useSubmitMarks() {
  return useMutation({ mutationFn: marksApi.submit, meta: { skipGlobalErrorToast: true } })
}

export function useStudentResult(studentId, termId) {
  return useQuery({
    queryKey: ['exams', 'result', studentId, termId],
    queryFn: () => resultsApi.getResult(studentId, termId),
    enabled: Boolean(studentId && termId),
    retry: false,
  })
}

export function useHallTicket(examId, studentId) {
  return useQuery({
    queryKey: ['exams', 'hall-ticket', examId, studentId],
    queryFn: () => examsApi.getHallTicket(examId, studentId),
    enabled: Boolean(examId && studentId),
    retry: false,
  })
}
