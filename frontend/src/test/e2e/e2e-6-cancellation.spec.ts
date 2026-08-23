/**
 * E2E-6: Order cancellation.
 *
 * Verifies:
 * - An eligible PENDING_PAYMENT or PAID order exposes cancellation button
 * - Cancellation proceeds through backend
 * - Displayed status becomes CANCELLED
 */

import { test, expect, type Page } from '@playwright/test'

const EMAIL = `e2e-cancel-${Date.now()}@example.com`
const PASSWORD = 'E2ECancel!2024'

async function registerAndLogin(page: Page) {
  await page.goto('/register')
  await page.waitForSelector('#firstName', { timeout: 10_000 })
  await page.fill('#firstName', 'Cancel')
  await page.fill('#lastName', 'Tester')
  await page.fill('#email', EMAIL)
  await page.fill('#password', PASSWORD)
  await page.click('button[type="submit"]')

  await page.waitForURL((url) => !url.pathname.startsWith('/register'), { timeout: 15_000 })

  if (page.url().includes('/login')) {
    await page.waitForSelector('#email', { timeout: 5_000 })
    await page.fill('#email', EMAIL)
    await page.fill('#password', PASSWORD)
    await page.click('button[type="submit"]')
    await page.waitForURL((url) => !url.pathname.startsWith('/login'), { timeout: 15_000 })
  }

  await expect(page).not.toHaveURL(/\/login|\/register/, { timeout: 5_000 })
}

async function createAddressAndCheckout(page: Page): Promise<string | null> {
  // Add product to cart
  await page.goto('/products')
  const addBtn = page.locator('button', { hasText: /add to cart/i }).first()
  await addBtn.waitFor({ timeout: 15_000 })
  await addBtn.click()
  await page.waitForTimeout(500)

  // Create address
  await page.goto('/addresses')
  await page.waitForSelector('h1', { timeout: 10_000 })
  await page.locator('button', { hasText: /\+\s*add address/i }).click()
  await page.waitForSelector('#addr-line1', { timeout: 10_000 })
  await page.fill('#addr-line1', '100 Cancel St')
  await page.fill('#addr-city', 'Chicago')
  await page.fill('#addr-state', 'IL')
  await page.fill('#addr-postal', '60601')
  await page.fill('#addr-country', 'USA')
  await page.click('button[type="submit"]')
  await page.waitForTimeout(1000)

  // Checkout
  await page.goto('/checkout')
  await page.locator('button', { hasText: /place order/i }).click()
  await expect(page).toHaveURL(/orders\/\d+\/payment/, { timeout: 15_000 })

  // Extract orderId from URL
  const match = page.url().match(/orders\/(\d+)\/payment/)
  return match ? match[1] : null
}

test.describe('E2E-6: Order Cancellation', () => {
  test('PENDING_PAYMENT order shows cancel button and cancels', async ({ page }) => {
    await registerAndLogin(page)
    const orderId = await createAddressAndCheckout(page)

    if (!orderId) {
      test.skip()
      return
    }

    // Navigate to order detail — it should be PENDING_PAYMENT
    await page.goto(`/orders/${orderId}`)
    await expect(page.locator('h1').first()).toBeVisible({ timeout: 10_000 })

    // Verify status is PENDING_PAYMENT (heuristic label) — use .first() as it appears in badge + metadata
    await expect(page.locator('text=/pending payment/i').first()).toBeVisible({ timeout: 5_000 })

    // Cancel button should be visible
    const cancelBtn = page.locator('button', { hasText: /cancel order/i })
    await cancelBtn.waitFor({ timeout: 5_000 })
    await cancelBtn.click()

    // After cancellation, status should update to CANCELLED — use .first() (badge + metadata)
    await expect(page.locator('text=/cancelled/i').first()).toBeVisible({ timeout: 10_000 })

    // Cancel button should no longer be visible
    await expect(page.locator('button', { hasText: /cancel order/i })).not.toBeVisible({ timeout: 3_000 })
  })
})
