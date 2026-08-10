/**
 * The backend locks every JSON response to snake_case (`enrollment_number`, `total_elements`,
 * `request_id`, ...) while accepting either snake_case or camelCase on the way in — see the
 * backend's `config/JacksonConfig.java` javadoc ("All JSON on the wire is snake_case... Only
 * serialization (responses) is locked to snake_case; deserialization accepts both").
 *
 * Every `src/api/*.js` module and page in this app is written against camelCase (matching the
 * Java DTO field names, e.g. `student.enrollmentNumber`), so every response body is converted
 * once, centrally, in `api-client.js` — nothing downstream (unwrap, api-error, individual pages)
 * needs to know the wire format is different from the JS object shape it works with.
 *
 * There is exactly one response shape in this backend that is a real `Map` keyed by something
 * other than a property name (`Map<UUID, AttendanceStatus>`) — and that's a *request* body, never
 * a response, so it's never passed through this converter. If a future endpoint ever returns a
 * raw map keyed by UUID/id, exclude that response from conversion rather than applying this
 * blindly — see this file's own git history if that need ever comes up.
 */

function snakeToCamelKey(key) {
  return key.replace(/_([a-z0-9])/g, (_, char) => char.toUpperCase())
}

/** Recursively converts every object key in `value` from snake_case to camelCase. */
export function snakeToCamelDeep(value) {
  if (Array.isArray(value)) {
    return value.map(snakeToCamelDeep)
  }
  if (value !== null && typeof value === 'object' && !(value instanceof Date)) {
    return Object.fromEntries(
      Object.entries(value).map(([key, val]) => [snakeToCamelKey(key), snakeToCamelDeep(val)])
    )
  }
  return value
}
