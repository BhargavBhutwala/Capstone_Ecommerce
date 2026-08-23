/**
 * Vitest global test setup.
 *
 * - Extends expect with @testing-library/jest-dom matchers
 * - Provides a clean sessionStorage before each test
 * - Resets fetch mock after each test
 */

import '@testing-library/jest-dom'
import { afterEach, beforeEach, vi } from 'vitest'
import { cleanup } from '@testing-library/react'

// Clean up rendered React trees after each test
afterEach(() => {
  cleanup()
})

// Clear sessionStorage before each test to avoid cross-test token bleed
beforeEach(() => {
  sessionStorage.clear()
})

// Reset all mocks after each test
afterEach(() => {
  vi.restoreAllMocks()
})
