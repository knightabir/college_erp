import { cn } from '@/lib/utils'

/**
 * PageHeader — consistent top section for every page.
 *
 * @param {string}      title       — Page or section title
 * @param {string}      description — Subtitle / helper text
 * @param {ReactNode}   actions     — Right-aligned action buttons
 * @param {ReactNode}   breadcrumb  — Optional breadcrumb element
 */
export function PageHeader({ title, description, actions, breadcrumb, className }) {
  return (
    <div className={cn('flex flex-col gap-1 border-b border-border bg-card px-6 py-4', className)}>
      {breadcrumb && (
        <div className="mb-1 text-xs text-muted-foreground">{breadcrumb}</div>
      )}
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold text-foreground">{title}</h1>
          {description && (
            <p className="mt-0.5 text-sm text-muted-foreground">{description}</p>
          )}
        </div>
        {actions && (
          <div className="flex shrink-0 items-center gap-2">{actions}</div>
        )}
      </div>
    </div>
  )
}
