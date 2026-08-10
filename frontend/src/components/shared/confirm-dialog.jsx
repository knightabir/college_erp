import { useState } from 'react'
import {
  Dialog,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Spinner } from '@/components/ui/spinner'

/**
 * ConfirmDialog — for destructive operations (delete, deactivate, etc.)
 * React Aria API: uses isOpen / onOpenChange.
 * The dialog stays open on error. Loading state disables both buttons.
 */
export function ConfirmDialog({
  open,
  onOpenChange,
  title = 'Confirm Action',
  description,
  entityName,
  actionLabel = 'Delete',
  onConfirm,
}) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  async function handleConfirm() {
    setLoading(true)
    setError(null)
    try {
      await onConfirm()
      onOpenChange(false)
    } catch (err) {
      setError(err?.message ?? 'Something went wrong. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  function handleOpenChange(val) {
    if (!loading) {
      setError(null)
      onOpenChange(val)
    }
  }

  return (
    <Dialog
      isOpen={open}
      onOpenChange={handleOpenChange}
      isDismissable={!loading}
      className="sm:max-w-105"
    >
      <DialogHeader>
        <DialogTitle>{title}</DialogTitle>
        <DialogDescription>
          {description ?? (
            <>
              Are you sure you want to {actionLabel.toLowerCase()}{' '}
              {entityName && (
                <span className="font-semibold text-foreground">{entityName}</span>
              )}
              ? This action cannot be undone.
            </>
          )}
        </DialogDescription>
      </DialogHeader>

      {error && (
        <p className="rounded-sm border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {error}
        </p>
      )}

      <DialogFooter>
        <Button
          variant="outline"
          onClick={() => handleOpenChange(false)}
          disabled={loading}
        >
          Cancel
        </Button>
        <Button
          onClick={handleConfirm}
          disabled={loading}
          className="bg-destructive text-white hover:bg-destructive/90"
        >
          {loading ? (
            <>
              <Spinner className="mr-2 size-3.5" />
              {actionLabel}ing…
            </>
          ) : (
            actionLabel
          )}
        </Button>
      </DialogFooter>
    </Dialog>
  )
}
