/**
 * Order API service (MVP scope — createOrder only).
 *
 * Maps frontend calls to the backend order operationIds defined in
 * docs/03-openapi-specification.yaml.
 *
 * All order endpoints require authentication (Bearer JWT).
 *
 * operationId mapping:
 *   createOrder → POST /orders
 *
 * Phase-2 / later milestone operationIds (not yet implemented):
 *   listOrders, getOrder, cancelOrder, buyAgain
 */

import { apiPost } from './client'
import type { CreateOrderRequest, OrderResponse } from '../types/api'

/**
 * operationId: createOrder
 * POST /orders — requires authentication
 * Creates an order from the active cart using the given shipping address.
 * Returns the created OrderResponse (id used to navigate to payment).
 */
export function createOrder(body: CreateOrderRequest): Promise<OrderResponse> {
  return apiPost<OrderResponse>('/orders', body)
}
