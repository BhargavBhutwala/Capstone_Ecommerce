/**
 * ProductGrid — responsive grid of ProductCard components.
 * Used by all product-listing pages.
 */

import type { ProductSummary } from '../../types/api'
import { ProductCard } from './ProductCard'
import styles from './ProductGrid.module.css'

interface ProductGridProps {
  products: ProductSummary[]
}

export function ProductGrid({ products }: ProductGridProps) {
  return (
    <div className={styles.grid}>
      {products.map((p) => (
        <ProductCard key={p.id} product={p} />
      ))}
    </div>
  )
}
