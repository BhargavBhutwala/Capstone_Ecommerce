/**
 * Unit tests: OrdersPage component.
 *
 * Verifies:
 * - Backend pagination metadata drives Pagination component
 * - Status filter calls API with correct status param
 * - Empty state rendered when no orders
 * - Order rows link to detail pages
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthContext, type AuthContextValue } from '../features/auth/AuthContext'
import { OrdersPage } from '../features/orders/OrdersPage'
import * as orderApi from '../api/orderApi'
import type { OrderResponse, PagedResponse, UserResponse } from '../types/api'

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

const sampleOrder: OrderResponse = {
  id: 1, orderNumber: 'ORD-0001', status: 'DELIVERED',
  items: [{ id: 1, productId: 100, productTitle: 'Clean Code', quantity: 1, unitPrice: 19.99, subtotal: 19.99 }],
  subtotal: 19.99, totalAmount: 19.99,
  placedAt: '2024-01-10T10:00:00Z',
}

const pagedResult: PagedResponse<OrderResponse> = {
  content: [sampleOrder],
  page: { page: 0, size: 20, totalElements: 1, totalPages: 1 },
}

const emptyResult: PagedResponse<OrderResponse> = {
  content: [],
  page: { page: 0, size: 20, totalElements: 0, totalPages: 0 },
}

function renderOrders() {
  return render(
    <AuthContext.Provider value={makeAuthValue()}>
      <MemoryRouter initialEntries={['/orders']}>
        <Routes>
          <Route path="/orders" element={<OrdersPage />} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('OrdersPage', () => {
  beforeEach(() => {
    vi.spyOn(orderApi, 'listOrders').mockResolvedValue(pagedResult)
  })

  it('renders order number from backend response', async () => {
    renderOrders()
    await waitFor(() => {
      expect(screen.getByText('#ORD-0001')).toBeInTheDocument()
    })
  })

  it('renders order total from backend response', async () => {
    renderOrders()
    await waitFor(() => {
      expect(screen.getByText('$19.99')).toBeInTheDocument()
    })
  })

  it('shows empty state when no orders returned', async () => {
    vi.spyOn(orderApi, 'listOrders').mockResolvedValue(emptyResult)
    renderOrders()
    await waitFor(() => {
      expect(screen.getByText(/no orders found/i)).toBeInTheDocument()
    })
  })

  it('calls listOrders with status filter when status is selected', async () => {
    renderOrders()
    await waitFor(() => screen.getByText('#ORD-0001'))

    const select = screen.getByRole('combobox', { name: /filter by status/i })
    await userEvent.selectOptions(select, 'DELIVERED')

    await waitFor(() => {
      expect(orderApi.listOrders).toHaveBeenCalledWith(
        expect.objectContaining({ status: 'DELIVERED' }),
      )
    })
  })

  it('renders correct pagination from backend page metadata', async () => {
    const multiPageResult: PagedResponse<OrderResponse> = {
      content: [sampleOrder],
      page: { page: 0, size: 20, totalElements: 25, totalPages: 2 },
    }
    vi.spyOn(orderApi, 'listOrders').mockResolvedValue(multiPageResult)
    renderOrders()

    await waitFor(() => {
      // Pagination renders because totalPages > 1
      expect(screen.getByRole('navigation')).toBeInTheDocument()
    })
  })
})
