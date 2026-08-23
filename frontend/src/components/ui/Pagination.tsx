/**
 * Pagination control.
 *
 * Renders prev/next and page-number buttons based on backend PageMetadata.
 * Page index is zero-based (matching the backend contract).
 * Only calls onPageChange — URL update is the caller's responsibility.
 */

import styles from './Pagination.module.css'

interface PaginationProps {
  /** Zero-based current page index */
  page: number
  totalPages: number
  totalElements: number
  onPageChange: (page: number) => void
}

export function Pagination({ page, totalPages, totalElements, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null

  const pages = buildPageWindow(page, totalPages)

  return (
    <nav className={styles.nav} aria-label="Pagination">
      <span className={styles.summary}>
        Page {page + 1} of {totalPages} ({totalElements.toLocaleString()} results)
      </span>
      <div className={styles.controls}>
        <button
          className={styles.btn}
          onClick={() => onPageChange(page - 1)}
          disabled={page === 0}
          aria-label="Previous page"
        >
          ‹ Prev
        </button>

        {pages.map((p, i) =>
          p === '…' ? (
            <span key={`ellipsis-${i}`} className={styles.ellipsis}>
              …
            </span>
          ) : (
            <button
              key={p}
              className={`${styles.btn} ${p === page ? styles.active : ''}`}
              onClick={() => onPageChange(p as number)}
              aria-label={`Page ${(p as number) + 1}`}
              aria-current={p === page ? 'page' : undefined}
            >
              {(p as number) + 1}
            </button>
          ),
        )}

        <button
          className={styles.btn}
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1}
          aria-label="Next page"
        >
          Next ›
        </button>
      </div>
    </nav>
  )
}

/** Build a condensed window of page numbers with ellipsis where needed */
function buildPageWindow(current: number, total: number): Array<number | '…'> {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i)
  const result: Array<number | '…'> = []
  const add = (n: number) => { if (!result.includes(n)) result.push(n) }
  add(0)
  if (current > 2) result.push('…')
  for (let i = Math.max(1, current - 1); i <= Math.min(total - 2, current + 1); i++) add(i)
  if (current < total - 3) result.push('…')
  add(total - 1)
  return result
}
