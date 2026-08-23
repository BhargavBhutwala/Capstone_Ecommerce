/**
 * Root application layout.
 *
 * Renders the persistent shell (header, main content area, footer) and
 * the <Outlet /> from React Router so child routes render inside the shell.
 */

import { Outlet } from 'react-router-dom'
import { Header } from './Header'
import styles from './AppLayout.module.css'

export function AppLayout() {
  return (
    <div className={styles.root}>
      <Header />
      <main className={styles.main}>
        <Outlet />
      </main>
      <footer className={styles.footer}>
        <p>&copy; {new Date().getFullYear()} E-Bookstore</p>
      </footer>
    </div>
  )
}
