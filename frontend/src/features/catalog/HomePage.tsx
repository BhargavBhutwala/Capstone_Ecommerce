/**
 * HomePage — public catalog landing page.
 *
 * Shows:
 * - Search bar → navigates to /products with ?q=
 * - Categories grid → links to /categories/:id
 * - Brands grid → links to /brands/:id
 *
 * All data is public (security: []). No auth required.
 */

import { type FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import * as catalogApi from '../../api/catalogApi'
import { useAsync } from '../../hooks/useAsync'
import { LoadingSpinner } from '../../components/states/LoadingSpinner'
import { ErrorState } from '../../components/states/ErrorState'
import styles from './HomePage.module.css'

export function HomePage() {
  const navigate = useNavigate()
  const [query, setQuery] = useState('')

  const categories = useAsync(() => catalogApi.listCategories(), [])
  const brands = useAsync(() => catalogApi.listBrands(), [])

  function handleSearch(e: FormEvent) {
    e.preventDefault()
    const q = query.trim()
    if (q) {
      navigate(`/products?q=${encodeURIComponent(q)}`)
    } else {
      navigate('/products')
    }
  }

  return (
    <div className={styles.page}>
      {/* ── Hero search ── */}
      <section className={styles.hero}>
        <h1 className={styles.heroTitle}>Find your next book</h1>
        <form onSubmit={handleSearch} className={styles.searchForm}>
          <input
            type="search"
            placeholder="Search titles, authors, keywords…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className={styles.searchInput}
            aria-label="Search products"
            maxLength={255}
          />
          <button type="submit" className={styles.searchBtn}>
            Search
          </button>
        </form>
        <p className={styles.heroSub}>
          <Link to="/products">Browse all products</Link>
        </p>
      </section>

      {/* ── Categories ── */}
      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>Browse by category</h2>
        {categories.loading && <LoadingSpinner label="Loading categories…" />}
        {categories.error && (
          <ErrorState message={categories.error} onRetry={categories.reload} />
        )}
        {categories.data && categories.data.length === 0 && (
          <p className={styles.empty}>No categories available.</p>
        )}
        {categories.data && categories.data.length > 0 && (
          <div className={styles.chipGrid}>
            {categories.data.map((cat) => (
              <Link key={cat.id} to={`/categories/${cat.id}`} className={styles.chip}>
                {cat.name}
              </Link>
            ))}
          </div>
        )}
      </section>

      {/* ── Brands ── */}
      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>Browse by brand / publisher</h2>
        {brands.loading && <LoadingSpinner label="Loading brands…" />}
        {brands.error && (
          <ErrorState message={brands.error} onRetry={brands.reload} />
        )}
        {brands.data && brands.data.length === 0 && (
          <p className={styles.empty}>No brands available.</p>
        )}
        {brands.data && brands.data.length > 0 && (
          <div className={styles.chipGrid}>
            {brands.data.map((brand) => (
              <Link key={brand.id} to={`/brands/${brand.id}`} className={styles.chip}>
                {brand.name}
              </Link>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
