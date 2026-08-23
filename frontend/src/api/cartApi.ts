/**
 * Cart API service.
 *
 * Maps frontend calls to the backend cart operationIds defined in
 * docs/03-openapi-specification.yaml.
 *
 * All cart endpoints require authentication (Bearer JWT).
 * The shared API client automatically attaches the Authorization header
 * when a token is active.
 *
 * operationId mapping:
 *   getCart        → GET    /cart
 *   addCartItem    → POST   /cart/items
 *   updateCartItem → PUT    /cart/items/{itemId}
 *   removeCartItem → DELETE /cart/items/{itemId}
 */

import { apiDelete, apiGet, apiPost, apiPut } from './client'
import type {
  AddCartItemRequest,
  CartResponse,
  UpdateCartItemRequest,
} from '../types/api'

/**
 * operationId: getCart
 * GET /cart — requires authentication
 * Returns the current user's active cart.
 */
export function getCart(): Promise<CartResponse> {
  return apiGet<CartResponse>('/cart')
}

/**
 * operationId: addCartItem
 * POST /cart/items — requires authentication
 * Adds a product to the active cart (or increments quantity if already present).
 * Returns the updated CartResponse.
 */
export function addCartItem(body: AddCartItemRequest): Promise<CartResponse> {
  return apiPost<CartResponse>('/cart/items', body)
}

/**
 * operationId: updateCartItem
 * PUT /cart/items/{itemId} — requires authentication
 * Updates the quantity of a specific cart item.
 * Returns the updated CartResponse.
 */
export function updateCartItem(
  itemId: number,
  body: UpdateCartItemRequest,
): Promise<CartResponse> {
  return apiPut<CartResponse>(`/cart/items/${itemId}`, body)
}

/**
 * operationId: removeCartItem
 * DELETE /cart/items/{itemId} — requires authentication
 * Removes a single item from the active cart. Returns 204 No Content.
 */
export function removeCartItem(itemId: number): Promise<void> {
  return apiDelete<void>(`/cart/items/${itemId}`)
}
