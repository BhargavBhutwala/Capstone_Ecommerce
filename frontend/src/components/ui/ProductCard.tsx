/**
 * ProductCard — displays a ProductSummary in a card layout.
 *
 * Used in product listings (search results, category, brand pages)
 * and related-products sections.
 *
 * Does NOT include Add-to-Cart functionality — that is FE-04.
 */

import { Link } from 'react-router-dom'
import type { ProductSummary } from '../../types/api'
import styles from './ProductCard.module.css'

interface ProductCardProps {
  product: ProductSummary
}

export function ProductCard({ product }: ProductCardProps) {
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
    </article>
  )
}
