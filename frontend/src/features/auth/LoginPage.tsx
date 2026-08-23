/**
 * Login page.
 *
 * Calls operationId: login → POST /auth/login
 * On success the auth context stores the token and user, then the user is
 * redirected to the originally requested route or the home page.
 *
 * Validation mirrors the LoginRequest constraints from the OpenAPI contract:
 *   email: required, email format
 *   password: required
 *
 * Field-level errors from the backend ErrorResponse.fieldErrors are surfaced
 * on the relevant fields.
 */

import { type FormEvent, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from './AuthContext'
import { getFieldErrors } from '../../api/errors'
import { FormField } from '../../components/forms/FormField'
import styles from './auth.module.css'

export function LoginPage() {
  const { login, loading } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [globalError, setGlobalError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  // Redirect back to the page the user was trying to reach, or home
  const from = (location.state as { from?: { pathname: string } } | null)?.from?.pathname ?? '/'

  function validateClient(): Record<string, string> {
    const errs: Record<string, string> = {}
    if (!email.trim()) errs.email = 'Email is required.'
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errs.email = 'Enter a valid email address.'
    if (!password) errs.password = 'Password is required.'
    return errs
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setGlobalError(null)
    setFieldErrors({})

    const clientErrs = validateClient()
    if (Object.keys(clientErrs).length > 0) {
      setFieldErrors(clientErrs)
      return
    }

    try {
      await login({ email, password })
      navigate(from, { replace: true })
    } catch (err) {
      const serverFieldErrors = getFieldErrors(err)
      if (Object.keys(serverFieldErrors).length > 0) {
        setFieldErrors(serverFieldErrors)
      } else {
        setGlobalError(err instanceof Error ? err.message : 'Login failed. Please try again.')
      }
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <h1 className={styles.title}>Sign in</h1>
        <p className={styles.subtitle}>
          Don&apos;t have an account?{' '}
          <Link to="/register">Register</Link>
        </p>

        {globalError && (
          <div className={styles.globalError} role="alert">
            {globalError}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate className={styles.form}>
          <FormField
            id="email"
            label="Email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            error={fieldErrors.email}
            disabled={loading}
          />
          <FormField
            id="password"
            label="Password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={fieldErrors.password}
            disabled={loading}
          />

          <button type="submit" disabled={loading} className={styles.submitBtn}>
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
      </div>
    </div>
  )
}
