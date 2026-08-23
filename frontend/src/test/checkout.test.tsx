/**
 * Unit tests: CheckoutPage component.
 *
 * Verifies:
 * - Selected addressId is sent in CreateOrderRequest
 * - Successful order creation navigates to payment route
 * - 409 checkout conflict is surfaced inline
 * - Empty cart prevents submission
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { AuthContext, type AuthContextValue } from '../features/auth/AuthContext'
import { CheckoutPage } from '../features/checkout/CheckoutPage'
import * as cartApi from '../api/cartApi'
import * as addressApi from '../api/addressApi'
import * as orderApi from '../api/orderApi'
import { ApiError } from '../api/client'
import type { CartResponse, AddressResponse, OrderResponse, UserResponse } from '../types/api'

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

const cartWithItem: CartResponse = {
  id: 1, status: 'ACTIVE', subtotal: 29.99, totalAmount: 29.99,
  items: [{
    id: 10, product: { id: 100, title: 'Domain-Driven Design', price: 29.99, available: true },
    quantity: 1, unitPrice: 29.99, subtotal: 29.99,
  }],
}

const emptyCart: CartResponse = {
  id: 1, status: 'ACTIVE', subtotal: 0, totalAmount: 0, items: [],
}

const addresses: AddressResponse[] = [
  {
    id: 5, addressLine1: '123 Main St', city: 'Springfield',
    state: 'IL', postalCode: '62701', country: 'USA', isDefault: true,
  },
  {
    id: 6, addressLine1: '456 Oak Ave', city: 'Chicago',
    state: 'IL', postalCode: '60601', country: 'USA', isDefault: false,
  },
]

const createdOrder: OrderResponse = {
  id: 99, orderNumber: 'ORD-0099', status: 'PENDING_PAYMENT',
  items: [], subtotal: 29.99, totalAmount: 29.99,
}

function renderCheckout() {
  return render(
    <AuthContext.Provider value={makeAuthValue()}>
      <MemoryRouter initialEntries={['/checkout']}>
        <Routes>
          <Route path="/checkout" element={<CheckoutPage />} />
          <Route path="/orders/:orderId/payment" element={<div>Payment page</div>} />
          <Route path="/products" element={<div>Products page</div>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('CheckoutPage', () => {
  beforeEach(() => {
    vi.spyOn(cartApi, 'getCart').mockResolvedValue(cartWithItem)
    vi.spyOn(addressApi, 'listAddresses').mockResolvedValue(addresses)
    vi.spyOn(orderApi, 'createOrder').mockResolvedValue(createdOrder)
  })

  it('sends selected addressId in CreateOrderRequest', async () => {
    renderCheckout()
    await waitFor(() => screen.getByText('Domain-Driven Design'))

    // Select the second address (id=6)
    const secondAddrRadio = screen.getByRole('radio', { name: /456 oak ave/i })
    await userEvent.click(secondAddrRadio)

    await userEvent.click(screen.getByRole('button', { name: /place order/i }))

    await waitFor(() => {
      expect(orderApi.createOrder).toHaveBeenCalledWith({ addressId: 6 })
    })
  })

  it('navigates to payment route after successful order creation', async () => {
    renderCheckout()
    await waitFor(() => screen.getByText('Domain-Driven Design'))

    await userEvent.click(screen.getByRole('button', { name: /place order/i }))

    await waitFor(() => {
      expect(screen.getByText('Payment page')).toBeInTheDocument()
    })
  })

  it('shows 409 conflict inline without navigating away', async () => {
    vi.spyOn(orderApi, 'createOrder').mockRejectedValue(
      new ApiError({
        timestamp: '', status: 409, code: 'INSUFFICIENT_STOCK',
        message: 'Item is out of stock.', path: '/api/orders',
      }),
    )
    renderCheckout()
    await waitFor(() => screen.getByText('Domain-Driven Design'))

    await userEvent.click(screen.getByRole('button', { name: /place order/i }))

    await waitFor(() => {
      expect(screen.getByText('Item is out of stock.')).toBeInTheDocument()
    })
    // Still on checkout page
    expect(screen.queryByText('Payment page')).not.toBeInTheDocument()
  })

  it('renders empty cart message when cart has no items', async () => {
    vi.spyOn(cartApi, 'getCart').mockResolvedValue(emptyCart)
    renderCheckout()
    await waitFor(() => {
      expect(screen.getByText(/your cart is empty/i)).toBeInTheDocument()
    })
  })

  it('auto-selects default address and uses it for order creation', async () => {
    renderCheckout()
    await waitFor(() => screen.getByText('Domain-Driven Design'))

    // Default address is id=5
    await userEvent.click(screen.getByRole('button', { name: /place order/i }))

    await waitFor(() => {
      expect(orderApi.createOrder).toHaveBeenCalledWith({ addressId: 5 })
    })
  })
})
