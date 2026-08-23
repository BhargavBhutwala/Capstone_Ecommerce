/**
 * 404 Not Found page.
 *
 * Rendered when no route matches. Provides a link back to the home page.
 */

import { Link } from 'react-router-dom'
import styles from './NotFoundPage.module.css'

export function NotFoundPage() {
  return (
    <div className={styles.page}>
      <h1 className={styles.code}>404</h1>
      <p className={styles.message}>Page not found.</p>
      <Link to="/" className={styles.link}>Go to home</Link>
    </div>
  )
}
