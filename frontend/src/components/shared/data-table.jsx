import { useState, useMemo } from 'react'
import { Search, ChevronLeft, ChevronRight } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { EmptyState } from './empty-state'
import { TableLoadingState } from './loading-state'
import { ErrorState } from './error-state'
import { cn } from '@/lib/utils'

const PAGE_SIZE = 10

/**
 * DataTable — opinionated data grid for administrative tables.
 * Uses plain HTML table elements to avoid React Aria Table collection constraints.
 *
 * @param {Array}    columns         — [{ key, label, render?, className?, headerClassName? }]
 * @param {Array}    data            — Array of row objects
 * @param {Array}    searchKeys      — Column keys to search across
 * @param {string}   searchPlaceholder
 * @param {ReactNode} filters        — Additional filter controls
 * @param {ReactNode} action         — Right-side action (e.g., Add button)
 * @param {boolean}  loading
 * @param {string}   error
 * @param {Function} onRetry
 * @param {string}   emptyTitle
 * @param {string}   emptyDescription
 * @param {ReactNode} emptyAction
 */
export function DataTable({
  columns = [],
  data = [],
  searchKeys = [],
  searchPlaceholder = 'Search…',
  filters,
  action,
  loading = false,
  error = null,
  onRetry,
  emptyTitle = 'No records found',
  emptyDescription,
  emptyAction,
}) {
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)

  const filtered = useMemo(() => {
    if (!search.trim()) return data
    const q = search.toLowerCase()
    return data.filter((row) =>
      searchKeys.some((k) => String(row[k] ?? '').toLowerCase().includes(q))
    )
  }, [data, search, searchKeys])

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const currentPage = Math.min(page, totalPages)
  const paginatedRows = filtered.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE
  )

  function handleSearch(e) {
    setSearch(e.target.value)
    setPage(1)
  }

  return (
    <div className="flex flex-col overflow-hidden rounded-sm border border-border bg-card">
      {/* Toolbar */}
      <div className="flex flex-wrap items-center gap-2 border-b border-border px-4 py-3">
        <div className="relative flex-1 min-w-45 max-w-xs">
          <Search className="pointer-events-none absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={search}
            onChange={handleSearch}
            placeholder={searchPlaceholder}
            className="h-8 pl-8 text-sm"
            aria-label="Search"
          />
        </div>
        {filters && (
          <div className="flex items-center gap-2">{filters}</div>
        )}
        {action && (
          <div className="ml-auto">{action}</div>
        )}
      </div>

      {/* Table area */}
      <div className="min-h-50 overflow-x-auto">
        {loading ? (
          <TableLoadingState rows={8} cols={columns.length} />
        ) : error ? (
          <ErrorState
            description="We couldn't load this data. Please try again."
            onRetry={onRetry}
          />
        ) : paginatedRows.length === 0 ? (
          <EmptyState
            title={search ? 'No matching results' : emptyTitle}
            description={
              search
                ? `No records match "${search}".`
                : emptyDescription
            }
            action={
              search ? (
                <Button variant="outline" size="sm" onClick={() => setSearch('')}>
                  Clear search
                </Button>
              ) : (
                emptyAction
              )
            }
          />
        ) : (
          <table className="w-full caption-bottom text-sm">
            <thead>
              <tr className="border-b border-border bg-muted/40">
                {columns.map((col) => (
                  <th
                    key={col.key}
                    scope="col"
                    className={cn(
                      'h-10 px-3 text-left align-middle text-xs font-semibold uppercase tracking-wide text-muted-foreground',
                      col.headerClassName
                    )}
                  >
                    {col.label}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {paginatedRows.map((row, rowIdx) => (
                <tr
                  key={row.id ?? rowIdx}
                  className="h-12 border-b border-border transition-colors hover:bg-muted/30 last:border-0"
                >
                  {columns.map((col) => (
                    <td
                      key={col.key}
                      className={cn('px-3 align-middle', col.className)}
                    >
                      {col.render ? col.render(row) : row[col.key]}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Pagination footer */}
      {!loading && !error && filtered.length > 0 && (
        <div className="flex items-center justify-between border-t border-border px-4 py-2.5 text-xs text-muted-foreground">
          <span>
            Showing{' '}
            <span className="font-medium text-foreground">
              {(currentPage - 1) * PAGE_SIZE + 1}–
              {Math.min(currentPage * PAGE_SIZE, filtered.length)}
            </span>{' '}
            of{' '}
            <span className="font-medium text-foreground">{filtered.length}</span>
            {filtered.length !== data.length && (
              <span> (filtered from {data.length})</span>
            )}
          </span>
          <div className="flex items-center gap-1">
            <Button
              variant="outline"
              size="icon-xs"
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              isDisabled={currentPage === 1}
              aria-label="Previous page"
            >
              <ChevronLeft className="size-3.5" />
            </Button>
            <span className="px-2 font-medium text-foreground">
              {currentPage} / {totalPages}
            </span>
            <Button
              variant="outline"
              size="icon-xs"
              onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              isDisabled={currentPage === totalPages}
              aria-label="Next page"
            >
              <ChevronRight className="size-3.5" />
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
