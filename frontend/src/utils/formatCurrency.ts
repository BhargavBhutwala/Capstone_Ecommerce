/**
 * Shared INR currency formatter.
 *
 * Uses Intl.NumberFormat with locale en-IN and currency INR so all
 * customer-facing monetary values display in the Indian Rupee format:
 *   ₹799.00
 *   ₹1,299.00
 *   ₹12,499.00
 *   ₹1,25,000.00
 *
 * Usage:
 *   import { formatCurrency } from '../../utils/formatCurrency'
 *   formatCurrency(product.price)   // "₹799.00"
 */

const formatter = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

/**
 * Format a numeric value as INR currency string.
 * @param value - the numeric monetary value (BigDecimal-sourced from backend)
 * @returns formatted string e.g. "₹799.00"
 */
export function formatCurrency(value: number): string {
  return formatter.format(value)
}
