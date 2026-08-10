import { apiClient, unwrap } from '@/lib/api-client'

/**
 * The one public, unauthenticated endpoint in this backend besides /auth/login — see
 * `tenant.controller.RegistrationController`. Deliberately not under `/auth`: this creates a
 * brand-new `Tenant` (college) plus its own first `SUPER_ADMIN` account in one call, and logs
 * that admin straight in (same access/refresh token shape `AuthService.login()` returns).
 */
export const tenantApi = {
  /** @returns {Promise<{tenant, adminUserId, accessToken, refreshToken, expiresIn}>} */
  async register(body) {
    const res = await apiClient.post('/register', body)
    return unwrap(res)
  },
}
