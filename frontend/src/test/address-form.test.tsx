/**
 * Unit tests: AddressForm validation.
 *
 * Verifies:
 * - Required fields show validation errors when blank
 * - postalCode minimum length validation
 * - country minimum length validation
 * - Valid data calls onSubmit with correct AddressRequest shape
 */

import { describe, it, expect, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AddressForm } from '../features/address/AddressForm'

function renderForm(onSubmit = vi.fn().mockResolvedValue(undefined)) {
  const onCancel = vi.fn()
  render(
    <AddressForm
      onSubmit={onSubmit}
      onCancel={onCancel}
      submitting={false}
    />,
  )
  return { onSubmit, onCancel }
}

describe('AddressForm validation', () => {
  it('shows error when addressLine1 is blank', async () => {
    renderForm()
    await userEvent.click(screen.getByRole('button', { name: /add address/i }))
    await waitFor(() => {
      expect(screen.getByText('Address line 1 is required.')).toBeInTheDocument()
    })
  })

  it('shows error when city is blank', async () => {
    renderForm()
    await userEvent.type(screen.getByLabelText(/address line 1/i), '123 Main St')
    await userEvent.click(screen.getByRole('button', { name: /add address/i }))
    await waitFor(() => {
      expect(screen.getByText('City is required.')).toBeInTheDocument()
    })
  })

  it('shows error when state is blank', async () => {
    renderForm()
    await userEvent.type(screen.getByLabelText(/address line 1/i), '123 Main St')
    await userEvent.type(screen.getByLabelText(/^city$/i), 'Springfield')
    await userEvent.click(screen.getByRole('button', { name: /add address/i }))
    await waitFor(() => {
      expect(screen.getByText('State / region is required.')).toBeInTheDocument()
    })
  })

  it('shows error when postalCode is too short', async () => {
    renderForm()
    await userEvent.type(screen.getByLabelText(/address line 1/i), '123 Main St')
    await userEvent.type(screen.getByLabelText(/^city$/i), 'Springfield')
    await userEvent.type(screen.getByLabelText(/state/i), 'IL')
    await userEvent.type(screen.getByLabelText(/postal code/i), 'AB')  // < 3 chars
    await userEvent.type(screen.getByLabelText(/country/i), 'USA')
    await userEvent.click(screen.getByRole('button', { name: /add address/i }))
    await waitFor(() => {
      expect(screen.getByText(/at least 3 characters/i)).toBeInTheDocument()
    })
  })

  it('shows error when country is too short', async () => {
    renderForm()
    await userEvent.type(screen.getByLabelText(/address line 1/i), '123 Main St')
    await userEvent.type(screen.getByLabelText(/^city$/i), 'Springfield')
    await userEvent.type(screen.getByLabelText(/state/i), 'IL')
    await userEvent.type(screen.getByLabelText(/postal code/i), '62701')
    await userEvent.type(screen.getByLabelText(/country/i), 'U')  // < 2 chars
    await userEvent.click(screen.getByRole('button', { name: /add address/i }))
    await waitFor(() => {
      expect(screen.getByText(/at least 2 characters/i)).toBeInTheDocument()
    })
  })

  it('calls onSubmit with correct AddressRequest when form is valid', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    renderForm(onSubmit)

    await userEvent.type(screen.getByLabelText(/address line 1/i), '123 Main St')
    await userEvent.type(screen.getByLabelText(/^city$/i), 'Springfield')
    await userEvent.type(screen.getByLabelText(/state/i), 'IL')
    await userEvent.type(screen.getByLabelText(/postal code/i), '62701')
    await userEvent.type(screen.getByLabelText(/country/i), 'USA')
    await userEvent.click(screen.getByRole('button', { name: /add address/i }))

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledOnce()
    })

    const arg = onSubmit.mock.calls[0][0]
    expect(arg.addressLine1).toBe('123 Main St')
    expect(arg.city).toBe('Springfield')
    expect(arg.state).toBe('IL')
    expect(arg.postalCode).toBe('62701')
    expect(arg.country).toBe('USA')
    // isDefault defaults to false
    expect(arg.isDefault).toBe(false)
  })

  it('does not call onSubmit when validation fails', async () => {
    const onSubmit = vi.fn()
    renderForm(onSubmit)
    await userEvent.click(screen.getByRole('button', { name: /add address/i }))
    await waitFor(() => {
      expect(screen.getByText('Address line 1 is required.')).toBeInTheDocument()
    })
    expect(onSubmit).not.toHaveBeenCalled()
  })
})
