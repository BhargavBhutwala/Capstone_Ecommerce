/**
 * OrderDetailPage — full order detail with Buy Again and cancellation.
 *
 * Route: /orders/:orderId
 *
 * operationId: getOrder    → GET  /orders/{orderId}
 * operationId: buyAgain    → POST /orders/{orderId}/buy-again
 * operationId: cancelOrder → POST /orders/{orderId}/cancel
 *
 * Buy Again: adds all items from the historical order back to the active cart,
 *   then navigates to /cart on success. 409 surfaces inline.
 *
 * Cancellation:
 *   - Cancel button is shown only when the order status is PENDING_PAYMENT or
 *     CONFIRMED (a reasonable heuristic — the backend is the final authority).
 *   - On success the page re-fetches and shows the updated status.
 *   - 409 (ineligible) is shown inline without navigating away.
 *
 * All item/price/address data shown here is the immutable historical snapshot
 * stored on the order — never live product data.
 */

import { useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import * as orderApi from '../../api/orderApi'
import { ApiError } from '../../api/client'
import { useAsync } from '../../hooks/useAsync'
import { LoadingSpinner } from '../../components/states/LoadingSpinner'
import { ErrorState } from '../../components/states/ErrorState'
import type { OrderResponse, OrderStatus } from '../../types/api'
import styles from './OrderDetailPage.module.css'

// ─── Helpers ──────────────────────────────────────────────────────────────────

function statusLabel(s: OrderStatus): string {
  return s
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase())
}

