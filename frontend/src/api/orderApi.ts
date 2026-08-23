/**
 * Order API service.
 *
 * Maps frontend calls to the backend order operationIds defined in
 * docs/03-openapi-specification.yaml.
 *
 * All order endpoints require authentication (Bearer JWT).
 *
 * operationId mapping:
 *   createOrder → POST /orders
 *   listOrders  → GET  /orders
 *   getOrder    → GET  /orders/{orderId}
 *   buyAgain    → POST /orders/{orderId}/buy-again
 *   cancelOrder → POST /orders/{orderId}/cancel
 */

import { apiGet, apiPost } from './client'
import type {
  CartResponse,
  CreateOrderRequest,
  OrderResponse,
  OrderStatus,
  PagedResponse,
} from '../types/api'

/**
 * operationId: createOrder
 * POST /orders — requires authentication
 * Creates an order from the active cart using the given shipping address.
 * Returns the created OrderResponse (id used to navigate to payment).
 */
export function createOrder(body: CreateOrderRequest): Promise<OrderResponse> {
  return apiPost<OrderResponse>('/orders', body)
}

// ─── Query params for listOrders ──────────────────────────────────────────────

export interface ListOrdersParams {
  /** Zero-based page index */
  page?: number
  /** Page size (default 20) */
  size?: number
  /** Optional status filter — exact OrderStatus enum value */
  status?: OrderStatus
}

/**
 * operationId: listOrders
 * GET /orders — requires authentication
 * Returns the authenticated user's order history, newest first.
 * Supports optional server-side status filter and pagination.
 */
export function listOrders(params: ListOrdersParams = {}): Promise<PagedResponse<OrderResponse>> {
  const parts: string[] = []
  if (params.page !== undefined) parts.push(`page=${params.page}`)
  if (params.size  !== undefined) parts.push(`size=${params.size}`)
  if (params.status) parts.push(`status=${encodeURIComponent(params.status)}`)
  const qs = parts.length ? `?${parts.join('&')}` : ''
  return apiGet<PagedResponse<OrderResponse>>(`/orders${qs}`)
}

/**
 * operationId: getOrder
 * GET /orders/{orderId} — requires authentication
 * Returns full order detail including item snapshots, shipping address, totals.
 */
export function getOrder(orderId: number): Promise<OrderResponse> {
  return apiGet<OrderResponse>(`/orders/${orderId}`)
}

/**
 * operationId: buyAgain
 * POST /orders/{orderId}/buy-again — requires authentication
 * Re-adds products from the given historical order to the active cart.
 * Returns the updated CartResponse on 200; throws ApiError on 409 (unavailable).
 */
export function buyAgain(orderId: number): Promise<CartResponse> {
  return apiPost<CartResponse>(`/orders/${orderId}/buy-again`)
}

/**
 * operationId: cancelOrder
 * POST /orders/{orderId}/cancel — requires authentication
 * Cancels an eligible order (within 48h, correct status).
 * Returns the updated OrderResponse on 200; throws ApiError 409 if ineligible.
 */
export function cancelOrder(orderId: number): Promise<OrderResponse> {
  return apiPost<OrderResponse>(`/orders/${orderId}/cancel`)
}
