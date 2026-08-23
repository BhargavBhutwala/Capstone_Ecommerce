/**
 * CategoryPage — product listing filtered to a single category.
 *
 * operationId: getProductsByCategory → GET /categories/{categoryId}/products
 * URL params: page, sort preserved in URL for back/refresh support.
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
import styles from './CategoryPage.module.css'

export function CategoryPage() {
  const { categoryId } = useParams<{ categoryId: string }>()
  const [searchParams, setSearchParams] = useSearchParams()
  const { addToCart, isAdding, getError } = useAddToCart()

  const id = Number(categoryId)
  const page = searchParams.get('page') ? Number(searchParams.get('page')) : 0
  const sort = searchParams.get('sort') ?? 'title,asc'

  // Fetch the category name from the full list to use as page title
  const categoriesState = useAsync(() => catalogApi.listCategories(), [])

  const productsState = useAsync(
    () => catalogApi.getProductsByCategory(id, { page, sort }),
    [id, page, sort],
  )

  const categoryName = categoriesState.data?.find((c) => c.id === id)?.name

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
        <span>{categoryName ?? `Category ${id}`}</span>
      </nav>

      <h1 className={styles.title}>{categoryName ?? `Category ${id}`}</h1>

      {productsState.loading && <LoadingSpinner label="Loading products…" />}
      {!productsState.loading && productsState.error && (
        <ErrorState message={productsState.error} onRetry={productsState.reload} />
      )}
      {!productsState.loading && !productsState.error && productsState.data?.content.length === 0 && (
        <EmptyState message="No products in this category." hint="Try browsing other categories." />
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
