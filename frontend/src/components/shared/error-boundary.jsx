import { Component } from 'react'
import { AlertTriangle } from 'lucide-react'
import { Button } from '@/components/ui/button'

/**
 * Last-resort catch for render-time errors React Query/try-catch can't cover (a bad prop shape,
 * a third-party chart library throwing, etc.) — shows a friendly fallback instead of a blank
 * white screen in production.
 */
export class ErrorBoundary extends Component {
  state = { hasError: false }

  static getDerivedStateFromError() {
    return { hasError: true }
  }

  componentDidCatch(error, info) {
    console.error('Unhandled UI error:', error, info)
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex min-h-svh w-full flex-col items-center justify-center gap-4 p-6 text-center">
          <div className="flex h-14 w-14 items-center justify-center rounded-sm border border-destructive/30 bg-destructive/10 text-destructive">
            <AlertTriangle className="size-6" />
          </div>
          <div>
            <h1 className="text-xl font-semibold text-foreground">Something went wrong</h1>
            <p className="mt-1 max-w-sm text-sm text-muted-foreground">
              An unexpected error occurred. Try reloading the page — if it keeps happening,
              contact support.
            </p>
          </div>
          <Button size="sm" onClick={() => window.location.reload()}>
            Reload page
          </Button>
        </div>
      )
    }

    return this.props.children
  }
}
