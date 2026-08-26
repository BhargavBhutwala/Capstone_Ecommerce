/**
 * CartPage — authenticated cart workflow.
 *
 * operationId: getCart        → GET    /cart
 * operationId: addCartItem    → POST   /cart/items  (initiated from ProductCard/ProductDetailPage)
 * operationId: updateCartItem → PUT    /cart/items/{itemId}
 * operationId: removeCartItem → DELETE /cart/items/{itemId}
 *
 * Design:
 * - Cart state is always loaded fresh from the backend (server is authoritative).
 * - Mutations (update/remove) re-use the CartResponse returned by the mutation,
 *   or refetch after a 204 remove, so the UI always reflects server state.
 * - No localStorage/sessionStorage cart; no client-side total calculation.
 * - 409 conflict errors are shown inline next to the affected action.
 * - recommendedProducts are rendered using the shared ProductGrid component.
 */

import { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'

import * as cartApi from '../../api/cartApi'
import { ApiError } from '../../api/client'
import { useAsync } from '../../hooks/useAsync'
import { LoadingSpinner } from '../../components/states/LoadingSpinner'
import { ErrorState } from '../../components/states/ErrorState'
import { EmptyState } from '../../components/states/EmptyState'
import { ProductGrid } from '../../components/ui/ProductGrid'
import type { CartResponse, CartItemResponse } from '../../types/api'
import { formatCurrency } from '../../utils/formatCurrency'
import styles from './CartPage.module.css'

// ─── CartItemRow ──────────────────────────────────────────────────────────────

interface CartItemRowProps {
  item: CartItemResponse
  onQuantityChange: (itemId: number, quantity: number) => Promise<void>
  onRemove: (itemId: number) => Promise<void>
  /** Inline conflict / error message for this specific row */
  rowError: string | null
}

function CartItemRow({ item, onQuantityChange, onRemove, rowError }: CartItemRowProps) {
  const [pending, setPending] = useState(false)

  async function handleQuantityChange(e: React.ChangeEvent<HTMLInputElement>) {
    const value = parseInt(e.target.value, 10)
    // Client-side: reject clearly invalid values before sending request
    if (isNaN(value) || value < 1 || value > 999) return
    setPending(true)
    try {
      await onQuantityChange(item.id, value)
    } finally {
      setPending(false)
    }
  }

  async function handleRemove() {
    setPending(true)
    try {
      await onRemove(item.id)
    } finally {
      setPending(false)
    }
  }

  return (
    <div className={styles.itemRow}>
      <div className={styles.itemInfo}>
        <Link to={`/products/${item.product.id}`} className={styles.itemTitle}>
          {item.product.title}
        </Link>
        {item.product.isbn && (
          <span className={styles.itemIsbn}>ISBN: {item.product.isbn}</span>
        )}
        <span className={styles.itemUnitPrice}>
          {formatCurrency(item.unitPrice)} each
        </span>
      </div>

      <div className={styles.itemActions}>
        <label htmlFor={`qty-${item.id}`} className={styles.qtyLabel}>
          Qty:
        </label>
        <input
          id={`qty-${item.id}`}
          type="number"
          min={1}
          max={999}
          value={item.quantity}
          onChange={handleQuantityChange}
          disabled={pending}
          className={styles.qtyInput}
          aria-label={`Quantity for ${item.product.title}`}
        />
        <button
          onClick={handleRemove}
          disabled={pending}
          className={styles.removeBtn}
          aria-label={`Remove ${item.product.title} from cart`}
        >
          Remove
        </button>
      </div>

      <div className={styles.itemSubtotal}>
        {formatCurrency(item.subtotal)}
      </div>

      {rowError && (
        <p className={styles.rowError} role="alert">
          {rowError}
        </p>
      )}
    </div>
  )
}

// ─── CartPage ─────────────────────────────────────────────────────────────────

export function CartPage() {
  // Server-authoritative cart state — refreshed after mutations
  const [cart, setCart] = useState<CartResponse | null>(null)
  // Per-item row errors (keyed by itemId) for 409/400 conflict messages
  const [rowErrors, setRowErrors] = useState<Record<number, string>>({})
  // Top-level mutation error (fallback for unexpected failures)
  const [mutationError, setMutationError] = useState<string | null>(null)

  const asyncState = useAsync(async () => {
    const data = await cartApi.getCart()
    setCart(data)
    return data
  }, [])

  // Expose server-derived cart; prefer live `cart` state over initial asyncState.data
  const displayCart = cart ?? asyncState.data

  // ── Update quantity ────────────────────────────────────────────────────────

  const handleQuantityChange = useCallback(
    async (itemId: number, quantity: number) => {
      // Clear any prior error for this row
      setRowErrors((prev) => {
        const next = { ...prev }
        delete next[itemId]
        return next
      })
      setMutationError(null)

      try {
        const updated = await cartApi.updateCartItem(itemId, { quantity })
        setCart(updated)
      } catch (err) {
        if (err instanceof ApiError) {
          if (err.isConflict) {
            // Surface 409 inline next to the affected item
            setRowErrors((prev) => ({ ...prev, [itemId]: err.message }))
            return
          }
          if (err.status === 400) {
            setRowErrors((prev) => ({ ...prev, [itemId]: err.message }))
            return
          }
          if (err.status === 404) {
            // Item no longer exists — refetch the cart
            asyncState.reload()
            return
          }
          // 401 — session expired; existing FE-02 handler will catch on next navigation
        }
        setMutationError(
          err instanceof Error ? err.message : 'Failed to update quantity.',
        )
      }
    },
    [asyncState],
  )

  // ── Remove item ────────────────────────────────────────────────────────────

  const handleRemove = useCallback(
    async (itemId: number) => {
      setRowErrors((prev) => {
        const next = { ...prev }
        delete next[itemId]
        return next
      })
      setMutationError(null)

      try {
        // removeCartItem returns 204 — refetch cart to get authoritative state
        await cartApi.removeCartItem(itemId)
        const updated = await cartApi.getCart()
        setCart(updated)
      } catch (err) {
        if (err instanceof ApiError && err.status === 404) {
          // Already removed — silently refetch
          asyncState.reload()
          return
        }
        setMutationError(
          err instanceof Error ? err.message : 'Failed to remove item.',
        )
      }
    },
    [asyncState],
  )

  // ── Render: loading ────────────────────────────────────────────────────────

  if (asyncState.loading && !displayCart) {
    return <LoadingSpinner label="Loading cart…" />
  }

  // ── Render: fetch error ────────────────────────────────────────────────────

  if (asyncState.error && !displayCart) {
    return (
      <ErrorState
        message={asyncState.error}
        onRetry={asyncState.reload}
      />
    )
  }

  // ── Render: empty cart ─────────────────────────────────────────────────────

  if (displayCart && displayCart.items.length === 0) {
    return (
      <div className={styles.page}>
        <h1 className={styles.heading}>Your cart</h1>
        <EmptyState
          message="Your cart is empty."
          hint="Browse products and add some books!"
        />
        <Link to="/products" className={styles.shopLink}>
          Browse products
        </Link>

        {/* Still render recommendations if the empty cart has any */}
        {displayCart.recommendedProducts && displayCart.recommendedProducts.length > 0 && (
          <section className={styles.recommendations}>
            <h2 className={styles.recTitle}>Recommended for you</h2>
            <ProductGrid products={displayCart.recommendedProducts} />
          </section>
        )}
      </div>
    )
  }

  if (!displayCart) return null

  return (
    <div className={styles.page}>
      <h1 className={styles.heading}>Your cart</h1>

      {/* ── Top-level mutation error ── */}
      {mutationError && (
        <p className={styles.mutationError} role="alert">
          {mutationError}
        </p>
      )}

      {/* ── Item list ── */}
      <div className={styles.itemList}>
        <div className={styles.listHeader}>
          <span className={styles.colItem}>Item</span>
          <span className={styles.colQty}>Quantity</span>
          <span className={styles.colTotal}>Subtotal</span>
        </div>
        {displayCart.items.map((item) => (
          <CartItemRow
            key={item.id}
            item={item}
            onQuantityChange={handleQuantityChange}
            onRemove={handleRemove}
            rowError={rowErrors[item.id] ?? null}
          />
        ))}
      </div>

      {/* ── Order summary ── */}
      <div className={styles.summary}>
        <div className={styles.summaryRow}>
          <span>Subtotal</span>
          <span>{formatCurrency(displayCart.subtotal)}</span>
        </div>
        <div className={`${styles.summaryRow} ${styles.summaryTotal}`}>
          <span>Total</span>
          <span>{formatCurrency(displayCart.totalAmount)}</span>
        </div>
        {/* Checkout link — navigation only; checkout implemented in FE-05 */}
        <Link to="/checkout" className={styles.checkoutBtn}>
          Proceed to Checkout
        </Link>
      </div>

      {/* ── Recommended products ── */}
      {displayCart.recommendedProducts && displayCart.recommendedProducts.length > 0 && (
        <section className={styles.recommendations}>
          <h2 className={styles.recTitle}>Recommended for you</h2>
          <ProductGrid products={displayCart.recommendedProducts} />
        </section>
      )}
    </div>
  )
}
