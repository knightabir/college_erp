import { QueryClient, QueryCache, MutationCache } from '@tanstack/react-query'
import { toast } from 'sonner'
import { getErrorMessage, toAppError } from './api-error'

/**
 * Central React Query client. Server data lives here (never in Zustand — see CLAUDE.md's
 * JS/React conventions).
 *
 * Error display policy:
 *  - Queries (GET) never toast — a failed fetch renders inline via <ErrorState onRetry=.../>,
 *    which is more useful than a toast that's gone before the user can act on it.
 *  - Mutations (POST/PUT/PATCH/DELETE) toast by default, since most fire from a menu item /
 *    button with no dedicated place to render the failure. Forms and confirm-dialogs that
 *    already show the error inline (see StudentForm/ConfirmDialog) set
 *    `meta: { skipGlobalErrorToast: true }` on their `useMutation` call to avoid double-reporting
 *    the same failure once inline and once as a toast.
 *  - A 401 that survives the api-client's silent-refresh attempt means the session is gone;
 *    that's surfaced once via `onAuthFailure` (see api-client.js), not per-query here.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => {
        const appError = toAppError(error)
        if (appError.status === 401 || appError.status === 403 || appError.status === 404) return false
        return failureCount < 1
      },
      staleTime: 30_000,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: false,
    },
  },
  queryCache: new QueryCache(),
  mutationCache: new MutationCache({
    onError: (error, _variables, _context, mutation) => {
      if (mutation.meta?.skipGlobalErrorToast) return
      const appError = toAppError(error)
      if (appError.isAuthError) return // handled by the session-expired flow instead
      toast.error(appError.title, { description: getErrorMessage(error) })
    },
  }),
})
