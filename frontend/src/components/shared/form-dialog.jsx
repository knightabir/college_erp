import {
  Dialog,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'

/**
 * FormDialog — reusable wrapper for all entity create/edit forms.
 *
 * Uses the React Aria shadcn Dialog API:
 *   - `isOpen` / `onOpenChange` for controlled state
 *   - No separate DialogContent wrapper — Dialog renders the panel directly
 *   - Default size is sm:max-w-[520px] (overridable via `size` prop)
 */
export function FormDialog({
  open,
  onOpenChange,
  title,
  description,
  size = 'default',
  children,
}) {
  const maxWidths = {
    sm:      'sm:max-w-[420px]',
    default: 'sm:max-w-[520px]',
    lg:      'sm:max-w-[660px]',
    xl:      'sm:max-w-[820px]',
  }

  return (
    <Dialog
      isOpen={open}
      onOpenChange={onOpenChange}
      className={maxWidths[size] ?? maxWidths.default}
    >
      <DialogHeader>
        <DialogTitle>{title}</DialogTitle>
        {description && (
          <DialogDescription>{description}</DialogDescription>
        )}
      </DialogHeader>
      {children}
    </Dialog>
  )
}
