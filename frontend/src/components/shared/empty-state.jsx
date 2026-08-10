import { PackageOpen } from 'lucide-react'
import { cn } from '@/lib/utils'

/**
 * EmptyState — shown when a data set is empty.
 *
 * Two modes:
 *  - "no-results" (filtered/searched) → shows "Clear Filters" CTA
 *  - "empty" (genuinely no data)      → shows "Add First Item" CTA
 */
export function EmptyState({
  icon: Icon = PackageOpen,
  title = 'No data found',
  description,
  action,
  className,
}) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center gap-3 py-16 text-center',
        className
      )}
    >
      <div className="flex h-12 w-12 items-center justify-center rounded-sm border border-dashed border-border bg-muted text-muted-foreground">
        <Icon className="size-5" />
      </div>
      <div>
        <p className="text-sm font-semibold text-foreground">{title}</p>
        {description && (
          <p className="mt-1 text-sm text-muted-foreground">{description}</p>
        )}
      </div>
      {action && action}
    </div>
  )
}
