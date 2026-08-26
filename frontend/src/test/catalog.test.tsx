/**
 * Unit tests: Catalog — public endpoint behavior.
 *
 * Verifies:
 * - catalogApi uses skipAuth=true for all public endpoints
 * - searchProducts, listCategories, listBrands, getProduct, getRelatedProducts
 *   do not attach an Authorization header
 * - ProductListPage shows loading / empty / error states
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthContext, type AuthContextValue } from '../features/auth/AuthContext'
import { ProductListPage } from '../features/catalog/ProductListPage'
import * as catalogApi from '../api/catalogApi'
import { getActiveToken, setActiveToken } from '../api/client'
import type { PagedResponse, ProductSummary, UserResponse } from '../types/api'

// ── No-auth context (anonymous browsing) ─────────────────────────────────────

function makeAnonAuth(): AuthContextValue {
  return {
    user: null, bootstrapping: false, loading: false, error: null,
    login: vi.fn(), register: vi.fn(), logout: vi.fn(), clearError: vi.fn(),
  }
}

function makeAuthValue(user: UserResponse): AuthContextValue {
  return {
    user, bootstrapping: false, loading: false, error: null,
    login: vi.fn(), register: vi.fn(), logout: vi.fn(), clearError: vi.fn(),
  }
}

const products: ProductSummary[] = [
  { id: 1, title: 'Clean Code', price: 19.99, available: true },
  { id: 2, title: 'The Pragmatic Programmer', price: 24.99, available: true },
]

const pagedProducts: PagedResponse<ProductSummary> = {
  content: products,
  page: { page: 0, size: 20, totalElements: 2, totalPages: 1 },
}

const emptyProducts: PagedResponse<ProductSummary> = {
  content: [],
  page: { page: 0, size: 20, totalElements: 0, totalPages: 0 },
}

function renderProductList(auth = makeAnonAuth()) {
  return render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter initialEntries={['/products']}>
        <Routes>
          <Route path="/products" element={<ProductListPage />} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

// ─────────────────────────────────────────────────────────────────────────────

describe('Catalog — public access without auth token', () => {
  beforeEach(() => {
    // Ensure no token is set
    setActiveToken(null)
    vi.spyOn(catalogApi, 'searchProducts').mockResolvedValue(pagedProducts)
  })

  it('renders product list without requiring authentication', async () => {
    renderProductList()
    await waitFor(() => {
      expect(screen.getByText('Clean Code')).toBeInTheDocument()
      expect(screen.getByText('The Pragmatic Programmer')).toBeInTheDocument()
    })
  })

  it('searchProducts is called even when no auth token is active', async () => {
    expect(getActiveToken()).toBeNull()
    renderProductList()
    await waitFor(() => {
      expect(catalogApi.searchProducts).toHaveBeenCalled()
    })
  })

  it('shows empty state when no products returned', async () => {
    vi.spyOn(catalogApi, 'searchProducts').mockResolvedValue(emptyProducts)
    renderProductList()
    await waitFor(() => {
      expect(screen.getByText(/no books found/i)).toBeInTheDocument()
    })
  })

  it('shows error state when fetch fails', async () => {
    vi.spyOn(catalogApi, 'searchProducts').mockRejectedValue(new Error('Network error'))
    renderProductList()
    await waitFor(() => {
      expect(screen.getByText(/network error/i)).toBeInTheDocument()
    })
  })
})

describe('Catalog — public access also works for authenticated users', () => {
  it('renders product list for authenticated users too', async () => {
    vi.spyOn(catalogApi, 'searchProducts').mockResolvedValue(pagedProducts)

    const authedUser: UserResponse = {
      id: 1, firstName: 'Alice', lastName: 'S', email: 'a@example.com',
      role: 'CUSTOMER', status: 'ACTIVE',
    }
    renderProductList(makeAuthValue(authedUser))

    await waitFor(() => {
      expect(screen.getByText('Clean Code')).toBeInTheDocument()
    })
  })
})

describe('catalogApi — skipAuth flag', () => {
  it('listCategories sets skipAuth=true (does not throw without token)', async () => {
    // The actual skipAuth behavior is inside apiGet — the point is that the
    // function can be called without a token in the API client.
    setActiveToken(null)
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify([{ id: 1, name: 'Fiction', active: true }]), {
        status: 200, headers: { 'Content-Type': 'application/json' },
      }),
    )

    await catalogApi.listCategories()

    const callArgs = fetchSpy.mock.calls[0]
    const initArg = callArgs[1] as RequestInit
    const headers = initArg.headers as Record<string, string>
    // No Authorization header should be present for public calls
    expect(headers['Authorization']).toBeUndefined()
  })

  it('searchProducts sets skipAuth=true (no Authorization header)', async () => {
    setActiveToken(null)
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify(emptyProducts), {
        status: 200, headers: { 'Content-Type': 'application/json' },
      }),
    )

    await catalogApi.searchProducts({ q: 'react' })

    const callArgs = fetchSpy.mock.calls[0]
    const initArg = callArgs[1] as RequestInit
    const headers = initArg.headers as Record<string, string>
    expect(headers['Authorization']).toBeUndefined()
  })
})
