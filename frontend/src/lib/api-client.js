import axios from 'axios'
import { useAuthStore } from '@/stores/auth-store'
import { toAppError, AppError } from './api-error'
import { snakeToCamelDeep } from './case-convert'

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

/** The instance every `src/api/*.js` module calls through. */
export const apiClient = axios.create({ baseURL, timeout: 20000 })

// A separate, un-intercepted instance for the refresh call itself — it must never go through
// the request interceptor below (which would attach the *expiring* access token) or the
// response interceptor (which would otherwise try to refresh a failed refresh, looping forever).
const refreshClient = axios.create({ baseURL, timeout: 20000 })

let onAuthFailureHandler = null
/** Registered once, near the router root — see App.jsx. Called when a session can't be renewed. */
export function onAuthFailure(handler) {
  onAuthFailureHandler = handler
}

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let refreshInFlight = null

async function refreshSession() {
  const refreshToken = useAuthStore.getState().refreshToken
  if (!refreshToken) {
    throw new AppError({ status: 401, title: 'Session expired', message: 'Please sign in again.' })
  }
  const response = await refreshClient.post('/auth/refresh', { refreshToken })
  const tokens = snakeToCamelDeep(response.data?.data)
  useAuthStore.getState().setTokens(tokens)
  return tokens
}

apiClient.interceptors.response.use(
  // Every response body is snake_case on the wire (see case-convert.js) — convert it once,
  // here, so nothing downstream (unwrap, api-error, every src/api/*.js module) has to know that.
  (response) => {
    response.data = snakeToCamelDeep(response.data)
    return response
  },
  async (error) => {
    if (error.response) {
      error.response.data = snakeToCamelDeep(error.response.data)
    }

    const originalRequest = error.config
    const status = error.response?.status
    const isAuthEndpoint =
      originalRequest?.url?.includes('/auth/login') || originalRequest?.url?.includes('/auth/refresh')
    const hasRefreshToken = Boolean(useAuthStore.getState().refreshToken)

    // A 401 on any *other* request, while we still hold a refresh token: try one silent
    // refresh-and-retry before giving up. `_retry` stops this looping on a second 401.
    if (status === 401 && !isAuthEndpoint && hasRefreshToken && !originalRequest._retry) {
      originalRequest._retry = true
      try {
        refreshInFlight ??= refreshSession().finally(() => {
          refreshInFlight = null
        })
        const tokens = await refreshInFlight
        originalRequest.headers.Authorization = `Bearer ${tokens.accessToken}`
        return apiClient(originalRequest)
      } catch (refreshError) {
        useAuthStore.getState().clear()
        onAuthFailureHandler?.()
        return Promise.reject(toAppError(refreshError))
      }
    }

    // 401 with no refresh token to fall back on (or the refresh itself just failed) — the
    // session is unrecoverable. Clear it and let the app redirect to /login.
    if (status === 401 && !isAuthEndpoint && !hasRefreshToken) {
      useAuthStore.getState().clear()
      onAuthFailureHandler?.()
    }

    return Promise.reject(toAppError(error))
  }
)

/** Every backend success response is wrapped in `{ success, message, data, timestamp }`. */
export function unwrap(response) {
  return response?.data?.data
}

/**
 * Financial write endpoints require an `Idempotency-Key` header so a retried/double-submitted
 * request doesn't double-charge (05-api-documentation.md §1). Pass the result as request config.
 */
export function idempotencyHeaders() {
  return { headers: { 'Idempotency-Key': crypto.randomUUID() } }
}
