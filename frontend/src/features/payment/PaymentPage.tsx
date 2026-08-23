/**
 * PaymentPage — simulated payment flow for a newly created order.
 *
 * Route: /orders/:orderId/payment
 *
 * operationId: initiatePayment → POST /payments
 * operationId: getPayment      → GET  /payments/{paymentId}
 *
 * Flow:
 * 1. User arrives at this page after createOrder (FE-05 checkout).
 *    orderId comes from the URL parameter.
 * 2. User selects a payment method (CREDIT_CARD or DEBIT_CARD).
 * 3. User clicks "Pay now" → POST /payments with { orderId, paymentMethod }.
 * 4. Backend returns a PaymentResponse.
 *    - On SUCCESS: show confirmation with backend amount/status/paidAt.
 *    - On FAILED: show error with retry option.
 *    - On INITIATED/PROCESSING: show status with GET /payments/{id} option.
 * 5. If the user revisits the page and a paymentId is known (session state),
 *    they can refresh the status via GET /payments/{paymentId}.
 *
 * Design rules:
 * - The backend is authoritative for the payment amount (never calculated here).
 * - No real card numbers, CVV, expiry dates, or banking data are collected.
 * - No real payment gateway is used — this is simulated payment only.
 * - No refund logic.
 * - 400 / 401 / 404 / 409 errors use the shared ApiError infrastructure.
 */

import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import * as paymentApi from '../../api/paymentApi'
import { ApiError } from '../../api/client'
import type { PaymentMethod, PaymentResponse, PaymentStatus } from '../../types/api'
import styles from './PaymentPage.module.css'

// ─── Helpers ──────────────────────────────────────────────────────────────────

