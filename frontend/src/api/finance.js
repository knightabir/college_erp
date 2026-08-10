import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient, unwrap, idempotencyHeaders } from '@/lib/api-client'

/** 05-api-documentation.md §7 — module/finance (fee invoices/payments/structure slice only;
 *  ledger/procurement/financial-year-close are staff-only back-office tooling out of this page's scope). */

export const feesApi = {
  generateInvoice: async (body) => unwrap(await apiClient.post('/finance/fees/invoices/generate', body)),
  getStudentInvoices: async (studentId) => unwrap(await apiClient.get(`/finance/fees/students/${studentId}/invoices`)),
  getOutstanding: async (studentId) => unwrap(await apiClient.get(`/finance/fees/students/${studentId}/outstanding`)),
  /** Financial write endpoint — carries an Idempotency-Key so a retried click can't double-charge. */
  collectPayment: async (invoiceId, body) =>
    unwrap(await apiClient.post(`/finance/fees/invoices/${invoiceId}/collect`, body, idempotencyHeaders())),
  getPayments: async (invoiceId) => unwrap(await apiClient.get(`/finance/fees/invoices/${invoiceId}/payments`)),
}

export const feeHeadsApi = {
  list: async () => unwrap(await apiClient.get('/finance/fee-heads')),
  create: async (body) => unwrap(await apiClient.post('/finance/fee-heads', body)),
}

export const feeStructuresApi = {
  listByCourse: async (courseId) => unwrap(await apiClient.get(`/finance/fee-structures/course/${courseId}`)),
  create: async (body) => unwrap(await apiClient.post('/finance/fee-structures', body)),
}

export function useStudentInvoices(studentId) {
  return useQuery({
    queryKey: ['fees', 'invoices', studentId],
    queryFn: () => feesApi.getStudentInvoices(studentId),
    enabled: Boolean(studentId),
  })
}

export function useOutstandingInvoices(studentId) {
  return useQuery({
    queryKey: ['fees', 'outstanding', studentId],
    queryFn: () => feesApi.getOutstanding(studentId),
    enabled: Boolean(studentId),
  })
}

export function useGenerateInvoice() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: feesApi.generateInvoice,
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['fees', 'invoices', variables.studentId] })
      queryClient.invalidateQueries({ queryKey: ['fees', 'outstanding', variables.studentId] })
    },
    meta: { skipGlobalErrorToast: true },
  })
}

export function useCollectPayment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ invoiceId, ...body }) => feesApi.collectPayment(invoiceId, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['fees'] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useFeeHeads() {
  return useQuery({ queryKey: ['fee-heads'], queryFn: feeHeadsApi.list, staleTime: 5 * 60_000 })
}

export function useCreateFeeHead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: feeHeadsApi.create,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['fee-heads'] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useFeeStructures(courseId) {
  return useQuery({
    queryKey: ['fee-structures', courseId],
    queryFn: () => feeStructuresApi.listByCourse(courseId),
    enabled: Boolean(courseId),
  })
}

export function useCreateFeeStructure() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: feeStructuresApi.create,
    onSuccess: (_data, variables) => queryClient.invalidateQueries({ queryKey: ['fee-structures', variables.courseId] }),
    meta: { skipGlobalErrorToast: true },
  })
}
