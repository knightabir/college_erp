/**
 * Normalizes every failure this app can hit — a Spring RFC 7807 `ErrorResponse` (see
 * backend `web/GlobalExceptionHandler.java` + 05-api-documentation.md §1.4), a raw network
 * failure, or a request that never got a response — into one predictable shape.
 *
 * Nothing in this app should read `error.response.data.detail` or `error.message` (an axios
 * internal string) directly — always go through `toAppError()` first, so a raw
 * "Network Error"/axios stack never reaches a toast or a form.
 */
export class AppError extends Error {
  constructor({ status, title, message, fieldErrors, requestId, type, isNetworkError = false }) {
    super(message)
    this.name = 'AppError'
    this.status = status ?? 0
    this.title = title ?? 'Something went wrong'
    this.fieldErrors = fieldErrors ?? null
    this.requestId = requestId ?? null
    this.type = type ?? 'unknown'
    this.isNetworkError = isNetworkError
  }

  /** True for 401 Unauthorized (used to short-circuit the refresh flow / redirect to login). */
  get isAuthError() {
    return this.status === 401
  }

  get isForbidden() {
    return this.status === 403
  }

  get isNotFound() {
    return this.status === 404
  }

  get isValidation() {
    return this.status === 400 && !!this.fieldErrors
  }
}

/**
 * @param {unknown} error — whatever axios rejected with
 * @returns {AppError}
 */
export function toAppError(error) {
  if (error instanceof AppError) return error

  // axios timeout / DNS failure / CORS / server unreachable — no `error.response` at all.
  if (error?.request && !error?.response) {
    const timedOut = error.code === 'ECONNABORTED'
    return new AppError({
      status: 0,
      title: 'Connection problem',
      message: timedOut
        ? 'The server took too long to respond. Please try again.'
        : 'Unable to reach the server. Check your connection and try again.',
      isNetworkError: true,
    })
  }

  const body = error?.response?.data
  if (body && typeof body === 'object') {
    return new AppError({
      status: error.response.status,
      title: body.title ?? 'Request failed',
      message: body.detail ?? 'Something went wrong. Please try again.',
      fieldErrors: body.fieldErrors ?? null,
      requestId: body.requestId ?? null,
      type: body.type ?? 'unknown',
    })
  }

  // Something threw before axios even ran (programmer error, bad JSON, etc.)
  return new AppError({
    status: 0,
    title: 'Unexpected error',
    message: 'An unexpected error occurred. Please try again.',
  })
}

/** Convenience for anywhere that just wants a string to render (toast, ErrorState, ...). */
export function getErrorMessage(error) {
  return toAppError(error).message
}

/**
 * Flattens `{ email: ["Enter a valid email"], phone: [...] }` into `{ email: "Enter a valid email" }`
 * for simple field-level display (forms in this app show one message per field).
 */
export function getFieldErrors(error) {
  const appError = toAppError(error)
  if (!appError.fieldErrors) return {}
  return Object.fromEntries(
    Object.entries(appError.fieldErrors).map(([field, messages]) => [field, messages[0]])
  )
}
