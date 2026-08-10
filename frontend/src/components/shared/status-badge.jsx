import { cn } from '@/lib/utils'

/**
 * StatusBadge — semantic status display.
 *
 * Variants map to the utility classes defined in index.css:
 *   active   → green
 *   inactive → grey
 *   pending  → amber
 *   danger   → red (rejected/failed/unpaid)
 *   info     → blue (processing/enrolled)
 */
const STATUS_MAP = {
  active:     { className: 'status-active',   label: 'Active' },
  inactive:   { className: 'status-inactive', label: 'Inactive' },
  pending:    { className: 'status-pending',  label: 'Pending' },
  approved:   { className: 'status-active',   label: 'Approved' },
  rejected:   { className: 'status-danger',   label: 'Rejected' },
  paid:       { className: 'status-active',   label: 'Paid' },
  unpaid:     { className: 'status-danger',   label: 'Unpaid' },
  draft:      { className: 'status-inactive', label: 'Draft' },
  processing: { className: 'status-info',     label: 'Processing' },
  enrolled:   { className: 'status-info',     label: 'Enrolled' },
  completed:  { className: 'status-active',   label: 'Completed' },
  failed:     { className: 'status-danger',   label: 'Failed' },
  warning:    { className: 'status-pending',  label: 'Warning' },
}

/**
 * @param {string} status  — Key from STATUS_MAP (case-insensitive)
 * @param {string} label   — Override display label
 */
export function StatusBadge({ status = 'inactive', label, className }) {
  const key = status.toLowerCase()
  const config = STATUS_MAP[key] ?? STATUS_MAP.inactive
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-sm px-2 py-0.5 text-xs font-medium',
        config.className,
        className
      )}
    >
      {label ?? config.label}
    </span>
  )
}
