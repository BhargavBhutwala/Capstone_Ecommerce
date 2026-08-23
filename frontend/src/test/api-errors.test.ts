/**
 * Unit tests: ApiError normalisation and error utility functions.
 *
 * Verifies:
 * - ApiError preserves all ErrorResponse fields
 * - getErrorMessage returns message from ApiError/Error/unknown
 * - getFieldErrors returns fieldErrors from ApiError, empty otherwise
 * - isApiErrorStatus matches correctly
 * - 401 / 409 convenience properties
 */

import { describe, it, expect } from 'vitest'
import { ApiError } from '../api/client'
import { getErrorMessage, getFieldErrors, isApiErrorStatus } from '../api/errors'
import type { ErrorResponse } from '../types/api'

const basePayload: ErrorResponse = {
  timestamp: '2024-01-01T00:00:00Z',
  status: 400,
  code: 'VALIDATION_ERROR',
  message: 'Validation failed.',
  path: '/api/users',
  fieldErrors: { email: 'Email is required.' },
}

describe('ApiError construction', () => {
  it('preserves status from payload', () => {
    const err = new ApiError(basePayload)
    expect(err.status).toBe(400)
  })

  it('preserves code from payload', () => {
    const err = new ApiError(basePayload)
    expect(err.code).toBe('VALIDATION_ERROR')
  })

  it('preserves message from payload', () => {
    const err = new ApiError(basePayload)
    expect(err.message).toBe('Validation failed.')
  })

  it('preserves path from payload', () => {
    const err = new ApiError(basePayload)
    expect(err.path).toBe('/api/users')
  })

  it('preserves fieldErrors from payload', () => {
    const err = new ApiError(basePayload)
    expect(err.fieldErrors).toEqual({ email: 'Email is required.' })
  })

  it('preserves timestamp from payload', () => {
    const err = new ApiError(basePayload)
    expect(err.timestamp).toBe('2024-01-01T00:00:00Z')
  })

  it('has name ApiError', () => {
    const err = new ApiError(basePayload)
    expect(err.name).toBe('ApiError')
  })

  it('is instanceof Error', () => {
    const err = new ApiError(basePayload)
    expect(err).toBeInstanceOf(Error)
  })

  it('is instanceof ApiError', () => {
    const err = new ApiError(basePayload)
    expect(err).toBeInstanceOf(ApiError)
  })
})

describe('ApiError.isUnauthorized', () => {
  it('returns true for status 401', () => {
    const err = new ApiError({ ...basePayload, status: 401, code: 'UNAUTHORIZED' })
    expect(err.isUnauthorized).toBe(true)
  })

  it('returns false for non-401', () => {
    const err = new ApiError(basePayload)
    expect(err.isUnauthorized).toBe(false)
  })
})

describe('ApiError.isConflict', () => {
  it('returns true for status 409', () => {
    const err = new ApiError({ ...basePayload, status: 409, code: 'CONFLICT' })
    expect(err.isConflict).toBe(true)
  })

  it('returns false for non-409', () => {
    const err = new ApiError(basePayload)
    expect(err.isConflict).toBe(false)
  })
})

describe('ApiError.isNotFound', () => {
  it('returns true for status 404', () => {
    const err = new ApiError({ ...basePayload, status: 404, code: 'NOT_FOUND' })
    expect(err.isNotFound).toBe(true)
  })

  it('returns false for non-404', () => {
    const err = new ApiError(basePayload)
    expect(err.isNotFound).toBe(false)
  })
})

describe('ApiError without fieldErrors', () => {
  it('fieldErrors is undefined when not supplied', () => {
    const err = new ApiError({ ...basePayload, fieldErrors: undefined })
    expect(err.fieldErrors).toBeUndefined()
  })
})

describe('getErrorMessage', () => {
  it('returns ApiError message for ApiError instances', () => {
    const err = new ApiError(basePayload)
    expect(getErrorMessage(err)).toBe('Validation failed.')
  })

  it('returns message for generic Error instances', () => {
    expect(getErrorMessage(new Error('Something broke'))).toBe('Something broke')
  })

  it('returns generic fallback for unknown thrown values', () => {
    expect(getErrorMessage('string error')).toBe('An unexpected error occurred. Please try again.')
  })

  it('returns generic fallback for null', () => {
    expect(getErrorMessage(null)).toBe('An unexpected error occurred. Please try again.')
  })
})

describe('getFieldErrors', () => {
  it('returns fieldErrors from ApiError when present', () => {
    const err = new ApiError(basePayload)
    expect(getFieldErrors(err)).toEqual({ email: 'Email is required.' })
  })

  it('returns empty record when ApiError has no fieldErrors', () => {
    const err = new ApiError({ ...basePayload, fieldErrors: undefined })
    expect(getFieldErrors(err)).toEqual({})
  })

  it('returns empty record for generic Error', () => {
    expect(getFieldErrors(new Error('oops'))).toEqual({})
  })

  it('returns empty record for unknown value', () => {
    expect(getFieldErrors(42)).toEqual({})
  })
})

describe('isApiErrorStatus', () => {
  it('returns true when ApiError status matches', () => {
    const err = new ApiError({ ...basePayload, status: 409 })
    expect(isApiErrorStatus(err, 409)).toBe(true)
  })

  it('returns false when ApiError status does not match', () => {
    const err = new ApiError(basePayload)
    expect(isApiErrorStatus(err, 409)).toBe(false)
  })

  it('returns false for non-ApiError', () => {
    expect(isApiErrorStatus(new Error('oops'), 400)).toBe(false)
  })

  it('returns false for null', () => {
    expect(isApiErrorStatus(null, 400)).toBe(false)
  })
})
