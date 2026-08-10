import { useNavigate } from 'react-router-dom'
import { ShieldAlert } from 'lucide-react'
import { Button } from '@/components/ui/button'

export default function ForbiddenPage() {
  const navigate = useNavigate()
  return (
    <div className="flex min-h-svh w-full flex-col items-center justify-center gap-4 p-6 text-center">
      <div className="flex h-14 w-14 items-center justify-center rounded-sm border border-destructive/30 bg-destructive/10 text-destructive">
        <ShieldAlert className="size-6" />
      </div>
      <div>
        <h1 className="text-xl font-semibold text-foreground">You don't have access to this page</h1>
        <p className="mt-1 max-w-sm text-sm text-muted-foreground">
          Your account role doesn't include permission to view this section. Contact your
          administrator if you believe this is a mistake.
        </p>
      </div>
      <Button size="sm" onClick={() => navigate('/')}>
        Back to Dashboard
      </Button>
    </div>
  )
}
