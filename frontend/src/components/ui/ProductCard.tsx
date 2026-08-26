/**
 * ProductCard — displays a ProductSummary in a card layout.
 *
 * Used in product listings (search results, category, brand pages)
 * and related-products sections.
 *
 * FE-04: supports an optional Add-to-Cart button.
 * - When onAddToCart is provided, a button is rendered for available products.
 * - Authenticated users: onAddToCart is called with the product id.
 * - Unauthenticated users: redirect to /login is handled by the parent via
 *   the same onAddToCart prop.
 *
 * Book-cover support:
 * - Displays ProductSummary.imageUrl when available.
 * - Falls back to a clean placeholder when imageUrl is missing.
 * - Falls back when a remote image fails to load.
 */

import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { ProductSummary } from '../../types/api'
import { formatCurrency } from '../../utils/formatCurrency'
import styles from './ProductCard.module.css'

interface ProductCardProps {
  product: ProductSummary

  /** Called when the user clicks Add to Cart. Handled by the parent. */
  onAddToCart?: (productId: number) => void

  /** True while this product's add-to-cart request is in flight */
  addingToCart?: boolean

  /** Inline error message from a failed add-to-cart attempt */
  addToCartError?: string | null
}

export function ProductCard({
  product,
  onAddToCart,
  addingToCart = false,
  addToCartError = null,
}: ProductCardProps) {
  const [imageFailed, setImageFailed] = useState(false)

  const showImage = Boolean(product.imageUrl) && !imageFailed

  return (
    <article className={styles.card}>
      <Link
        to={`/products/${product.id}`}
        className={styles.link}
        aria-label={`View ${product.title}`}
      >
        {/* ── Book cover ── */}
        <div className={styles.imageWrapper}>
          {showImage ? (
            <img
              src={product.imageUrl ?? undefined}
              alt={`${product.title} cover`}
              className={styles.image}
              loading="lazy"
              decoding="async"
              onError={() => setImageFailed(true)}
            />
          ) : (
            <div
              className={styles.imagePlaceholder}
              aria-label={`No cover available for ${product.title}`}
            >
              <span
                className={styles.placeholderIcon}
                aria-hidden="true"
              >
                📖
              </span>

              <span className={styles.placeholderText}>
                No cover available
              </span>
            </div>
          )}

          {!product.available && (
            <span className={styles.unavailableBadge}>
              Unavailable
            </span>
          )}
        </div>

        {/* ── Product information ── */}
        <div className={styles.body}>
          <h3 className={styles.title}>
            {product.title}
          </h3>

          {product.isbn && (
            <p className={styles.isbn}>
              ISBN: {product.isbn}
            </p>
          )}

          <div className={styles.bottomInfo}>
            <p className={styles.price}>
              {formatCurrency(product.price)}
            </p>

            <span
              className={
                product.available
                  ? styles.available
                  : styles.unavailable
              }
            >
              {product.available
                ? product.stockQuantity !== undefined
                  ? `${product.stockQuantity} in stock`
                  : 'In stock'
                : 'Unavailable'}
            </span>
          </div>
        </div>
      </Link>

      {/* ── Add to Cart action ── */}
      {onAddToCart && (
        <div className={styles.cartAction}>
          <button
            type="button"
            className={styles.addToCartBtn}
            onClick={() => onAddToCart(product.id)}
            disabled={
              addingToCart || !product.available
            }
            aria-label={`Add ${product.title} to cart`}
          >
            {addingToCart
              ? 'Adding…'
              : product.available
                ? 'Add to Cart'
                : 'Unavailable'}
          </button>

          {addToCartError && (
            <p
              className={styles.addToCartError}
              role="alert"
            >
              {addToCartError}
            </p>
          )}
        </div>
      )}
    </article>
  )
}