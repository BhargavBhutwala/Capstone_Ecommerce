/**
 * Unit tests: ProtectedRoute and PublicOnlyRoute.
 *
 * Verifies:
 * - Unauthenticated user is redirected to /login
 * - Intended destination is preserved in location state
 * - Authenticated user accessing /login is redirected away
 * - Bootstrap pending: route renders nothing (null) to avoid flicker
 */

import { useEffect } from 'react'
import type { MutableRefObject } from 'react'
import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import type { Location } from 'react-router-dom'
import { AuthContext, type AuthContextValue } from '../features/auth/AuthContext'
import { ProtectedRoute } from '../routes/ProtectedRoute'
import { PublicOnlyRoute } from '../routes/PublicOnlyRoute'
import type { UserResponse } from '../types/api'

// ── Test user fixture ─────────────────────────────────────────────────────────

const testUser: UserResponse = {
  id: 1,
  firstName: 'Alice',
  lastName: 'Smith',
  email: 'alice@example.com',
  role: 'CUSTOMER',
  status: 'ACTIVE',
}

// ── Auth context factory ───────────────────────────────────────────────────────

function makeAuthValue(overrides: Partial<AuthContextValue> = {}): AuthContextValue {
  return {
    user: null,
    bootstrapping: false,
    loading: false,
    error: null,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    clearError: vi.fn(),
    ...overrides,
  }
}

// ── Helper: capture location state ────────────────────────────────────────────
// Uses a mutable ref (React-safe) so render tests can inspect location.state.
// Must end in "Ref" per the eslint-plugin-react-hooks/immutability rule.

function LocationCapture({ captureRef }: { captureRef: MutableRefObject<Location | null> }) {
  const loc = useLocation()
  useEffect(() => {
    captureRef.current = loc
  }, [loc, captureRef])
  return null
}

// ─────────────────────────────────────────────────────────────────────────────
// ProtectedRoute
// ─────────────────────────────────────────────────────────────────────────────

describe('ProtectedRoute', () => {
  it('redirects unauthenticated user to /login', () => {
    render(
      <AuthContext.Provider value={makeAuthValue({ user: null })}>
        <MemoryRouter initialEntries={['/cart']}>
          <Routes>
            <Route element={<ProtectedRoute />}>
              <Route path="/cart" element={<div>Cart</div>} />
            </Route>
            <Route path="/login" element={<div>Login page</div>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )
    expect(screen.getByText('Login page')).toBeInTheDocument()
    expect(screen.queryByText('Cart')).not.toBeInTheDocument()
  })

  it('preserves intended destination in location state', () => {
    const captureRef = { current: null as Location | null }

    render(
      <AuthContext.Provider value={makeAuthValue({ user: null })}>
        <MemoryRouter initialEntries={['/cart']}>
          <Routes>
            <Route element={<ProtectedRoute />}>
              <Route path="/cart" element={<div>Cart</div>} />
            </Route>
            <Route
              path="/login"
              element={<LocationCapture captureRef={captureRef as MutableRefObject<Location | null>} />}
            />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )

    // The redirect sets state.from.pathname to /cart
    const state = captureRef.current?.state as { from?: { pathname: string } } | null
    expect(state?.from?.pathname).toBe('/cart')
  })

  it('renders outlet when user is authenticated', () => {
    render(
      <AuthContext.Provider value={makeAuthValue({ user: testUser })}>
        <MemoryRouter initialEntries={['/cart']}>
          <Routes>
            <Route element={<ProtectedRoute />}>
              <Route path="/cart" element={<div>Cart page</div>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )
    expect(screen.getByText('Cart page')).toBeInTheDocument()
  })

  it('renders nothing while bootstrapping', () => {
    const { container } = render(
      <AuthContext.Provider value={makeAuthValue({ user: null, bootstrapping: true })}>
        <MemoryRouter initialEntries={['/cart']}>
          <Routes>
            <Route element={<ProtectedRoute />}>
              <Route path="/cart" element={<div>Cart page</div>} />
            </Route>
            <Route path="/login" element={<div>Login page</div>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )
    // Should render nothing (no cart, no login) while bootstrapping
    expect(container.firstChild).toBeNull()
  })
})

// ─────────────────────────────────────────────────────────────────────────────
// PublicOnlyRoute
// ─────────────────────────────────────────────────────────────────────────────

describe('PublicOnlyRoute', () => {
  it('renders outlet when user is NOT authenticated', () => {
    render(
      <AuthContext.Provider value={makeAuthValue({ user: null })}>
        <MemoryRouter initialEntries={['/login']}>
          <Routes>
            <Route element={<PublicOnlyRoute />}>
              <Route path="/login" element={<div>Login form</div>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )
    expect(screen.getByText('Login form')).toBeInTheDocument()
  })

  it('redirects authenticated user away from /login to home', () => {
    render(
      <AuthContext.Provider value={makeAuthValue({ user: testUser })}>
        <MemoryRouter initialEntries={['/login']}>
          <Routes>
            <Route element={<PublicOnlyRoute />}>
              <Route path="/login" element={<div>Login form</div>} />
            </Route>
            <Route path="/" element={<div>Home page</div>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )
    expect(screen.getByText('Home page')).toBeInTheDocument()
    expect(screen.queryByText('Login form')).not.toBeInTheDocument()
  })

  it('redirects authenticated user to intended destination when state.from is set', () => {
    render(
      <AuthContext.Provider value={makeAuthValue({ user: testUser })}>
        <MemoryRouter
          initialEntries={[
            { pathname: '/login', state: { from: { pathname: '/cart' } } },
          ]}
        >
          <Routes>
            <Route element={<PublicOnlyRoute />}>
              <Route path="/login" element={<div>Login form</div>} />
            </Route>
            <Route path="/cart" element={<div>Cart page</div>} />
            <Route path="/" element={<div>Home page</div>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )
    expect(screen.getByText('Cart page')).toBeInTheDocument()
  })

  it('renders nothing while bootstrapping', () => {
    const { container } = render(
      <AuthContext.Provider value={makeAuthValue({ user: null, bootstrapping: true })}>
        <MemoryRouter initialEntries={['/login']}>
          <Routes>
            <Route element={<PublicOnlyRoute />}>
              <Route path="/login" element={<div>Login form</div>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )
    expect(container.firstChild).toBeNull()
  })
})
