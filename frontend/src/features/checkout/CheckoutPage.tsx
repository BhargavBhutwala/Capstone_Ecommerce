/**
 * CheckoutPage — review cart and select a shipping address to create an order.
 *
 * operationId: getCart       → GET /cart           (display cart items + totals)
 * operationId: listAddresses → GET /addresses      (select shipping address)
 * operationId: createOrder   → POST /orders        (submit checkout)
 *
 * Design:
 * - The backend is authoritative for all prices, stock, and order totals.
 * - The frontend only passes addressId in CreateOrderRequest — nothing else.
 * - On success (HTTP 201): navigate to /orders/:orderId/payment.
 * - 400 / 404 / 409 errors are shown inline above the submit button.
 * - Empty cart prevents submission.
 * - No saved-address creation in this flow; user is directed to /addresses.
 * - No gift points, coupons, or discount logic.
 */

import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import * as cartApi from '../../api/cartApi'
import * as addressApi from '../../api/addressApi'
import * as orderApi from '../../api/orderApi'
import { ApiError } from '../../api/client'
import { useAsync } from '../../hooks/useAsync'
import { LoadingSpinner } from '../../components/states/LoadingSpinner'
import { ErrorState } from '../../components/states/ErrorState'
import type { AddressResponse } from '../../types/api'
import { formatCurrency } from '../../utils/formatCurrency'
import styles from './CheckoutPage.module.css'

export function CheckoutPage() {
  const navigate = useNavigate()

  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)

  // Fetch cart and addresses in parallel
  const cartState = useAsync(() => cartApi.getCart(), [])
  const addrState = useAsync(() => addressApi.listAddresses(), [])

  // ── Derived state ──────────────────────────────────────────────────────────

  const cart = cartState.data
  const addresses = addrState.data ?? []

  // Auto-select default address when addresses first load
  // (done once — after that the user controls the selection)
  const defaultAddress = addresses.find((a) => a.isDefault) ?? addresses[0] ?? null

  const effectiveAddressId =
    selectedAddressId !== null
      ? selectedAddressId
      : defaultAddress?.id ?? null

  const isLoading = cartState.loading || addrState.loading
  const loadError = cartState.error ?? addrState.error

  // ── Submit ─────────────────────────────────────────────────────────────────

  async function handlePlaceOrder() {
    if (effectiveAddressId === null) return
    setSubmitError(null)
    setSubmitting(true)

    try {
      const order = await orderApi.createOrder({ addressId: effectiveAddressId })
      // Navigate to payment page for the new order
      navigate(`/orders/${order.id}/payment`)
    } catch (err) {
      if (err instanceof ApiError) {
        setSubmitError(err.message)
      } else if (err instanceof Error) {
        setSubmitError(err.message)
      } else {
        setSubmitError('An unexpected error occurred. Please try again.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  // ── Render: loading ────────────────────────────────────────────────────────

  if (isLoading) {
    return <LoadingSpinner label="Loading checkout…" />
  }

  // ── Render: fetch errors ───────────────────────────────────────────────────

  if (loadError) {
    return (
      <ErrorState
        message={loadError}
        onRetry={() => {
          cartState.reload()
          addrState.reload()
        }}
      />
    )
  }

  // ── Render: empty cart ─────────────────────────────────────────────────────

  if (!cart || cart.items.length === 0) {
    return (
      <div className={styles.page}>
        <h1 className={styles.heading}>Checkout</h1>
        <div className={styles.emptyCart}>
          <p>Your cart is empty — add some books before checking out.</p>
          <Link to="/products" className={styles.browseLink}>
            Browse products
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className={styles.page}>
      <h1 className={styles.heading}>Checkout</h1>

      <div className={styles.layout}>
        {/* ── Left: order summary ── */}
        <section className={styles.summary}>
          <h2 className={styles.sectionTitle}>Order summary</h2>
          <ul className={styles.itemList}>
            {cart.items.map((item) => (
              <li key={item.id} className={styles.item}>
                <span className={styles.itemTitle}>{item.product.title}</span>
                <span className={styles.itemMeta}>
                  {item.quantity} × {formatCurrency(item.unitPrice)}
                </span>
                <span className={styles.itemSubtotal}>
                  {formatCurrency(item.subtotal)}
                </span>
              </li>
            ))}
          </ul>

          <div className={styles.totals}>
            <div className={styles.totalRow}>
              <span>Subtotal</span>
              <span>{formatCurrency(cart.subtotal)}</span>
            </div>
            <div className={`${styles.totalRow} ${styles.grandTotal}`}>
              <span>Total</span>
              <span>{formatCurrency(cart.totalAmount)}</span>
            </div>
          </div>

          <p className={styles.priceNote}>
            Final prices and stock are confirmed by the server at order creation.
          </p>
        </section>

        {/* ── Right: address selection + place order ── */}
        <section className={styles.addressSection}>
          <h2 className={styles.sectionTitle}>Shipping address</h2>

          {addresses.length === 0 ? (
            <div className={styles.noAddresses}>
              <p>You have no saved addresses.</p>
              <Link to="/addresses" className={styles.addAddrLink}>
                Add an address
              </Link>{' '}
              before placing your order.
            </div>
          ) : (
            <ul className={styles.addrList}>
              {addresses.map((addr: AddressResponse) => {
                const isSelected = effectiveAddressId === addr.id
                return (
                  <li
                    key={addr.id}
                    className={`${styles.addrCard} ${isSelected ? styles.addrSelected : ''}`}
                    onClick={() => setSelectedAddressId(addr.id)}
                  >
                    <input
                      type="radio"
                      id={`addr-${addr.id}`}
                      name="shippingAddress"
                      value={addr.id}
                      checked={isSelected}
                      onChange={() => setSelectedAddressId(addr.id)}
                      className={styles.addrRadio}
                    />
                    <label htmlFor={`addr-${addr.id}`} className={styles.addrLabel}>
                      {addr.label && (
                        <span className={styles.addrLabelText}>{addr.label}</span>
                      )}
                      {addr.isDefault && (
                        <span className={styles.defaultBadge}>Default</span>
                      )}
                      <span className={styles.addrLine}>{addr.addressLine1}</span>
                      {addr.addressLine2 && (
                        <span className={styles.addrLine}>{addr.addressLine2}</span>
                      )}
                      <span className={styles.addrLine}>
                        {addr.city}, {addr.state} {addr.postalCode}
                      </span>
                      <span className={styles.addrLine}>{addr.country}</span>
                    </label>
                  </li>
                )
              })}
            </ul>
          )}

          <p className={styles.manageLink}>
            <Link to="/addresses">Manage addresses</Link>
          </p>

          {/* ── Submit error ── */}
          {submitError && (
            <p className={styles.submitError} role="alert">
              {submitError}
            </p>
          )}

          {/* ── Place order button ── */}
          <button
            className={styles.placeOrderBtn}
            onClick={handlePlaceOrder}
            disabled={
              submitting ||
              effectiveAddressId === null ||
              addresses.length === 0
            }
          >
            {submitting ? 'Placing order…' : 'Place order'}
          </button>

          <p className={styles.paymentNote}>
            You will be redirected to payment after order creation.
          </p>
        </section>
      </div>
    </div>
  )
}
