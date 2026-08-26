/**
 * ProductListPage — searchProducts with URL-driven filter/sort/page state.
 *
 * URL parameters (all optional, matching backend contract exactly):
 *   q, categoryId, brandId, minPrice, maxPrice, availableOnly, page, size, sort
 *
 * operationId: searchProducts → GET /products
 * All supported sort values use Spring-style "field,direction" e.g. "title,asc".
 */

import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import * as catalogApi from '../../api/catalogApi'
import { useAsync } from '../../hooks/useAsync'
import { useAddToCart } from '../../hooks/useAddToCart'
import { LoadingSpinner } from '../../components/states/LoadingSpinner'
import { ErrorState } from '../../components/states/ErrorState'
import { EmptyState } from '../../components/states/EmptyState'
import { ProductGrid } from '../../components/ui/ProductGrid'
import { Pagination } from '../../components/ui/Pagination'
import styles from './ProductListPage.module.css'

const SORT_OPTIONS = [
  { value: 'title,asc', label: 'Title A → Z' },
  { value: 'title,desc', label: 'Title Z → A' },
  { value: 'price,asc', label: 'Price: low to high' },
  { value: 'price,desc', label: 'Price: high to low' },
]

const DEFAULT_SIZE = 20

export function ProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams()

  const {
    addToCart,
    isAdding,
    getError,
  } = useAddToCart()

  // ── Derive current filter/page state from URL ─────────────────────────────

  const q = searchParams.get('q') ?? ''

  const categoryId = searchParams.get('categoryId')
    ? Number(searchParams.get('categoryId'))
    : undefined

  const brandId = searchParams.get('brandId')
    ? Number(searchParams.get('brandId'))
    : undefined

  const minPrice = searchParams.get('minPrice')
    ? Number(searchParams.get('minPrice'))
    : undefined

  const maxPrice = searchParams.get('maxPrice')
    ? Number(searchParams.get('maxPrice'))
    : undefined

  const availableOnly =
    searchParams.get('availableOnly') !== 'false'

  const page = searchParams.get('page')
    ? Number(searchParams.get('page'))
    : 0

  const sort =
    searchParams.get('sort') ?? 'title,asc'

  // ── Local controlled filter state ─────────────────────────────────────────

  const [draftQ, setDraftQ] = useState(q)

  const [draftMinPrice, setDraftMinPrice] = useState(
    minPrice !== undefined ? String(minPrice) : '',
  )

  const [draftMaxPrice, setDraftMaxPrice] = useState(
    maxPrice !== undefined ? String(maxPrice) : '',
  )

  const [draftAvailableOnly, setDraftAvailableOnly] =
    useState(availableOnly)

  // Keep search draft in sync when URL changes, e.g. browser back/forward
  const prevQ = useRef(q)

  useEffect(() => {
    if (q !== prevQ.current) {
      setDraftQ(q)
      prevQ.current = q
    }
  }, [q])

  // ── Data ──────────────────────────────────────────────────────────────────

  const productsState = useAsync(
    () =>
      catalogApi.searchProducts({
        q: q || undefined,
        categoryId,
        brandId,
        minPrice,
        maxPrice,
        availableOnly,
        page,
        size: DEFAULT_SIZE,
        sort,
      }),
    [
      q,
      categoryId,
      brandId,
      minPrice,
      maxPrice,
      availableOnly,
      page,
      sort,
    ],
  )

  // ── URL update helpers ────────────────────────────────────────────────────

  function applyFilters() {
    const next = new URLSearchParams()

    if (draftQ.trim()) {
      next.set('q', draftQ.trim())
    }

    if (categoryId !== undefined) {
      next.set('categoryId', String(categoryId))
    }

    if (brandId !== undefined) {
      next.set('brandId', String(brandId))
    }

    if (draftMinPrice) {
      next.set('minPrice', draftMinPrice)
    }

    if (draftMaxPrice) {
      next.set('maxPrice', draftMaxPrice)
    }

    if (!draftAvailableOnly) {
      next.set('availableOnly', 'false')
    }

    if (sort !== 'title,asc') {
      next.set('sort', sort)
    }

    next.set('page', '0')

    setSearchParams(next, { replace: true })
  }

  function handleSortChange(newSort: string) {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev)

        next.set('sort', newSort)
        next.set('page', '0')

        return next
      },
      { replace: true },
    )
  }

  function handlePageChange(newPage: number) {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev)

        next.set('page', String(newPage))

        return next
      },
      { replace: true },
    )

    window.scrollTo({
      top: 0,
      behavior: 'smooth',
    })
  }

  function clearFilters() {
    setDraftQ('')
    setDraftMinPrice('')
    setDraftMaxPrice('')
    setDraftAvailableOnly(true)

    setSearchParams({}, { replace: true })
  }

  const hasActiveFilters =
    Boolean(q) ||
    categoryId !== undefined ||
    brandId !== undefined ||
    minPrice !== undefined ||
    maxPrice !== undefined ||
    !availableOnly

  const {
    data: result,
    loading,
    error,
    reload,
  } = productsState

  return (
    <div className={styles.layout}>
      {/* ── Sidebar filters ── */}
      <aside className={styles.sidebar}>
        <h2 className={styles.filterTitle}>
          Filters
        </h2>

        <div className={styles.filterGroup}>
          <label
            htmlFor="product-search"
            className={styles.filterLabel}
          >
            Search
          </label>

          <input
            id="product-search"
            type="search"
            value={draftQ}
            onChange={(event) =>
              setDraftQ(event.target.value)
            }
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                applyFilters()
              }
            }}
            placeholder="Title, keyword…"
            className={styles.filterInput}
            maxLength={255}
          />
        </div>

        <div className={styles.filterGroup}>
          <label
            htmlFor="min-price"
            className={styles.filterLabel}
          >
            Min price ($)
          </label>

          <input
            id="min-price"
            type="number"
            value={draftMinPrice}
            onChange={(event) =>
              setDraftMinPrice(event.target.value)
            }
            min={0}
            step={0.01}
            className={styles.filterInput}
          />
        </div>

        <div className={styles.filterGroup}>
          <label
            htmlFor="max-price"
            className={styles.filterLabel}
          >
            Max price ($)
          </label>

          <input
            id="max-price"
            type="number"
            value={draftMaxPrice}
            onChange={(event) =>
              setDraftMaxPrice(event.target.value)
            }
            min={0}
            step={0.01}
            className={styles.filterInput}
          />
        </div>

        <div className={styles.filterGroup}>
          <label className={styles.checkLabel}>
            <input
              type="checkbox"
              checked={draftAvailableOnly}
              onChange={(event) =>
                setDraftAvailableOnly(
                  event.target.checked,
                )
              }
            />

            In stock only
          </label>
        </div>

        <button
          type="button"
          className={styles.applyBtn}
          onClick={applyFilters}
        >
          Apply filters
        </button>

        {hasActiveFilters && (
          <button
            type="button"
            className={styles.clearBtn}
            onClick={clearFilters}
          >
            Clear all
          </button>
        )}

        {(categoryId !== undefined ||
          brandId !== undefined) && (
          <>
            <hr className={styles.divider} />

            {categoryId !== undefined && (
              <p className={styles.activeFilter}>
                Category filter active.{' '}

                <Link
                  to="/products"
                  className={styles.removeLink}
                >
                  Remove
                </Link>
              </p>
            )}

            {brandId !== undefined && (
              <p className={styles.activeFilter}>
                Brand filter active.{' '}

                <Link
                  to="/products"
                  className={styles.removeLink}
                >
                  Remove
                </Link>
              </p>
            )}
          </>
        )}
      </aside>

      {/* ── Main results ── */}
      <main className={styles.results}>
        <div className={styles.toolbar}>
          <div className={styles.headingGroup}>
            <h1 className={styles.pageTitle}>
              {q
                ? `Results for "${q}"`
                : 'All books'}
            </h1>

            {!loading && !error && result && (
              <p className={styles.resultCount}>
                {result.page.totalElements}{' '}
                {result.page.totalElements === 1
                  ? 'book'
                  : 'books'}{' '}
                found
              </p>
            )}
          </div>

          <div className={styles.sortGroup}>
            <label
              htmlFor="product-sort"
              className={styles.sortLabel}
            >
              Sort by
            </label>

            <select
              id="product-sort"
              value={sort}
              onChange={(event) =>
                handleSortChange(event.target.value)
              }
              className={styles.sortSelect}
              aria-label="Sort products"
            >
              {SORT_OPTIONS.map((option) => (
                <option
                  key={option.value}
                  value={option.value}
                >
                  {option.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        {loading && (
          <div className={styles.stateArea}>
            <LoadingSpinner label="Loading books…" />
          </div>
        )}

        {!loading && error && (
          <div className={styles.stateArea}>
            <ErrorState
              message={error}
              onRetry={reload}
            />
          </div>
        )}

        {!loading &&
          !error &&
          result?.content.length === 0 && (
            <div className={styles.stateArea}>
              <EmptyState
                message="No books found."
                hint="Try adjusting your search or filters."
              />
            </div>
          )}

        {!loading &&
          !error &&
          result &&
          result.content.length > 0 && (
            <>
              <div className={styles.productArea}>
                <ProductGrid
                  products={result.content}
                  onAddToCart={addToCart}
                  isAdding={isAdding}
                  getAddToCartError={getError}
                />
              </div>

              <Pagination
                page={result.page.page}
                totalPages={result.page.totalPages}
                totalElements={
                  result.page.totalElements
                }
                onPageChange={handlePageChange}
              />
            </>
          )}
      </main>
    </div>
  )
}