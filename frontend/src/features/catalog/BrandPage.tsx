/**
 * BrandPage — product listing filtered to a single brand/publisher.
 *
 * operationId: getProductsByBrand → GET /brands/{brandId}/products
 */

import { useSearchParams, useParams, Link } from 'react-router-dom'
import * as catalogApi from '../../api/catalogApi'
import { useAsync } from '../../hooks/useAsync'
import { useAddToCart } from '../../hooks/useAddToCart'
import { LoadingSpinner } from '../../components/states/LoadingSpinner'
import { ErrorState } from '../../components/states/ErrorState'
import { EmptyState } from '../../components/states/EmptyState'
import { ProductGrid } from '../../components/ui/ProductGrid'
import { Pagination } from '../../components/ui/Pagination'
import styles from './BrandPage.module.css'

export function BrandPage() {
  const { brandId } = useParams<{ brandId: string }>()
  const [searchParams, setSearchParams] = useSearchParams()
  const { addToCart, isAdding, getError } = useAddToCart()

  const id = Number(brandId)
  const page = searchParams.get('page') ? Number(searchParams.get('page')) : 0
  const sort = searchParams.get('sort') ?? 'title,asc'

  const brandsState = useAsync(() => catalogApi.listBrands(), [])

  const productsState = useAsync(
    () => catalogApi.getProductsByBrand(id, { page, sort }),
    [id, page, sort],
  )

  const brandName = brandsState.data?.find((b) => b.id === id)?.name

  function handlePageChange(newPage: number) {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev)
        next.set('page', String(newPage))
        return next
      },
      { replace: true },
    )
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  return (
    <div className={styles.page}>
      <nav className={styles.breadcrumb} aria-label="Breadcrumb">
        <Link to="/">Home</Link>
        <span> / </span>
        <Link to="/products">Products</Link>
        <span> / </span>
        <span>{brandName ?? `Brand ${id}`}</span>
      </nav>

      <h1 className={styles.title}>{brandName ?? `Brand ${id}`}</h1>

      {productsState.loading && <LoadingSpinner label="Loading products…" />}
      {!productsState.loading && productsState.error && (
        <ErrorState message={productsState.error} onRetry={productsState.reload} />
      )}
      {!productsState.loading && !productsState.error && productsState.data?.content.length === 0 && (
        <EmptyState message="No products from this brand." hint="Try browsing other brands." />
      )}
      {!productsState.loading && !productsState.error && productsState.data && productsState.data.content.length > 0 && (
        <>
          <ProductGrid
            products={productsState.data.content}
            onAddToCart={addToCart}
            isAdding={isAdding}
            getAddToCartError={getError}
          />
          <Pagination
            page={productsState.data.page.page}
            totalPages={productsState.data.page.totalPages}
            totalElements={productsState.data.page.totalElements}
            onPageChange={handlePageChange}
          />
        </>
      )}
    </div>
  )
}