function formatDateTime(iso: string): string {
  try {
    return new Intl.DateTimeFormat(undefined, {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(iso))
  } catch {
    return iso
  }
}

/**
 * Orders that may still be cancellable — heuristic only.
 * The backend is the definitive authority; 409 handles backend refusal.
 */
const CANCELLABLE_STATUSES: Set<OrderStatus> = new Set([
  'PENDING_PAYMENT',
  'PAID',
])

// ─── Component ────────────────────────────────────────────────────────────────

export function OrderDetailPage() {
  const { orderId } = useParams<{ orderId: string }>()
  const navigate    = useNavigate()
  const orderIdNum  = Number(orderId)

  const [buyAgainPending,  setBuyAgainPending]  = useState(false)
  const [buyAgainError,    setBuyAgainError]    = useState<string | null>(null)
  const [cancelPending,    setCancelPending]    = useState(false)
  const [cancelError,      setCancelError]      = useState<string | null>(null)

  // Holds an override when the order is refreshed after cancellation
  const [overrideOrder, setOverrideOrder] = useState<OrderResponse | null>(null)

  const asyncState = useAsync(async () => {
    if (!orderIdNum) throw new Error('INVALID_ORDER_ID')
    try {
      return await orderApi.getOrder(orderIdNum)
    } catch (err) {
      if (err instanceof ApiError && err.isNotFound) {
        const e = new Error('ORDER_NOT_FOUND')
        ;(e as Error & { cause?: unknown }).cause = err
        throw e
      }
      throw err
    }
  }, [orderIdNum])

  const order = overrideOrder ?? asyncState.data

  // ── Buy Again ──────────────────────────────────────────────────────────────

  async function handleBuyAgain() {
    if (!orderIdNum) return
    setBuyAgainError(null)
    setBuyAgainPending(true)
    try {
      await orderApi.buyAgain(orderIdNum)
      navigate('/cart')
    } catch (err) {
      const msg =
        err instanceof ApiError ? err.message
        : err instanceof Error  ? err.message
        : 'Failed to add items to cart.'
      setBuyAgainError(msg)
    } finally {
      setBuyAgainPending(false)
    }
  }

  // ── Cancel order ───────────────────────────────────────────────────────────

  async function handleCancel() {
    if (!orderIdNum) return
    setCancelError(null)
    setCancelPending(true)
    try {
      const updated = await orderApi.cancelOrder(orderIdNum)
      setOverrideOrder(updated)
    } catch (err) {
      const msg =
        err instanceof ApiError ? err.message
        : err instanceof Error  ? err.message
        : 'Cancellation failed.'
      setCancelError(msg)
    } finally {
      setCancelPending(false)
    }
  }

  // ── Render: loading ────────────────────────────────────────────────────────

  if (asyncState.loading && !order) {
    return <LoadingSpinner label="Loading order…" />
  }

  // ── Render: 404 ───────────────────────────────────────────────────────────

  if (asyncState.error === 'ORDER_NOT_FOUND' || asyncState.error === 'INVALID_ORDER_ID') {
    return (
      <div className={styles.notFound}>
        <h1 className={styles.notFoundCode}>404</h1>
        <p className={styles.notFoundMsg}>Order not found.</p>
        <Link to="/orders" className={styles.backLink}>Back to orders</Link>
      </div>
    )
  }

  // ── Render: general error ─────────────────────────────────────────────────

  if (asyncState.error && !order) {
    return <ErrorState message={asyncState.error} onRetry={asyncState.reload} />
  }

  if (!order) return null

  const canCancel = CANCELLABLE_STATUSES.has(order.status)

  return (
    <div className={styles.page}>
      {/* ── Breadcrumb ── */}
      <nav className={styles.breadcrumb} aria-label="Breadcrumb">
        <Link to="/orders">My Orders</Link>
        <span> / </span>
        <span className={styles.breadcrumbCurrent}>#{order.orderNumber}</span>
      </nav>

      {/* ── Header ── */}
      <div className={styles.header}>
        <div>
          <h1 className={styles.heading}>Order #{order.orderNumber}</h1>
          {order.placedAt && (
            <p className={styles.placedAt}>Placed {formatDateTime(order.placedAt)}</p>
          )}
        </div>
        <span className={`${styles.statusBadge} ${styles[`status_${order.status}`]}`}>
          {statusLabel(order.status)}
        </span>
      </div>

      {/* ── Actions ── */}
      <div className={styles.actions}>
        {/* Buy Again */}
        <div className={styles.actionGroup}>
          <button
            className={styles.buyAgainBtn}
            onClick={handleBuyAgain}
            disabled={buyAgainPending}
          >
            {buyAgainPending ? 'Adding to cart…' : 'Buy Again'}
          </button>
          {buyAgainError && (
            <p className={styles.actionError} role="alert">{buyAgainError}</p>
          )}
        </div>

        {/* Cancel — shown only when status heuristic permits */}
        {canCancel && (
          <div className={styles.actionGroup}>
            <button
              className={styles.cancelBtn}
              onClick={handleCancel}
              disabled={cancelPending}
            >
              {cancelPending ? 'Cancelling…' : 'Cancel order'}
            </button>
            {cancelError && (
              <p className={styles.actionError} role="alert">{cancelError}</p>
            )}
          </div>
        )}
      </div>

      <div className={styles.body}>
        {/* ── Left: items ── */}
        <section className={styles.itemsSection}>
          <h2 className={styles.sectionTitle}>Items</h2>
          <div className={styles.itemList}>
            <div className={styles.itemHeader}>
              <span>Product</span>
              <span className={styles.colRight}>Qty</span>
              <span className={styles.colRight}>Unit price</span>
              <span className={styles.colRight}>Subtotal</span>
            </div>
            {order.items.map((item) => (
              <div key={item.id} className={styles.itemRow}>
                <span className={styles.itemTitle}>{item.productTitle}</span>
                <span className={styles.colRight}>{item.quantity}</span>
                <span className={styles.colRight}>${item.unitPrice.toFixed(2)}</span>
                <span className={styles.colRight}>${item.subtotal.toFixed(2)}</span>
              </div>
            ))}
          </div>

          {/* Totals */}
          <div className={styles.totals}>
            <div className={styles.totalRow}>
              <span>Subtotal</span>
              <span>${order.subtotal.toFixed(2)}</span>
            </div>
            {order.shippingAmount !== undefined && order.shippingAmount > 0 && (
              <div className={styles.totalRow}>
                <span>Shipping</span>
                <span>${order.shippingAmount.toFixed(2)}</span>
              </div>
            )}
            {order.discountAmount !== undefined && order.discountAmount > 0 && (
              <div className={styles.totalRow}>
                <span>Discount</span>
                <span>−${order.discountAmount.toFixed(2)}</span>
              </div>
            )}
            <div className={`${styles.totalRow} ${styles.grandTotal}`}>
              <span>Total</span>
              <span>${order.totalAmount.toFixed(2)}</span>
            </div>
          </div>
        </section>

        {/* ── Right: metadata ── */}
        <aside className={styles.meta}>
          {/* Shipping address snapshot */}
          {order.shippingAddress && (
            <div className={styles.metaCard}>
              <h3 className={styles.metaTitle}>Shipping address</h3>
              <address className={styles.address}>
                {order.shippingAddress.name && (
                  <p>{order.shippingAddress.name}</p>
                )}
                <p>{order.shippingAddress.addressLine1}</p>
                {order.shippingAddress.addressLine2 && (
                  <p>{order.shippingAddress.addressLine2}</p>
                )}
                <p>
                  {order.shippingAddress.city}, {order.shippingAddress.state}{' '}
                  {order.shippingAddress.postalCode}
                </p>
                <p>{order.shippingAddress.country}</p>
              </address>
            </div>
          )}

          {/* Order info */}
          <div className={styles.metaCard}>
            <h3 className={styles.metaTitle}>Order information</h3>
            <dl className={styles.infoList}>
              <dt>Order ID</dt>
              <dd>{order.id}</dd>
              <dt>Order number</dt>
              <dd>{order.orderNumber}</dd>
              <dt>Status</dt>
              <dd>{statusLabel(order.status)}</dd>
              {order.placedAt && (
                <>
                  <dt>Placed</dt>
                  <dd>{formatDateTime(order.placedAt)}</dd>
                </>
              )}
              {order.cancellationDeadline && (
                <>
                  <dt>Cancel by</dt>
                  <dd>{formatDateTime(order.cancellationDeadline)}</dd>
                </>
              )}
            </dl>
          </div>
        </aside>
      </div>
    </div>
  )
}
