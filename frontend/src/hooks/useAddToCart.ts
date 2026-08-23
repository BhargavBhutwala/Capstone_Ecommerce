/**
 * useAddToCart — shared hook for Add-to-Cart behavior.
 *
 * Encapsulates:
 * - Authentication check: redirects unauthenticated users to /login,
 *   preserving the current location so login can return to it.
 * - POST /cart/items via cartApi.addCartItem
 * - Per-product loading and error state
 * - 409 conflict / other API error surfacing
 *
 * Used by ProductCard (via ProductGrid) and ProductDetailPage.
 *
 * Design:
 * - Does NOT maintain a local cart copy. The CartPage reads the cart directly.
 * - Callers receive a success callback to react to (e.g. show a toast or navigate).
 */

import { useCallback, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import * as cartApi from '../api/cartApi'
import { ApiError } from '../api/client'

interface UseAddToCartOptions {
  /** Called after a successful add-to-cart API response */
  onSuccess?: () => void
}

export interface UseAddToCartReturn {
  /**
   * Initiate an add-to-cart for the given productId with the given quantity.
   * Handles unauthenticated redirect automatically.
   * @param onSuccess Optional callback invoked immediately after a successful add.
   */
  addToCart: (productId: number, quantity?: number, onSuccess?: () => void) => Promise<void>
  /** True while the API request for a specific productId is in flight */
  isAdding: (productId: number) => boolean
  /** Per-product error message (e.g. 409 conflict) */
  getError: (productId: number) => string | null
  /** Clear the error for a specific product */
  clearError: (productId: number) => void
}

export function useAddToCart(options: UseAddToCartOptions = {}): UseAddToCartReturn {
  const { user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  // Per-product loading state
  const [loading, setLoading] = useState<Record<number, boolean>>({})
  // Per-product error state
  const [errors, setErrors] = useState<Record<number, string>>({})

  const addToCart = useCallback(
    async (productId: number, quantity = 1, onSuccess?: () => void) => {
      // ── Unauthenticated: redirect to login preserving current page ──────────
      if (!user) {
        navigate('/login', { state: { from: location }, replace: false })
        return
      }

      // Clear any previous error for this product
      setErrors((prev) => {
        const next = { ...prev }
        delete next[productId]
        return next
      })

      setLoading((prev) => ({ ...prev, [productId]: true }))
      try {
        await cartApi.addCartItem({ productId, quantity })
        options.onSuccess?.()
        onSuccess?.()
      } catch (err) {
        let message = 'Failed to add to cart.'
        if (err instanceof ApiError) {
          message = err.message
        } else if (err instanceof Error) {
          message = err.message
        }
        setErrors((prev) => ({ ...prev, [productId]: message }))
      } finally {
        setLoading((prev) => ({ ...prev, [productId]: false }))
      }
    },
    [user, navigate, location, options],
  )

  const isAdding = useCallback(
    (productId: number) => loading[productId] === true,
    [loading],
  )

  const getError = useCallback(
    (productId: number) => errors[productId] ?? null,
    [errors],
  )

  const clearError = useCallback((productId: number) => {
    setErrors((prev) => {
      const next = { ...prev }
      delete next[productId]
      return next
    })
  }, [])

  return { addToCart, isAdding, getError, clearError }
}
