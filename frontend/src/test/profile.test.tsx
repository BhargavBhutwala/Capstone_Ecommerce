/**
 * Unit tests: ProfilePage component.
 *
 * Verifies:
 * - Renders user first name, last name, email, role, and status
 * - Renders createdAt in IST (includes "IST") when present
 * - Renders the "Saved Addresses" section heading
 * - Address list renders inside the profile (via AddressesPage)
 * - Add address / edit / delete interactions delegated to AddressesPage
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AuthContext, type AuthContextValue } from '../features/auth/AuthContext'
import { ProfilePage } from '../features/profile/ProfilePage'
import * as addressApi from '../api/addressApi'
import type { AddressResponse, UserResponse } from '../types/api'

// ── Fixtures ──────────────────────────────────────────────────────────────────

const testUser: UserResponse = {
  id: 1,
  firstName: 'Priya',
  lastName: 'Sharma',
  email: 'priya@example.com',
  role: 'CUSTOMER',
  status: 'ACTIVE',
  createdAt: '2024-03-10T08:00:00Z',
}

const testAddress: AddressResponse = {
  id: 10,
  label: 'Home',
  addressLine1: '42 MG Road',
  city: 'Bengaluru',
  state: 'Karnataka',
  postalCode: '560001',
  country: 'India',
  isDefault: true,
}

function makeAuthValue(user: UserResponse | null = testUser): AuthContextValue {
  return {
    user,
    bootstrapping: false,
    loading: false,
    error: null,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    clearError: vi.fn(),
  }
}

function renderProfile(user: UserResponse | null = testUser) {
  return render(
    <AuthContext.Provider value={makeAuthValue(user)}>
      <MemoryRouter>
        <ProfilePage />
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

// ─────────────────────────────────────────────────────────────────────────────

describe('ProfilePage — user information', () => {
  beforeEach(() => {
    vi.spyOn(addressApi, 'listAddresses').mockResolvedValue([])
  })

  it('renders user first name', () => {
    renderProfile()
    // "Priya Sharma" is a single text node; use exact match to avoid collision with email
    expect(screen.getByText('Priya Sharma')).toBeInTheDocument()
  })

  it('renders user last name', () => {
    renderProfile()
    expect(screen.getByText(/sharma/i)).toBeInTheDocument()
  })

  it('renders user email', () => {
    renderProfile()
    expect(screen.getByText('priya@example.com')).toBeInTheDocument()
  })

  it('renders user role', () => {
    renderProfile()
    expect(screen.getByText('CUSTOMER')).toBeInTheDocument()
  })

  it('renders account status badge', () => {
    renderProfile()
    expect(screen.getByText('ACTIVE')).toBeInTheDocument()
  })

  it('renders createdAt with IST suffix when present', () => {
    renderProfile()
    // formatDateTime appends " IST"
    expect(screen.getByText(/IST/)).toBeInTheDocument()
  })

  it('does not render member since row when createdAt is absent', () => {
    const userWithoutDate: UserResponse = { ...testUser, createdAt: undefined }
    renderProfile(userWithoutDate)
    expect(screen.queryByText(/member since/i)).not.toBeInTheDocument()
  })

  it('renders nothing when user is null', () => {
    const { container } = renderProfile(null)
    expect(container.firstChild).toBeNull()
  })
})

describe('ProfilePage — address section', () => {
  it('renders "Saved Addresses" section heading', async () => {
    vi.spyOn(addressApi, 'listAddresses').mockResolvedValue([])
    renderProfile()
    expect(screen.getByRole('heading', { name: /saved addresses/i })).toBeInTheDocument()
  })

  it('renders an address from the API inside the profile', async () => {
    vi.spyOn(addressApi, 'listAddresses').mockResolvedValue([testAddress])
    renderProfile()
    await waitFor(() => {
      expect(screen.getByText('42 MG Road')).toBeInTheDocument()
    })
    expect(screen.getByText('Bengaluru, Karnataka 560001')).toBeInTheDocument()
  })

  it('renders empty state message when no addresses', async () => {
    vi.spyOn(addressApi, 'listAddresses').mockResolvedValue([])
    renderProfile()
    await waitFor(() => {
      expect(screen.getByText(/no saved addresses yet/i)).toBeInTheDocument()
    })
  })

  it('renders Add address button', async () => {
    vi.spyOn(addressApi, 'listAddresses').mockResolvedValue([])
    renderProfile()
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /add address/i })).toBeInTheDocument()
    })
  })
})
