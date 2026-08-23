/**
 * Unit tests: PaymentPage component.
 *
 * Verifies:
 * - Only CREDIT_CARD and DEBIT_CARD are selectable
 * - Payment amount and status displayed from backend response
 * - Duplicate/invalid payment 409 is surfaced inline
 * - Successful payment shows confirmation
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthContext, type AuthContextValue } from '../features/auth/AuthContext'
import { PaymentPage } from '../features/payment/PaymentPage'
import * as paymentApi from '../api/paymentApi'
import { ApiError } from '../api/client'
import type { PaymentResponse, UserResponse } from '../types/api'

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

const successPayment: PaymentResponse = {
  id: 1, orderId: 42, paymentReference: 'PAY-001',
  paymentMethod: 'CREDIT_CARD', amount: 29.99,
  status: 'SUCCESS', paidAt: '2024-01-15T10:00:00Z',
}

const failedPayment: PaymentResponse = {
  id: 2, orderId: 42, paymentReference: 'PAY-002',
  paymentMethod: 'DEBIT_CARD', amount: 29.99,
  status: 'FAILED', paidAt: null,
}

function renderPayment(orderId = '42') {
  return render(
    <AuthContext.Provider value={makeAuthValue()}>
      <MemoryRouter initialEntries={[`/orders/${orderId}/payment`]}>
        <Routes>
          <Route path="/orders/:orderId/payment" element={<PaymentPage />} />
          <Route path="/orders" element={<div>Orders page</div>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('PaymentPage', () => {
  beforeEach(() => {
    vi.spyOn(paymentApi, 'initiatePayment').mockResolvedValue(successPayment)
  })

  it('renders CREDIT_CARD and DEBIT_CARD radio options', () => {
    renderPayment()
    expect(screen.getByLabelText(/credit card/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/debit card/i)).toBeInTheDocument()
  })

  it('CREDIT_CARD is selected by default', () => {
    renderPayment()
    const creditRadio = screen.getByDisplayValue('CREDIT_CARD')
    expect(creditRadio).toBeChecked()
  })

  it('can switch to DEBIT_CARD', async () => {
    renderPayment()
    await userEvent.click(screen.getByDisplayValue('DEBIT_CARD'))
    expect(screen.getByDisplayValue('DEBIT_CARD')).toBeChecked()
    expect(screen.getByDisplayValue('CREDIT_CARD')).not.toBeChecked()
  })

  it('sends CREDIT_CARD in initiatePayment request', async () => {
    renderPayment()
    await userEvent.click(screen.getByRole('button', { name: /pay now/i }))

    await waitFor(() => {
      expect(paymentApi.initiatePayment).toHaveBeenCalledWith({
        orderId: 42,
        paymentMethod: 'CREDIT_CARD',
      })
    })
  })

  it('sends DEBIT_CARD in initiatePayment request when selected', async () => {
    renderPayment()
    await userEvent.click(screen.getByDisplayValue('DEBIT_CARD'))
    await userEvent.click(screen.getByRole('button', { name: /pay now/i }))

    await waitFor(() => {
      expect(paymentApi.initiatePayment).toHaveBeenCalledWith({
        orderId: 42,
        paymentMethod: 'DEBIT_CARD',
      })
    })
  })

  it('displays payment amount from backend response', async () => {
    renderPayment()
    await userEvent.click(screen.getByRole('button', { name: /pay now/i }))
    await waitFor(() => {
      expect(screen.getByText('$29.99')).toBeInTheDocument()
    })
  })

  it('displays SUCCESS status label after successful payment', async () => {
    renderPayment()
    await userEvent.click(screen.getByRole('button', { name: /pay now/i }))
    await waitFor(() => {
      expect(screen.getByText('Payment successful')).toBeInTheDocument()
    })
  })

  it('displays FAILED status label after failed payment', async () => {
    vi.spyOn(paymentApi, 'initiatePayment').mockResolvedValue(failedPayment)
    renderPayment()
    await userEvent.click(screen.getByRole('button', { name: /pay now/i }))
    await waitFor(() => {
      expect(screen.getByText('Payment failed')).toBeInTheDocument()
    })
  })

  it('surfaces 409 duplicate payment error inline', async () => {
    vi.spyOn(paymentApi, 'initiatePayment').mockRejectedValue(
      new ApiError({
        timestamp: '', status: 409, code: 'DUPLICATE_PAYMENT',
        message: 'Payment already exists for this order.', path: '/api/payments',
      }),
    )
    renderPayment()
    await userEvent.click(screen.getByRole('button', { name: /pay now/i }))
    await waitFor(() => {
      expect(screen.getByText('Payment already exists for this order.')).toBeInTheDocument()
    })
  })

  it('shows invalid order message for non-numeric orderId', () => {
    renderPayment('abc')
    expect(screen.getByText(/invalid order/i)).toBeInTheDocument()
  })
})
