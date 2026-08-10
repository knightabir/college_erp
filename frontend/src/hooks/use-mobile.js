import * as React from "react"

const MOBILE_BREAKPOINT = 768

function getIsMobile() {
  if (typeof window === "undefined") return false
  return window.innerWidth < MOBILE_BREAKPOINT
}

export function useIsMobile() {
  // Computed synchronously during render via a lazy initializer (safe — it only reads
  // window.innerWidth, it doesn't subscribe to anything) instead of calling setState
  // synchronously inside the effect body, which would otherwise force an extra render
  // right after mount. The effect below only handles subscribing to further changes.
  const [isMobile, setIsMobile] = React.useState(getIsMobile)

  React.useEffect(() => {
    const mql = window.matchMedia(`(max-width: ${MOBILE_BREAKPOINT - 1}px)`)
    const onChange = () => {
      setIsMobile(window.innerWidth < MOBILE_BREAKPOINT)
    }
    mql.addEventListener("change", onChange)
    return () => mql.removeEventListener("change", onChange)
  }, [])

  return isMobile
}