function statusLabel(status: PaymentStatus): string {
  switch (status) {
    case 'INITIATED':   return 'Initiated'
    case 'PROCESSING':  return 'Processing'
    case 'SUCCESS':     return 'Payment successful'
    case 'FAILED':      return 'Payment failed'
    case 'REFUNDED':    return 'Refunded'
  }
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

// ─── Payment result panel ─────────────────────────────────────────────────────

interface PaymentResultProps {
  payment: PaymentResponse
  onRefreshStatus: () => void
  refreshing: boolean
}

function PaymentResult({ payment, onRefreshStatus, refreshing }: PaymentResultProps) {
  const isSuccess = payment.status === 'SUCCESS'
  const isFailed  = payment.status === 'FAILED'
  const isPending = payment.status === 'INITIATED' || payment.status === 'PROCESSING'

  return (
    <div className={`${styles.result} ${isSuccess ? styles.resultSuccess : isFailed ? styles.resultFailed : styles.resultPending}`}>
      <p className={styles.resultStatus}>{statusLabel(payment.status)}</p>

      <dl className={styles.resultDetails}>
        <dt>Reference</dt>
        <dd>{payment.paymentReference}</dd>

        <dt>Amount</dt>
        <dd>${payment.amount.toFixed(2)}</dd>

        <dt>Payment method</dt>
        <dd>{payment.paymentMethod === 'CREDIT_CARD' ? 'Credit card' : 'Debit card'}</dd>

        {payment.paidAt && (
          <>
            <dt>Paid at</dt>
            <dd>{formatDateTime(payment.paidAt)}</dd>
          </>
        )}
      </dl>

      {isSuccess && (
        <div className={styles.resultActions}>
          <Link to="/orders" className={styles.ordersLink}>
            View my orders
          </Link>
        </div>
      )}

      {isFailed && (
        <p className={styles.failedNote}>
          Your payment could not be processed. Please try again or contact support.
        </p>
      )}

      {isPending && (
        <div className={styles.resultActions}>
          <button
            className={styles.refreshBtn}
            onClick={onRefreshStatus}
            disabled={refreshing}
          >
            {refreshing ? 'Checking…' : 'Refresh status'}
          </button>
        </div>
      )}
    </div>
  )
}

// ─── PaymentPage ──────────────────────────────────────────────────────────────

export function PaymentPage() {
  const { orderId } = useParams<{ orderId: string }>()
  const orderIdNum = Number(orderId)

  const [selectedMethod, setSelectedMethod] = useState<PaymentMethod>('CREDIT_CARD')
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)

  // Holds the latest PaymentResponse once the user initiates or refreshes
  const [payment, setPayment] = useState<PaymentResponse | null>(null)
  const [refreshing, setRefreshing] = useState(false)
  const [refreshError, setRefreshError] = useState<string | null>(null)

  // ── Initiate payment ───────────────────────────────────────────────────────

  async function handlePay() {
    if (!orderIdNum) return
    setSubmitError(null)
    setSubmitting(true)
    try {
      const result = await paymentApi.initiatePayment({
        orderId: orderIdNum,
        paymentMethod: selectedMethod,
      })
      setPayment(result)
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

  // ── Refresh payment status ─────────────────────────────────────────────────

  async function handleRefreshStatus() {
    if (!payment) return
    setRefreshError(null)
    setRefreshing(true)
    try {
      const updated = await paymentApi.getPayment(payment.id)
      setPayment(updated)
    } catch (err) {
      if (err instanceof ApiError) {
        setRefreshError(err.message)
      } else if (err instanceof Error) {
        setRefreshError(err.message)
      } else {
        setRefreshError('Failed to refresh payment status.')
      }
    } finally {
      setRefreshing(false)
    }
  }

  // ── Guard: invalid orderId ─────────────────────────────────────────────────

  if (!orderIdNum || isNaN(orderIdNum)) {
    return (
      <div className={styles.page}>
        <p className={styles.errorNote}>Invalid order. <Link to="/orders">Back to orders</Link></p>
      </div>
    )
  }

  return (
    <div className={styles.page}>
      <h1 className={styles.heading}>Payment</h1>
      <p className={styles.orderRef}>Order #{orderId}</p>

      {/* ── Payment has been initiated: show result ── */}
      {payment ? (
        <>
          <PaymentResult
            payment={payment}
            onRefreshStatus={handleRefreshStatus}
            refreshing={refreshing}
          />
          {refreshError && (
            <p className={styles.errorNote} role="alert">{refreshError}</p>
          )}
        </>
      ) : (
        /* ── Payment form ── */
        <div className={styles.card}>
          <h2 className={styles.sectionTitle}>Select payment method</h2>

          <p className={styles.simNote}>
            This is a simulated payment. No real card data is collected or processed.
          </p>

          <fieldset className={styles.methodGroup} disabled={submitting}>
            <legend className={styles.methodLegend}>Payment method</legend>

            <label className={`${styles.methodOption} ${selectedMethod === 'CREDIT_CARD' ? styles.methodSelected : ''}`}>
              <input
                type="radio"
                name="paymentMethod"
                value="CREDIT_CARD"
                checked={selectedMethod === 'CREDIT_CARD'}
                onChange={() => setSelectedMethod('CREDIT_CARD')}
                className={styles.methodRadio}
              />
              <span className={styles.methodLabel}>Credit card</span>
            </label>

            <label className={`${styles.methodOption} ${selectedMethod === 'DEBIT_CARD' ? styles.methodSelected : ''}`}>
              <input
                type="radio"
                name="paymentMethod"
                value="DEBIT_CARD"
                checked={selectedMethod === 'DEBIT_CARD'}
                onChange={() => setSelectedMethod('DEBIT_CARD')}
                className={styles.methodRadio}
              />
              <span className={styles.methodLabel}>Debit card</span>
            </label>
          </fieldset>

          {submitError && (
            <p className={styles.errorNote} role="alert">{submitError}</p>
          )}

          <button
            className={styles.payBtn}
            onClick={handlePay}
            disabled={submitting}
          >
            {submitting ? 'Processing…' : 'Pay now'}
          </button>

          <p className={styles.backNote}>
            <Link to="/orders">Cancel and view orders</Link>
          </p>
        </div>
      )}
    </div>
  )
}
