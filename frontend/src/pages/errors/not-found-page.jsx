import { useNavigate } from 'react-router-dom'
import { FileQuestion } from 'lucide-react'
import { Button } from '@/components/ui/button'

export default function NotFoundPage() {
  const navigate = useNavigate()
  return (
    <div className="flex min-h-svh w-full flex-col items-center justify-center gap-4 p-6 text-center">
      <div className="flex h-14 w-14 items-center justify-center rounded-sm border border-border bg-muted text-muted-foreground">
        <FileQuestion className="size-6" />
      </div>
      <div>
        <h1 className="text-xl font-semibold text-foreground">Page not found</h1>
        <p className="mt-1 max-w-sm text-sm text-muted-foreground">
          The page you're looking for doesn't exist or may have been moved.
        </p>
      </div>
      <Button size="sm" onClick={() => navigate('/')}>
        Back to Dashboard
      </Button>
    </div>
  )
}
