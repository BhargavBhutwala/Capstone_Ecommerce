/**
 * Payment API service.
 *
 * Maps frontend calls to the backend payment operationIds defined in
 * docs/03-openapi-specification.yaml.
 *
 * All payment endpoints require authentication (Bearer JWT).
 *
 * operationId mapping:
 *   initiatePayment → POST /payments
 *   getPayment      → GET  /payments/{paymentId}
 *
 * This is a simulated payment processor — no real card data is collected
 * or transmitted. The backend determines the payment amount from the order.
 */

import { apiGet, apiPost } from './client'
import type { CreatePaymentRequest, PaymentResponse } from '../types/api'

/**
 * operationId: initiatePayment
 * POST /payments — requires authentication
 * Initiates payment for the given order with the chosen payment method.
 * Returns PaymentResponse (status: INITIATED → PROCESSING → SUCCESS/FAILED).
 * The payment amount is authoritative from the backend — never from the client.
 */
export function initiatePayment(body: CreatePaymentRequest): Promise<PaymentResponse> {
  return apiPost<PaymentResponse>('/payments', body)
}

/**
 * operationId: getPayment
 * GET /payments/{paymentId} — requires authentication
 * Fetches the current status of a previously initiated payment.
 */
export function getPayment(paymentId: number): Promise<PaymentResponse> {
  return apiGet<PaymentResponse>(`/payments/${paymentId}`)
}
