import { cn } from '@/lib/utils'

/**
 * PageContainer — standard content wrapper with consistent padding.
 * Keeps all page content aligned to the same grid.
 */
export function PageContainer({ children, className }) {
  return (
    <div className={cn('flex flex-col gap-6 p-6', className)}>
      {children}
    </div>
  )
}
