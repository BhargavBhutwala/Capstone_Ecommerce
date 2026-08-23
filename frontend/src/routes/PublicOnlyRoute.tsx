/**
 * PublicOnlyRoute
 *
 * Renders its child route only when the user is NOT authenticated.
 * Authenticated users visiting public-only routes (login, register) are
 * redirected away to the appropriate authenticated landing page.
 *
 * If the router state carries a `from` location (set by ProtectedRoute),
 * that destination is used; otherwise the user lands on the home page.
 */

import { Navigate, Outlet, useLocation } from 'react-router-dom'
import type { Location } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'

export function PublicOnlyRoute() {
  const { user, bootstrapping } = useAuth()
  const location = useLocation()

  // Wait for session bootstrap before making an auth decision
  if (bootstrapping) {
    return null
  }

  if (user) {
    const from = (location.state as { from?: Location } | null)?.from
    return <Navigate to={from?.pathname ?? '/'} replace />
  }

  return <Outlet />
}
