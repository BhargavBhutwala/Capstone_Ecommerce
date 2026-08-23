/**
 * Register page.
 *
 * Calls operationId: registerUser → POST /auth/register
 * On successful registration the user is redirected to login so they can
 * sign in with their new credentials.
 *
 * Validation mirrors the RegisterRequest constraints from the OpenAPI contract:
 *   firstName: required, 1–100 chars
 *   lastName:  required, 1–100 chars
 *   email:     required, email format, max 255 chars
 *   password:  required, 8–100 chars
 *
 * Field-level errors from the backend ErrorResponse.fieldErrors are surfaced
 * on the relevant fields.
 */

import { type FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from './AuthContext'
import { getFieldErrors } from '../../api/errors'
import { FormField } from '../../components/forms/FormField'
import styles from './auth.module.css'

export function RegisterPage() {
  const { register, loading } = useAuth()
  const navigate = useNavigate()

  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [globalError, setGlobalError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  function validateClient(): Record<string, string> {
    const errs: Record<string, string> = {}
    if (!firstName.trim()) errs.firstName = 'First name is required.'
    else if (firstName.trim().length > 100) errs.firstName = 'First name must be 100 characters or fewer.'
    if (!lastName.trim()) errs.lastName = 'Last name is required.'
    else if (lastName.trim().length > 100) errs.lastName = 'Last name must be 100 characters or fewer.'
    if (!email.trim()) errs.email = 'Email is required.'
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errs.email = 'Enter a valid email address.'
    else if (email.length > 255) errs.email = 'Email must be 255 characters or fewer.'
    if (!password) errs.password = 'Password is required.'
    else if (password.length < 8) errs.password = 'Password must be at least 8 characters.'
    else if (password.length > 100) errs.password = 'Password must be 100 characters or fewer.'
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
      await register({ firstName, lastName, email, password })
      // Registration successful — redirect to login
      navigate('/login', { state: { registered: true } })
    } catch (err) {
      const serverFieldErrors = getFieldErrors(err)
      if (Object.keys(serverFieldErrors).length > 0) {
        setFieldErrors(serverFieldErrors)
      } else {
        setGlobalError(
          err instanceof Error ? err.message : 'Registration failed. Please try again.',
        )
      }
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <h1 className={styles.title}>Create account</h1>
        <p className={styles.subtitle}>
          Already have an account?{' '}
          <Link to="/login">Sign in</Link>
        </p>

        {globalError && (
          <div className={styles.globalError} role="alert">
            {globalError}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate className={styles.form}>
          <div className={styles.nameRow}>
            <FormField
              id="firstName"
              label="First name"
              type="text"
              autoComplete="given-name"
              value={firstName}
              onChange={(e) => setFirstName(e.target.value)}
              error={fieldErrors.firstName}
              disabled={loading}
              maxLength={100}
            />
            <FormField
              id="lastName"
              label="Last name"
              type="text"
              autoComplete="family-name"
              value={lastName}
              onChange={(e) => setLastName(e.target.value)}
              error={fieldErrors.lastName}
              disabled={loading}
              maxLength={100}
            />
          </div>

          <FormField
            id="email"
            label="Email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            error={fieldErrors.email}
            disabled={loading}
            maxLength={255}
          />

          <FormField
            id="password"
            label="Password"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={fieldErrors.password}
            disabled={loading}
            minLength={8}
            maxLength={100}
          />

          <button type="submit" disabled={loading} className={styles.submitBtn}>
            {loading ? 'Creating account…' : 'Create account'}
          </button>
        </form>
      </div>
    </div>
  )
}
