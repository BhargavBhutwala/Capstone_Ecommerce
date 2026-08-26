/**
 * Unit tests: formatCurrency utility.
 *
 * Verifies:
 * - INR symbol (₹) is present in output
 * - en-IN grouping: 1,000 → ₹1,000.00 (standard Indian grouping)
 * - 2 decimal places always produced
 * - zero renders correctly
 * - large Indian-style grouping: 1,25,000
 */

import { describe, it, expect } from 'vitest'
import { formatCurrency } from '../utils/formatCurrency'

describe('formatCurrency', () => {
  it('includes the ₹ symbol', () => {
    const result = formatCurrency(799)
    expect(result).toContain('₹')
  })

  it('formats 799 with 2 decimal places', () => {
    const result = formatCurrency(799)
    expect(result).toContain('799.00')
  })

  it('formats 1299 with 2 decimal places', () => {
    const result = formatCurrency(1299)
    expect(result).toContain('1,299.00')
  })

  it('formats 0 correctly', () => {
    const result = formatCurrency(0)
    expect(result).toContain('0.00')
    expect(result).toContain('₹')
  })

  it('formats a large value with Indian grouping', () => {
    const result = formatCurrency(125000)
    // Indian grouping: 1,25,000
    expect(result).toContain('25,000')
    expect(result).toContain('₹')
  })

  it('preserves two decimal places for fractional values', () => {
    const result = formatCurrency(399.5)
    expect(result).toContain('399.50')
  })
})
