/**
 * Unit tests: useAddToCart hook.
 *
 * Verifies:
 * - Authenticated user triggers cartApi.addCartItem
 * - Unauthenticated user is redirected to /login with location.from preserved
 * - 409 conflict error is stored per-product
 * - clearError removes the per-product error
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import type { ReactNode } from 'react'
import { AuthContext, type AuthContextValue } from '../features/auth/AuthContext'
import { useAddToCart } from '../hooks/useAddToCart'
import * as cartApi from '../api/cartApi'
import { ApiError } from '../api/client'
import type { CartResponse, UserResponse } from '../types/api'

// ── Navigation capture mock ────────────────────────────────────────────────────

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>()
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

// ── Fixtures ──────────────────────────────────────────────────────────────────

const testUser: UserResponse = {
  id: 1, firstName: 'Alice', lastName: 'S', email: 'a@example.com',
  role: 'CUSTOMER', status: 'ACTIVE',
}

const cartResponse: CartResponse = {
  id: 1, status: 'ACTIVE', subtotal: 19.99, totalAmount: 19.99,
  items: [{ id: 1, product: { id: 100, title: 'Clean Code', price: 19.99, available: true }, quantity: 1, unitPrice: 19.99, subtotal: 19.99 }],
}

function makeAuthValue(user: UserResponse | null = testUser): AuthContextValue {
  return {
    user, bootstrapping: false, loading: false, error: null,
    login: vi.fn(), register: vi.fn(), logout: vi.fn(), clearError: vi.fn(),
  }
}

function wrapper(user: UserResponse | null = testUser) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return (
      <AuthContext.Provider value={makeAuthValue(user)}>
        <MemoryRouter>{children}</MemoryRouter>
      </AuthContext.Provider>
    )
  }
}

// ─────────────────────────────────────────────────────────────────────────────

describe('useAddToCart', () => {
  beforeEach(() => {
    mockNavigate.mockReset()
    vi.spyOn(cartApi, 'addCartItem').mockResolvedValue(cartResponse)
  })

  it('calls addCartItem with productId and quantity for authenticated user', async () => {
    const { result } = renderHook(() => useAddToCart(), { wrapper: wrapper() })

    await act(async () => {
      await result.current.addToCart(100, 2)
    })

    expect(cartApi.addCartItem).toHaveBeenCalledWith({ productId: 100, quantity: 2 })
  })

  it('redirects unauthenticated user to /login', async () => {
    const { result } = renderHook(() => useAddToCart(), { wrapper: wrapper(null) })

    await act(async () => {
      await result.current.addToCart(100, 1)
    })

    expect(mockNavigate).toHaveBeenCalledWith(
      '/login',
      expect.objectContaining({ state: expect.objectContaining({ from: expect.anything() }) }),
    )
    expect(cartApi.addCartItem).not.toHaveBeenCalled()
  })

  it('stores 409 conflict error per-product', async () => {
    vi.spyOn(cartApi, 'addCartItem').mockRejectedValue(
      new ApiError({
        timestamp: '', status: 409, code: 'INSUFFICIENT_STOCK',
        message: 'Only 1 in stock.', path: '/api/cart/items',
      }),
    )
    const { result } = renderHook(() => useAddToCart(), { wrapper: wrapper() })

    await act(async () => {
      await result.current.addToCart(100, 5)
    })

    expect(result.current.getError(100)).toBe('Only 1 in stock.')
    expect(result.current.isAdding(100)).toBe(false)
  })

  it('clearError removes the per-product error', async () => {
    vi.spyOn(cartApi, 'addCartItem').mockRejectedValue(
      new ApiError({
        timestamp: '', status: 409, code: 'INSUFFICIENT_STOCK',
        message: 'Only 1 in stock.', path: '/api/cart/items',
      }),
    )
    const { result } = renderHook(() => useAddToCart(), { wrapper: wrapper() })

    await act(async () => {
      await result.current.addToCart(100, 5)
    })

    expect(result.current.getError(100)).toBe('Only 1 in stock.')

    act(() => {
      result.current.clearError(100)
    })

    expect(result.current.getError(100)).toBeNull()
  })

  it('invokes onSuccess callback after successful add', async () => {
    const onSuccess = vi.fn()
    const { result } = renderHook(() => useAddToCart(), { wrapper: wrapper() })

    await act(async () => {
      await result.current.addToCart(100, 1, onSuccess)
    })

    expect(onSuccess).toHaveBeenCalledOnce()
  })

  it('isAdding returns false initially', () => {
    const { result } = renderHook(() => useAddToCart(), { wrapper: wrapper() })
    expect(result.current.isAdding(100)).toBe(false)
  })
})
