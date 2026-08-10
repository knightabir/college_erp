import { Skeleton } from '@/components/ui/skeleton'

/**
 * LoadingState — skeleton rows for table-level page loading.
 * @param {number} rows     — Number of skeleton rows (default: 8)
 * @param {number} cols     — Number of skeleton columns (default: 5)
 */
export function TableLoadingState({ rows = 8, cols = 5 }) {
  return (
    <div className="w-full space-y-px">
      {/* Header skeleton */}
      <div className="flex h-10 items-center gap-4 border-b border-border bg-muted/40 px-4">
        {Array.from({ length: cols }).map((_, i) => (
          <Skeleton key={i} className="h-3 flex-1 rounded-sm" />
        ))}
      </div>
      {/* Row skeletons */}
      {Array.from({ length: rows }).map((_, i) => (
        <div
          key={i}
          className="flex h-12 items-center gap-4 border-b border-border px-4"
        >
          {Array.from({ length: cols }).map((_, j) => (
            <Skeleton
              key={j}
              className="h-3 flex-1 rounded-sm"
              style={{ opacity: 1 - i * 0.07 }}
            />
          ))}
        </div>
      ))}
    </div>
  )
}

/**
 * CardLoadingState — skeleton for metric/stat cards.
 */
export function CardLoadingState({ count = 4 }) {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {Array.from({ length: count }).map((_, i) => (
        <div
          key={i}
          className="rounded-sm border border-border bg-card p-5"
        >
          <Skeleton className="mb-3 h-3 w-24 rounded-sm" />
          <Skeleton className="mb-2 h-7 w-16 rounded-sm" />
          <Skeleton className="h-3 w-32 rounded-sm" />
        </div>
      ))}
    </div>
  )
}
