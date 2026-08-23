/**
 * Shared error normalisation utilities.
 *
 * Provides helpers for extracting human-readable messages and field-level
 * validation errors from ApiError instances and unknown thrown values.
 */

import { ApiError } from './client'

/**
 * Extracts the top-level error message from an unknown thrown value.
 * Falls back to a generic message so the UI always has something to display.
 */
export function getErrorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return 'An unexpected error occurred. Please try again.'
}

/**
 * Extracts field-level validation errors from an unknown thrown value.
 * Returns an empty record when there are no field errors.
 */
export function getFieldErrors(error: unknown): Record<string, string> {
  if (error instanceof ApiError && error.fieldErrors) {
    return error.fieldErrors
  }
  return {}
}

/**
 * Returns true when the thrown error is an ApiError with the given HTTP status.
 */
export function isApiErrorStatus(error: unknown, status: number): boolean {
  return error instanceof ApiError && error.status === status
}
