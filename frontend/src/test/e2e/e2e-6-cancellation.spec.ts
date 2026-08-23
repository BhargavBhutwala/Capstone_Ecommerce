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
  await page.waitForSelector('input[type="email"]', { timeout: 10_000 })
  await page.fill('input[name="firstName"], input[id*="firstName"], input[placeholder*="First"]', 'Cancel')
  await page.fill('input[name="lastName"], input[id*="lastName"], input[placeholder*="Last"]', 'Tester')
  await page.fill('input[type="email"]', EMAIL)
  await page.fill('input[type="password"]', PASSWORD)
  await page.click('button[type="submit"]')
  await page.waitForTimeout(1000)
  if (page.url().includes('/login') || page.url().includes('/register')) {
    await page.goto('/login')
    await page.fill('input[type="email"]', EMAIL)
    await page.fill('input[type="password"]', PASSWORD)
    await page.click('button[type="submit"]')
  }
  await expect(page).not.toHaveURL(/\/login|\/register/, { timeout: 10_000 })
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
  const addAddrBtn = page.locator('button', { hasText: /add.*address|new address/i }).first()
  if (await addAddrBtn.isVisible({ timeout: 3_000 })) {
    await addAddrBtn.click()
  }
  await page.waitForSelector('input[id*="addr-line1"], input[autocomplete="address-line1"]', { timeout: 5_000 })
  await page.fill('input[id*="addr-line1"], input[autocomplete="address-line1"]', '100 Cancel St')
  await page.fill('input[id*="addr-city"], input[autocomplete="address-level2"]', 'Chicago')
  await page.fill('input[id*="addr-state"], input[autocomplete="address-level1"]', 'IL')
  await page.fill('input[id*="addr-postal"], input[autocomplete="postal-code"]', '60601')
  await page.fill('input[id*="addr-country"], input[autocomplete="country-name"]', 'USA')
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

    // Verify status is PENDING_PAYMENT (heuristic label)
    await expect(page.locator('text=/pending payment/i')).toBeVisible({ timeout: 5_000 })

    // Cancel button should be visible
    const cancelBtn = page.locator('button', { hasText: /cancel order/i })
    await cancelBtn.waitFor({ timeout: 5_000 })
    await cancelBtn.click()

    // After cancellation, status should update to CANCELLED
    await expect(page.locator('text=/cancelled/i')).toBeVisible({ timeout: 10_000 })

    // Cancel button should no longer be visible
    await expect(page.locator('button', { hasText: /cancel order/i })).not.toBeVisible({ timeout: 3_000 })
  })
})
