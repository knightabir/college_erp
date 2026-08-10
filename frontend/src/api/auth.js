import { apiClient, unwrap } from '@/lib/api-client'

/**
 * 05-api-documentation.md §2 / AuthController. `login`/`refresh` are unauthenticated;
 * `logout`/`changePassword` require the caller to already hold a valid access token (attached
 * automatically by the api-client request interceptor).
 */
export const authApi = {
  /** @returns {Promise<import('@/stores/auth-store').AuthUser & {accessToken, refreshToken, expiresIn}>} */
  async login({ identifier, password }) {
    const res = await apiClient.post('/auth/login', { identifier, password })
    return unwrap(res)
  },

  async refresh(refreshToken) {
    const res = await apiClient.post('/auth/refresh', { refreshToken })
    return unwrap(res)
  },

  async logout(refreshToken) {
    await apiClient.post('/auth/logout', { refreshToken })
  },

  async changePassword({ currentPassword, newPassword }) {
    await apiClient.post('/auth/change-password', { currentPassword, newPassword })
  },
}
