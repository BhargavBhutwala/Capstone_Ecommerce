/**
 * Address API service.
 *
 * Maps frontend calls to the backend address operationIds defined in
 * docs/03-openapi-specification.yaml.
 *
 * All address endpoints require authentication (Bearer JWT).
 *
 * operationId mapping:
 *   listAddresses  → GET    /addresses
 *   createAddress  → POST   /addresses
 *   updateAddress  → PUT    /addresses/{addressId}
 *   deleteAddress  → DELETE /addresses/{addressId}
 */

import { apiDelete, apiGet, apiPost, apiPut } from './client'
import type { AddressRequest, AddressResponse } from '../types/api'

/**
 * operationId: listAddresses
 * GET /addresses — requires authentication
 * Returns the current user's saved addresses.
 */
export function listAddresses(): Promise<AddressResponse[]> {
  return apiGet<AddressResponse[]>('/addresses')
}

/**
 * operationId: createAddress
 * POST /addresses — requires authentication
 * Creates a new saved address. Returns the created AddressResponse.
 */
export function createAddress(body: AddressRequest): Promise<AddressResponse> {
  return apiPost<AddressResponse>('/addresses', body)
}

/**
 * operationId: updateAddress
 * PUT /addresses/{addressId} — requires authentication
 * Replaces the entire address with the given body. Returns updated AddressResponse.
 */
export function updateAddress(
  addressId: number,
  body: AddressRequest,
): Promise<AddressResponse> {
  return apiPut<AddressResponse>(`/addresses/${addressId}`, body)
}

/**
 * operationId: deleteAddress
 * DELETE /addresses/{addressId} — requires authentication
 * Returns 204 No Content.
 */
export function deleteAddress(addressId: number): Promise<void> {
  return apiDelete<void>(`/addresses/${addressId}`)
}
