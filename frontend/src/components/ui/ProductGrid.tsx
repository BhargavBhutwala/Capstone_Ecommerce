/**
 * ProductGrid — responsive grid of ProductCard components.
 * Used by all product-listing pages.
 *
 * FE-04: accepts optional cart interaction props that are forwarded to each
 * ProductCard. When onAddToCart is not provided the grid renders in read-only
 * (catalog-only) mode — no buttons are shown on the cards.
 */

import type { ProductSummary } from '../../types/api'
import { ProductCard } from './ProductCard'
import styles from './ProductGrid.module.css'

interface ProductGridProps {
  products: ProductSummary[]
  /** When provided, each card receives an Add-to-Cart button */
  onAddToCart?: (productId: number) => void
  /** Per-product loading state accessor */
  isAdding?: (productId: number) => boolean
  /** Per-product error accessor */
  getAddToCartError?: (productId: number) => string | null
}

export function ProductGrid({
  products,
  onAddToCart,
  isAdding,
  getAddToCartError,
}: ProductGridProps) {
  return (
    <div className={styles.grid}>
      {products.map((p) => (
        <ProductCard
          key={p.id}
          product={p}
          onAddToCart={onAddToCart}
          addingToCart={isAdding ? isAdding(p.id) : false}
          addToCartError={getAddToCartError ? getAddToCartError(p.id) : null}
        />
      ))}
    </div>
  )
}
