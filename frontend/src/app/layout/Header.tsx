/**
 * Application header.
 *
 * Shows the site name and navigation links appropriate to the current
 * authentication state. Navigation to feature pages is added in later
 * milestones as those features are implemented.
 */

import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../features/auth/AuthContext'
import styles from './Header.module.css'

export function Header() {
  const { user, logout, loading } = useAuth()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/login')
  }

  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <Link to="/" className={styles.brand}>
          E-Bookstore
        </Link>

        <nav className={styles.nav}>
          <Link to="/">Catalog</Link>

          {user ? (
            <>
              <Link to="/orders">My Orders</Link>
              <Link to="/cart">Cart</Link>
              <Link to="/profile" className={styles.userLabel}>
                {user.firstName} {user.lastName}
              </Link>
              <button
                onClick={handleLogout}
                disabled={loading}
                className={styles.logoutBtn}
              >
                Sign out
              </button>
            </>
          ) : (
            <>
              <Link to="/login">Sign in</Link>
              <Link to="/register">Register</Link>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}
