/**
 * ProductDetailPage — full product detail with related products.
 *
 * operationId: getProduct            → GET /products/{productId}
 * operationId: getRelatedProducts    → GET /products/{productId}/related
 *
 * FE-04: includes Add-to-Cart action (POST /cart/items via useAddToCart).
 * - Authenticated users: quantity input + Add to Cart button.
 * - Unauthenticated users: clicking Add to Cart redirects to /login.
 * - 409 conflict and other errors surfaced inline.
 *
 * Handles:
 * - loading state
 * - valid product display (available and unavailable)
 * - 404 / unknown product (ApiError.isNotFound)
 * - general API failure with retry
 * - related products section (empty handled gracefully)
 *
 * Public endpoint — no auth required for the page itself.
 */

import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import * as catalogApi from '../../api/catalogApi'
import { ApiError } from '../../api/client'
import { useAsync } from '../../hooks/useAsync'
import { useAddToCart } from '../../hooks/useAddToCart'
import { LoadingSpinner } from '../../components/states/LoadingSpinner'
import { ErrorState } from '../../components/states/ErrorState'
import { ProductGrid } from '../../components/ui/ProductGrid'
import styles from './ProductDetailPage.module.css'

export function ProductDetailPage() {
  const { productId } = useParams<{ productId: string }>()
  const id = Number(productId)

  const { addToCart, isAdding, getError, clearError } = useAddToCart()

  // Quantity for the "Add to Cart" action on the detail page
  const [quantity, setQuantity] = useState(1)
  // Success feedback message
  const [addedMessage, setAddedMessage] = useState<string | null>(null)

  // Wrap the getProduct call to translate ApiError 404 into a recognisable error
  const productState = useAsync(async () => {
    try {
      return await catalogApi.getProduct(id)
    } catch (err) {
      if (err instanceof ApiError && err.isNotFound) {
        const notFound = new Error('PRODUCT_NOT_FOUND') as Error & { cause?: unknown }
        notFound.cause = err
        throw notFound
      }
      throw err
    }
  }, [id])

  // Related products fetched independently — failures shown inline only
  const relatedState = useAsync(() => catalogApi.getRelatedProducts(id, 8), [id])

  // ── Loading ───────────────────────────────────────────────────────────────
  if (productState.loading) {
    return <LoadingSpinner label="Loading product…" />
  }

  // ── 404 ───────────────────────────────────────────────────────────────────
  if (productState.error === 'PRODUCT_NOT_FOUND') {
    return (
      <div className={styles.notFound}>
        <h1 className={styles.notFoundCode}>404</h1>
        <p className={styles.notFoundMsg}>This product does not exist.</p>
        <Link to="/products" className={styles.backLink}>
          Back to products
        </Link>
      </div>
    )
  }

  // ── General error ─────────────────────────────────────────────────────────
  if (productState.error) {
    return <ErrorState message={productState.error} onRetry={productState.reload} />
  }

  const product = productState.data
  if (!product) return null

  const addToCartError = getError(id)

  async function handleAddToCart() {
    clearError(id)
    setAddedMessage(null)
    await addToCart(id, quantity, () => {
      // onSuccess callback — called by the hook only on a successful API response
      setAddedMessage('Added to cart!')
      setTimeout(() => setAddedMessage(null), 3000)
    })
  }

  return (
    <div className={styles.page}>
      {/* ── Breadcrumb ── */}
      <nav className={styles.breadcrumb} aria-label="Breadcrumb">
        <Link to="/">Home</Link>
        <span> / </span>
        <Link to="/products">Products</Link>
        {product.category && (
          <>
            <span> / </span>
            <Link to={`/categories/${product.category.id}`}>
              {product.category.name}
            </Link>
          </>
        )}
        <span> / </span>
        <span className={styles.breadcrumbCurrent}>{product.title}</span>
      </nav>

      {/* ── Main detail ── */}
      <div className={styles.detail}>
        <div className={styles.info}>
          <h1 className={styles.title}>{product.title}</h1>

          {product.isbn && (
            <p className={styles.isbn}>ISBN: {product.isbn}</p>
          )}

          {product.brand && (
            <p className={styles.metaLine}>
              <span className={styles.metaKey}>Publisher</span>
              <Link to={`/brands/${product.brand.id}`} className={styles.metaLink}>
                {product.brand.name}
              </Link>
            </p>
          )}

          {product.category && (
            <p className={styles.metaLine}>
              <span className={styles.metaKey}>Category</span>
              <Link to={`/categories/${product.category.id}`} className={styles.metaLink}>
                {product.category.name}
              </Link>
            </p>
          )}

          <p className={styles.price}>${product.price.toFixed(2)}</p>

          <p className={product.available ? styles.inStock : styles.outOfStock}>
            {product.available
              ? product.stockQuantity !== undefined
                ? `In stock · ${product.stockQuantity} available`
                : 'In stock'
              : 'Currently unavailable'}
          </p>

          {product.deliveryEstimate && (
            <p className={styles.delivery}>
              Estimated delivery:{' '}
              {product.deliveryEstimate.minDays}–{product.deliveryEstimate.maxDays} business days
            </p>
          )}

          {/* ── Add to Cart action ── */}
          <div className={styles.cartSection}>
            {product.available && (
              <div className={styles.cartControls}>
                <label htmlFor="detail-qty" className={styles.qtyLabel}>
                  Qty:
                </label>
                <input
                  id="detail-qty"
                  type="number"
                  min={1}
                  max={999}
                  value={quantity}
                  onChange={(e) => {
                    const v = parseInt(e.target.value, 10)
                    if (!isNaN(v) && v >= 1 && v <= 999) setQuantity(v)
                  }}
                  className={styles.qtyInput}
                  disabled={isAdding(id)}
                  aria-label="Quantity"
                />
                <button
                  className={styles.addToCartBtn}
                  onClick={handleAddToCart}
                  disabled={isAdding(id)}
                >
                  {isAdding(id) ? 'Adding…' : 'Add to Cart'}
                </button>
              </div>
            )}
            {!product.available && (
              <p className={styles.unavailableNote}>
                This product is currently unavailable and cannot be added to the cart.
              </p>
            )}
            {addedMessage && (
              <p className={styles.addedMsg} role="status">
                {addedMessage}
              </p>
            )}
            {addToCartError && (
              <p className={styles.addToCartError} role="alert">
                {addToCartError}
              </p>
            )}
          </div>
        </div>

        {product.description && (
          <div className={styles.description}>
            <h2 className={styles.descTitle}>Description</h2>
            <p className={styles.descText}>{product.description}</p>
          </div>
        )}
      </div>

      {/* ── Related products ── */}
      <section className={styles.related}>
        <h2 className={styles.relatedTitle}>Related products</h2>
        {relatedState.loading && (
          <LoadingSpinner label="Loading related products…" />
        )}
        {!relatedState.loading && relatedState.error && (
          <p className={styles.relatedNote}>Related products unavailable.</p>
        )}
        {!relatedState.loading && !relatedState.error && relatedState.data?.length === 0 && (
          <p className={styles.relatedNote}>No related products found.</p>
        )}
        {!relatedState.loading && !relatedState.error && relatedState.data && relatedState.data.length > 0 && (
          <ProductGrid products={relatedState.data} />
        )}
      </section>
    </div>
  )
}
