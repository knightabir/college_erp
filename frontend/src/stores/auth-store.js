import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'

/**
 * Auth session state — the one piece of "server data" this app deliberately keeps in Zustand
 * rather than React Query, per the project convention (Zustand = UI/session state, TanStack
 * Query = server data). It's not really server *data* so much as the credential the rest of
 * the app's server-data fetching depends on.
 *
 * Persisted to localStorage so a page refresh doesn't force a re-login — the backend has no
 * httpOnly-cookie session of its own (JWTs are returned in the response body, see
 * 08-system-architecture.md §4), so this is the only place a refresh token can live client-side.
 *
 * `user.roles` comes from `LoginResponse.roles` at login time only — there is no `GET /me`
 * endpoint in this backend to re-fetch it later (see AuthController). This is fine for RBAC's
 * actual purpose here: hiding/showing UI. Every write the backend cares about is re-authorized
 * server-side on every request via `@PreAuthorize` regardless of what this store says (ADR-004 —
 * roles are deliberately not embedded in the JWT so a revocation takes effect immediately
 * server-side, independent of what this client-side cache still believes).
 */
export const useAuthStore = create()(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      refreshToken: null,

      /** Called after a successful login. */
      setSession({ user, accessToken, refreshToken }) {
        set({ user, accessToken, refreshToken })
      },

      /** Called after a successful silent refresh — user identity doesn't change. */
      setTokens({ accessToken, refreshToken }) {
        set({ accessToken, refreshToken })
      },

      /** Logout / session-expired — wipes everything. */
      clear() {
        set({ user: null, accessToken: null, refreshToken: null })
      },

      isAuthenticated() {
        const { user, accessToken } = get()
        return Boolean(user && accessToken)
      },
    }),
    {
      name: 'erp-auth',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        user: state.user,
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
      }),
    }
  )
)

/** Selector hook — prefer this over `useAuthStore((s) => s.user)` scattered everywhere. */
export function useCurrentUser() {
  return useAuthStore((s) => s.user)
}

export function useIsAuthenticated() {
  return useAuthStore((s) => Boolean(s.user && s.accessToken))
}
