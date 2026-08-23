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
 *   the same onAddToCart prop (see ProductGrid / page components).
 */

import { Link } from 'react-router-dom'
import type { ProductSummary } from '../../types/api'
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
  return (
    <article className={styles.card}>
      <Link to={`/products/${product.id}`} className={styles.link}>
        <div className={styles.body}>
          <h3 className={styles.title}>{product.title}</h3>
          {product.isbn && <p className={styles.isbn}>ISBN: {product.isbn}</p>}
          <p className={styles.price}>${product.price.toFixed(2)}</p>
          <span className={product.available ? styles.available : styles.unavailable}>
            {product.available ? 'In stock' : 'Unavailable'}
          </span>
        </div>
      </Link>

      {onAddToCart && (
        <div className={styles.cartAction}>
          <button
            className={styles.addToCartBtn}
            onClick={() => onAddToCart(product.id)}
            disabled={addingToCart || !product.available}
            aria-label={`Add ${product.title} to cart`}
          >
            {addingToCart ? 'Adding…' : 'Add to Cart'}
          </button>
          {addToCartError && (
            <p className={styles.addToCartError} role="alert">
              {addToCartError}
            </p>
          )}
        </div>
      )}
    </article>
  )
}
