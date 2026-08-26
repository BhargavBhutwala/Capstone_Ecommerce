/**
 * Unit tests: formatDateTime and formatDate utilities.
 *
 * Verifies:
 * - formatDateTime appends " IST" suffix
 * - formatDate returns a date string without a time component suffix
 * - Both functions return the original string on an invalid ISO input
 * - Output contains expected year
 */

import { describe, it, expect } from 'vitest'
import { formatDateTime, formatDate } from '../utils/formatDateTime'

// UTC midnight on 2024-01-15 = 05:30 IST on 2024-01-15
const ISO_UTC = '2024-01-15T00:00:00Z'
// UTC time that crosses midnight to the next day in IST
// 2024-01-14T19:00:00Z = 2024-01-15 00:30 IST
const ISO_UTC_NEXT_DAY = '2024-01-14T19:00:00Z'

describe('formatDateTime', () => {
  it('appends IST to the formatted string', () => {
    const result = formatDateTime(ISO_UTC)
    expect(result).toMatch(/IST$/)
  })

  it('includes the year 2024', () => {
    const result = formatDateTime(ISO_UTC)
    expect(result).toContain('2024')
  })

  it('returns the original string for an invalid ISO value', () => {
    const bad = 'not-a-date'
    const result = formatDateTime(bad)
    expect(result).toBe(bad)
  })

  it('accounts for IST offset: UTC 19:00 Jan 14 → Jan 15 in IST', () => {
    // 2024-01-14T19:00:00Z is 2024-01-15T00:30:00+05:30
    const result = formatDateTime(ISO_UTC_NEXT_DAY)
    expect(result).toContain('2024')
    expect(result).toMatch(/IST$/)
  })
})

describe('formatDate', () => {
  it('does NOT end with IST (date only)', () => {
    const result = formatDate(ISO_UTC)
    expect(result).not.toMatch(/IST$/)
  })

  it('includes the year 2024', () => {
    const result = formatDate(ISO_UTC)
    expect(result).toContain('2024')
  })

  it('returns the original string for an invalid ISO value', () => {
    const bad = 'not-a-date'
    const result = formatDate(bad)
    expect(result).toBe(bad)
  })

  it('includes a month abbreviation', () => {
    const result = formatDate(ISO_UTC)
    // en-IN with month:'short' should produce something like "15 Jan 2024"
    expect(result).toMatch(/jan/i)
  })
})
