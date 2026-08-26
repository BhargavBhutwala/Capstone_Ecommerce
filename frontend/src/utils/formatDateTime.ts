/**
 * Shared IST date/time formatters.
 *
 * All customer-facing timestamps are displayed in Indian Standard Time (UTC+5:30)
 * using the en-IN locale. The rendered value includes an explicit "IST" suffix
 * so it is unambiguous to the user.
 *
 * Example outputs:
 *   formatDateTime('2026-08-26T08:30:00Z')  → "26 Aug 2026, 2:00 pm IST"
 *   formatDate('2026-08-26T08:30:00Z')       → "26 Aug 2026"
 *
 * Usage:
 *   import { formatDateTime, formatDate } from '../../utils/formatDateTime'
 */

const DATE_TIME_FORMATTER = new Intl.DateTimeFormat('en-IN', {
  timeZone: 'Asia/Kolkata',
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  hour: 'numeric',
  minute: '2-digit',
  hour12: true,
})

const DATE_FORMATTER = new Intl.DateTimeFormat('en-IN', {
  timeZone: 'Asia/Kolkata',
  day: '2-digit',
  month: 'short',
  year: 'numeric',
})

/**
 * Format an ISO-8601 timestamp string as a human-readable date+time in IST.
 * Returns the original string unchanged if parsing fails.
 *
 * @param iso - ISO-8601 datetime string from the backend
 * @returns e.g. "26 Aug 2026, 2:00 pm IST"
 */
export function formatDateTime(iso: string): string {
  try {
    return DATE_TIME_FORMATTER.format(new Date(iso)) + ' IST'
  } catch {
    return iso
  }
}

/**
 * Format an ISO-8601 timestamp string as a date-only string in IST.
 * Returns the original string unchanged if parsing fails.
 *
 * @param iso - ISO-8601 datetime string from the backend
 * @returns e.g. "26 Aug 2026"
 */
export function formatDate(iso: string): string {
  try {
    return DATE_FORMATTER.format(new Date(iso))
  } catch {
    return iso
  }
}
