import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient, unwrap } from '@/lib/api-client'

/** 05-api-documentation.md §8 — module/library. */

export const catalogApi = {
  searchTitles: async ({ q, page = 0, size = 20 } = {}) =>
    unwrap(await apiClient.get('/library/catalog/titles', { params: { q: q || undefined, page, size } })),
  getTitle: async (id) => unwrap(await apiClient.get(`/library/catalog/titles/${id}`)),
  createTitle: async (body) => unwrap(await apiClient.post('/library/catalog/titles', body)),
  listCopies: async (titleId) => unwrap(await apiClient.get(`/library/catalog/titles/${titleId}/copies`)),
  addCopy: async (titleId, body) => unwrap(await apiClient.post(`/library/catalog/titles/${titleId}/copies`, body)),
  markLost: async (copyId) => unwrap(await apiClient.patch(`/library/catalog/copies/${copyId}/mark-lost`)),
  barcodeImageUrl: (copyId) => `${apiClient.defaults.baseURL}/library/catalog/copies/${copyId}/barcode-image`,
}

export const circulationApi = {
  issue: async (body) => unwrap(await apiClient.post('/library/circulation/issue', body)),
  returnBook: async (body) => unwrap(await apiClient.post('/library/circulation/return', body)),
  renew: async (body) => unwrap(await apiClient.post('/library/circulation/renew', body)),
  custodyChain: async (copyId) => unwrap(await apiClient.get(`/library/circulation/copies/${copyId}/custody-chain`)),
}

export const finesApi = {
  listByUser: async (userId) => unwrap(await apiClient.get(`/library/fines/user/${userId}`)),
  pay: async (id, amount) => unwrap(await apiClient.patch(`/library/fines/${id}/pay`, { amount })),
  waive: async (id, reason) => unwrap(await apiClient.patch(`/library/fines/${id}/waive`, { reason })),
}

export const reservationsApi = {
  join: async (titleId) => unwrap(await apiClient.post(`/library/reservations/titles/${titleId}`)),
  cancel: async (id) => unwrap(await apiClient.delete(`/library/reservations/${id}`)),
}

export function useSearchTitles(params) {
  return useQuery({ queryKey: ['library', 'titles', params], queryFn: () => catalogApi.searchTitles(params) })
}

export function useCreateTitle() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: catalogApi.createTitle,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['library', 'titles'] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useCopies(titleId) {
  return useQuery({
    queryKey: ['library', 'copies', titleId],
    queryFn: () => catalogApi.listCopies(titleId),
    enabled: Boolean(titleId),
  })
}

export function useAddCopy() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ titleId, ...body }) => catalogApi.addCopy(titleId, body),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['library', 'copies', variables.titleId] })
      queryClient.invalidateQueries({ queryKey: ['library', 'titles'] })
    },
    meta: { skipGlobalErrorToast: true },
  })
}

export function useMarkCopyLost() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: catalogApi.markLost,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['library'] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useIssueBook() {
  return useMutation({ mutationFn: circulationApi.issue, meta: { skipGlobalErrorToast: true } })
}

export function useReturnBook() {
  return useMutation({ mutationFn: circulationApi.returnBook, meta: { skipGlobalErrorToast: true } })
}

export function useRenewBook() {
  return useMutation({ mutationFn: circulationApi.renew, meta: { skipGlobalErrorToast: true } })
}

export function useFinesByUser(userId) {
  return useQuery({
    queryKey: ['library', 'fines', userId],
    queryFn: () => finesApi.listByUser(userId),
    enabled: Boolean(userId),
  })
}

export function usePayFine() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, amount }) => finesApi.pay(id, amount),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['library', 'fines'] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useWaiveFine() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, reason }) => finesApi.waive(id, reason),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['library', 'fines'] }),
    meta: { skipGlobalErrorToast: true },
  })
}

export function useJoinReservation() {
  return useMutation({ mutationFn: reservationsApi.join, meta: { skipGlobalErrorToast: true } })
}

export function useCancelReservation() {
  return useMutation({ mutationFn: reservationsApi.cancel, meta: { skipGlobalErrorToast: true } })
}
