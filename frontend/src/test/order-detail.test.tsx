/**
 * Unit tests: OrderDetailPage — Buy Again, cancellation visibility.
 *
 * Verifies:
 * - Cancel button visible for PENDING_PAYMENT and PAID
 * - Cancel button NOT visible for CONFIRMED and CANCELLED
 * - Buy Again success navigates to /cart
 * - 409 cancellation denial is surfaced inline
 * - 409 Buy Again conflict is surfaced inline
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthContext, type AuthContextValue } from '../features/auth/AuthContext'
import { OrderDetailPage } from '../features/orders/OrderDetailPage'
import * as orderApi from '../api/orderApi'
import { ApiError } from '../api/client'
import type { OrderResponse, OrderStatus, UserResponse } from '../types/api'

const testUser: UserResponse = {
  id: 1, firstName: 'Alice', lastName: 'S', email: 'a@example.com',
  role: 'CUSTOMER', status: 'ACTIVE',
}

function makeAuthValue(user = testUser): AuthContextValue {
  return {
    user, bootstrapping: false, loading: false, error: null,
    login: vi.fn(), register: vi.fn(), logout: vi.fn(), clearError: vi.fn(),
  }
}

function makeOrder(status: OrderStatus): OrderResponse {
  return {
    id: 10, orderNumber: 'ORD-0010', status,
    items: [{ id: 1, productId: 100, productTitle: 'Clean Code', quantity: 1, unitPrice: 19.99, subtotal: 19.99 }],
    subtotal: 19.99, totalAmount: 19.99,
  }
}

function renderOrderDetail(orderId = '10') {
  return render(
    <AuthContext.Provider value={makeAuthValue()}>
      <MemoryRouter initialEntries={[`/orders/${orderId}`]}>
        <Routes>
          <Route path="/orders/:orderId" element={<OrderDetailPage />} />
          <Route path="/cart" element={<div>Cart page</div>} />
          <Route path="/orders" element={<div>Orders list</div>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('OrderDetailPage — cancellation visibility', () => {
  it('shows Cancel button for PENDING_PAYMENT', async () => {
    vi.spyOn(orderApi, 'getOrder').mockResolvedValue(makeOrder('PENDING_PAYMENT'))
    renderOrderDetail()
    await waitFor(() => screen.getByText('Clean Code'))
    expect(screen.getByRole('button', { name: /cancel order/i })).toBeInTheDocument()
  })

  it('shows Cancel button for PAID', async () => {
    vi.spyOn(orderApi, 'getOrder').mockResolvedValue(makeOrder('PAID'))
    renderOrderDetail()
    await waitFor(() => screen.getByText('Clean Code'))
    expect(screen.getByRole('button', { name: /cancel order/i })).toBeInTheDocument()
  })

  it('does NOT show Cancel button for CONFIRMED', async () => {
    vi.spyOn(orderApi, 'getOrder').mockResolvedValue(makeOrder('CONFIRMED'))
    renderOrderDetail()
    await waitFor(() => screen.getByText('Clean Code'))
    expect(screen.queryByRole('button', { name: /cancel order/i })).not.toBeInTheDocument()
  })

  it('does NOT show Cancel button for CANCELLED', async () => {
    vi.spyOn(orderApi, 'getOrder').mockResolvedValue(makeOrder('CANCELLED'))
    renderOrderDetail()
    await waitFor(() => screen.getByText('Clean Code'))
    expect(screen.queryByRole('button', { name: /cancel order/i })).not.toBeInTheDocument()
  })
})

describe('OrderDetailPage — Buy Again', () => {
  beforeEach(() => {
    vi.spyOn(orderApi, 'getOrder').mockResolvedValue(makeOrder('DELIVERED'))
    vi.spyOn(orderApi, 'buyAgain').mockResolvedValue({
      id: 1, status: 'ACTIVE', subtotal: 0, totalAmount: 0, items: [],
    })
  })

  it('navigates to /cart on successful Buy Again', async () => {
    renderOrderDetail()
    await waitFor(() => screen.getByText('Clean Code'))

    await userEvent.click(screen.getByRole('button', { name: /buy again/i }))

    await waitFor(() => {
      expect(screen.getByText('Cart page')).toBeInTheDocument()
    })
    expect(orderApi.buyAgain).toHaveBeenCalledWith(10)
  })

  it('surfaces 409 conflict inline for Buy Again', async () => {
    vi.spyOn(orderApi, 'buyAgain').mockRejectedValue(
      new ApiError({
        timestamp: '', status: 409, code: 'PRODUCT_UNAVAILABLE',
        message: 'Some items are no longer available.', path: '/api/orders/10/buy-again',
      }),
    )
    renderOrderDetail()
    await waitFor(() => screen.getByText('Clean Code'))

    await userEvent.click(screen.getByRole('button', { name: /buy again/i }))

    await waitFor(() => {
      expect(screen.getByText('Some items are no longer available.')).toBeInTheDocument()
    })
  })
})

describe('OrderDetailPage — Cancellation action', () => {
  beforeEach(() => {
    vi.spyOn(orderApi, 'getOrder').mockResolvedValue(makeOrder('PENDING_PAYMENT'))
  })

  it('surfaces 409 cancellation denial inline', async () => {
    vi.spyOn(orderApi, 'cancelOrder').mockRejectedValue(
      new ApiError({
        timestamp: '', status: 409, code: 'CANCELLATION_NOT_ALLOWED',
        message: 'Cancellation deadline has passed.', path: '/api/orders/10/cancel',
      }),
    )
    renderOrderDetail()
    await waitFor(() => screen.getByText('Clean Code'))

    await userEvent.click(screen.getByRole('button', { name: /cancel order/i }))

    await waitFor(() => {
      expect(screen.getByText('Cancellation deadline has passed.')).toBeInTheDocument()
    })
  })

  it('updates order status to CANCELLED on successful cancellation', async () => {
    vi.spyOn(orderApi, 'cancelOrder').mockResolvedValue(makeOrder('CANCELLED'))
    renderOrderDetail()
    await waitFor(() => screen.getByText('Clean Code'))

    await userEvent.click(screen.getByRole('button', { name: /cancel order/i }))

    await waitFor(() => {
      // After cancel, overrideOrder is set with CANCELLED status
      expect(screen.queryByRole('button', { name: /cancel order/i })).not.toBeInTheDocument()
    })
  })
})
