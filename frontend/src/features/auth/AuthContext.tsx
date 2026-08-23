/* eslint-disable react-refresh/only-export-components */
/**
 * Authentication context.
 *
 * Provides session state (user, token, loading) and auth actions (login,
 * logout, register) to the component tree.
 *
 * Design decisions:
 * - The bearer token is persisted in sessionStorage so a tab reload restores
 *   the session without requiring re-login, but closing the browser clears it.
 * - The module-level token in api/client.ts is kept in sync so API calls
 *   outside React tree use the same token.
 * - On bootstrap, GET /users/me validates the stored token and restores the
 *   user snapshot. A 401 response clears the stale token silently.
 * - No refresh-token logic — the backend contract does not define one.
 */

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react'
import type { ReactNode } from 'react'

import { setActiveToken } from '../../api/client'
import * as authApi from '../../api/authApi'
import type { LoginRequest, RegisterRequest, UserResponse } from '../../types/api'

// ─── Storage key ──────────────────────────────────────────────────────────────

const TOKEN_KEY = 'ebookstore_token'

// ─── Context shape ─────────────────────────────────────────────────────────────

export interface AuthContextValue {
  /** The currently authenticated user, or null when not signed in */
  user: UserResponse | null
  /** True while the initial session bootstrap is in progress */
  bootstrapping: boolean
  /** True when an auth action (login/register/logout) is in flight */
  loading: boolean
  /** Top-level auth error message, cleared on the next action */
  error: string | null
  /** Sign in with email + password */
  login: (data: LoginRequest) => Promise<void>
  /** Register a new account */
  register: (data: RegisterRequest) => Promise<UserResponse>
  /** Sign out the current user */
  logout: () => Promise<void>
  /** Clear any stale auth error */
  clearError: () => void
}

// ─── Context ──────────────────────────────────────────────────────────────────

export const AuthContext = createContext<AuthContextValue | null>(null)

// ─── Provider ─────────────────────────────────────────────────────────────────

interface AuthProviderProps {
  children: ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<UserResponse | null>(null)
  const [bootstrapping, setBootstrapping] = useState(true)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Guard to prevent state updates after unmount
  const mounted = useRef(true)
  useEffect(() => {
    mounted.current = true
    return () => {
      mounted.current = false
    }
  }, [])

  // ── Session bootstrap ──────────────────────────────────────────────────────
  useEffect(() => {
    const stored = sessionStorage.getItem(TOKEN_KEY)

    if (!stored) {
      // No token stored — resolve bootstrap asynchronously to avoid
      // synchronous setState-in-effect lint error
      Promise.resolve().then(() => {
        if (mounted.current) setBootstrapping(false)
      })
      return
    }

    // Sync token into the API client before making the request
    setActiveToken(stored)

    authApi
      .getCurrentUser()
      .then((u: UserResponse) => {
        if (mounted.current) {
          setUser(u)
        }
      })
      .catch(() => {
        // Token is invalid or expired — clear it silently
        sessionStorage.removeItem(TOKEN_KEY)
        setActiveToken(null)
      })
      .finally(() => {
        if (mounted.current) {
          setBootstrapping(false)
        }
      })
  }, [])

  // ── Actions ────────────────────────────────────────────────────────────────

  const doLogin = useCallback(async (data: LoginRequest): Promise<void> => {
    setLoading(true)
    setError(null)
    try {
      const response = await authApi.login(data)
      sessionStorage.setItem(TOKEN_KEY, response.accessToken)
      setActiveToken(response.accessToken)
      if (mounted.current) {
        setUser(response.user)
      }
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Login failed.'
      if (mounted.current) setError(msg)
      throw err
    } finally {
      if (mounted.current) setLoading(false)
    }
  }, [])

  const doRegister = useCallback(
    async (data: RegisterRequest): Promise<UserResponse> => {
      setLoading(true)
      setError(null)
      try {
        const created = await authApi.registerUser(data)
        return created
      } catch (err) {
        const msg = err instanceof Error ? err.message : 'Registration failed.'
        if (mounted.current) setError(msg)
        throw err
      } finally {
        if (mounted.current) setLoading(false)
      }
    },
    [],
  )

  const doLogout = useCallback(async (): Promise<void> => {
    setLoading(true)
    try {
      await authApi.logout()
    } catch {
      // Best-effort: clear local session even if the server call fails
    } finally {
      sessionStorage.removeItem(TOKEN_KEY)
      setActiveToken(null)
      if (mounted.current) {
        setUser(null)
        setError(null)
        setLoading(false)
      }
    }
  }, [])

  const clearError = useCallback(() => setError(null), [])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      bootstrapping,
      loading,
      error,
      login: doLogin,
      register: doRegister,
      logout: doLogout,
      clearError,
    }),
    [user, bootstrapping, loading, error, doLogin, doRegister, doLogout, clearError],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// ─── Hook ─────────────────────────────────────────────────────────────────────

/**
 * Returns the auth context. Must be used inside <AuthProvider>.
 */
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return ctx
}
