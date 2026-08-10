import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient, unwrap } from '@/lib/api-client'

/** 05-api-documentation.md §11 — module/communication (notices half; helpdesk not yet wired). */

export const noticesApi = {
  /** departmentId/batchId are optional scoping filters — see NoticeService.getRelevant() javadoc. */
  list: async ({ departmentId, batchId } = {}) =>
    unwrap(await apiClient.get('/notices', { params: { departmentId, batchId } })),
  create: async (body) => unwrap(await apiClient.post('/notices', body)),
}

export function useNotices(params) {
  return useQuery({ queryKey: ['notices', params], queryFn: () => noticesApi.list(params) })
}

export function useCreateNotice() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: noticesApi.create,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notices'] }),
    meta: { skipGlobalErrorToast: true },
  })
}
