/**
 * ProtectedRoute
 *
 * Renders its child route only when the user is authenticated.
 * While session bootstrap is in progress the route shows nothing (spinner
 * could be added; kept minimal for FE-02 scope).
 * Unauthenticated access redirects to /login, preserving the intended
 * destination in location state so the login page can redirect back after
 * successful authentication.
 */

import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'

export function ProtectedRoute() {
  const { user, bootstrapping } = useAuth()
  const location = useLocation()

  // Wait for session bootstrap before making an auth decision
  if (bootstrapping) {
    return null
  }

  if (!user) {
    // Preserve intended destination so login can redirect back
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return <Outlet />
}
