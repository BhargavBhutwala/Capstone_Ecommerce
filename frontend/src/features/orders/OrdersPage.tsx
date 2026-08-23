/**
 * OrdersPage — authenticated order history list.
 *
 * operationId: listOrders → GET /orders
 *
 * Features:
 * - Paginated order list, newest first (backend ordering, not client-side).
 * - Optional status filter using the documented OrderStatus enum values.
 * - Loading / empty / error states.
 * - Each row links to /orders/:orderId detail page.
 */

import { useSearchParams, Link } from 'react-router-dom'
import * as orderApi from '../../api/orderApi'
import { useAsync } from '../../hooks/useAsync'
import { LoadingSpinner } from '../../components/states/LoadingSpinner'
import { ErrorState } from '../../components/states/ErrorState'
import { EmptyState } from '../../components/states/EmptyState'
import { Pagination } from '../../components/ui/Pagination'
import type { OrderStatus } from '../../types/api'
import styles from './OrdersPage.module.css'

// All documented OrderStatus values — used to build the filter dropdown
const ALL_STATUSES: OrderStatus[] = [
  'PENDING_PAYMENT',
  'PAID',
  'CONFIRMED',
  'CANCELLED',
  'SHIPPED',
  'DELIVERED',
  'RETURN_REQUESTED',
  'RETURNED',
  'REFUNDED',
]

function statusLabel(s: OrderStatus): string {
  return s
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase())
}

function formatDate(iso: string): string {
  try {
    return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(iso))
  } catch {
    return iso
  }
}

const DEFAULT_SIZE = 20

export function OrdersPage() {
  const [searchParams, setSearchParams] = useSearchParams()

  const page   = searchParams.get('page')   ? Number(searchParams.get('page'))   : 0
  const status = (searchParams.get('status') as OrderStatus | null) ?? undefined

  const ordersState = useAsync(
    () => orderApi.listOrders({ page, size: DEFAULT_SIZE, status }),
    [page, status],
  )

  function handleStatusChange(e: React.ChangeEvent<HTMLSelectElement>) {
    const next = new URLSearchParams()
    if (e.target.value) next.set('status', e.target.value)
    next.set('page', '0')
    setSearchParams(next, { replace: true })
  }

  function handlePageChange(newPage: number) {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev)
        next.set('page', String(newPage))
        return next
      },
      { replace: true },
    )
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const { data: result, loading, error, reload } = ordersState

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <h1 className={styles.heading}>My Orders</h1>

        {/* Status filter — backed by documented OrderStatus enum only */}
        <select
          value={status ?? ''}
          onChange={handleStatusChange}
          className={styles.statusSelect}
          aria-label="Filter by status"
        >
          <option value="">All statuses</option>
          {ALL_STATUSES.map((s) => (
            <option key={s} value={s}>
              {statusLabel(s)}
            </option>
          ))}
        </select>
      </div>

      {loading && <LoadingSpinner label="Loading orders…" />}

      {!loading && error && <ErrorState message={error} onRetry={reload} />}

      {!loading && !error && result?.content.length === 0 && (
        <EmptyState
          message="No orders found."
          hint={status ? 'Try removing the status filter.' : 'Place your first order to see it here.'}
        />
      )}

      {!loading && !error && result && result.content.length > 0 && (
        <>
          <ul className={styles.list}>
            {result.content.map((order) => (
              <li key={order.id} className={styles.card}>
                <Link to={`/orders/${order.id}`} className={styles.cardLink}>
                  <div className={styles.cardTop}>
                    <span className={styles.orderNumber}>#{order.orderNumber}</span>
                    <span className={`${styles.statusBadge} ${styles[`status_${order.status}`]}`}>
                      {statusLabel(order.status)}
                    </span>
                  </div>
                  <div className={styles.cardMeta}>
                    {order.placedAt && (
                      <span>{formatDate(order.placedAt)}</span>
                    )}
                    <span>{order.items.length} item{order.items.length !== 1 ? 's' : ''}</span>
                    <span className={styles.total}>
                      ${order.totalAmount.toFixed(2)}
                    </span>
                  </div>
                </Link>
              </li>
            ))}
          </ul>

          <Pagination
            page={result.page.page}
            totalPages={result.page.totalPages}
            totalElements={result.page.totalElements}
            onPageChange={handlePageChange}
          />
        </>
      )}
    </div>
  )
}
