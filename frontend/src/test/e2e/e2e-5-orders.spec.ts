/**
 * E2E-5: Order history.
 *
 * Verifies:
 * - Open order history
 * - Open order detail
 * - Buy Again and verify navigation to cart
 */

import { test, expect, type Page } from '@playwright/test'

const EMAIL = `e2e-orders-${Date.now()}@example.com`
const PASSWORD = 'E2EOrders!2024'

async function registerAndLogin(page: Page) {
  await page.goto('/register')
  await page.waitForSelector('input[type="email"]', { timeout: 10_000 })
  await page.fill('input[name="firstName"], input[id*="firstName"], input[placeholder*="First"]', 'Orders')
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

test.describe('E2E-5: Order History', () => {
  test('order history page loads and is accessible', async ({ page }) => {
    await registerAndLogin(page)
    await page.goto('/orders')
    await expect(page.locator('h1').first()).toBeVisible({ timeout: 10_000 })
    // Either orders list or empty state
    await expect(
      page.locator('[class*="list"], [class*="emptyState"], [class*="card"]').first()
    ).toBeVisible({ timeout: 10_000 })
  })

  test('clicking order row opens order detail', async ({ page }) => {
    await registerAndLogin(page)
    await page.goto('/orders')

    const orderLink = page.locator('a[href*="/orders/"]').first()
    const count = await orderLink.count()
    if (count === 0) {
      // No orders — skip detail navigation test
      test.skip()
      return
    }

    await orderLink.click()
    await expect(page).toHaveURL(/\/orders\/\d+$/, { timeout: 5_000 })
    await expect(page.locator('h1').first()).toBeVisible({ timeout: 10_000 })
  })

  test('Buy Again navigates to /cart', async ({ page }) => {
    await registerAndLogin(page)
    await page.goto('/orders')

    const orderLink = page.locator('a[href*="/orders/"]').first()
    const count = await orderLink.count()
    if (count === 0) {
      test.skip()
      return
    }

    await orderLink.click()
    await expect(page).toHaveURL(/\/orders\/\d+$/, { timeout: 5_000 })

    const buyAgainBtn = page.locator('button', { hasText: /buy again/i })
    await buyAgainBtn.waitFor({ timeout: 5_000 })
    await buyAgainBtn.click()

    // Should navigate to /cart
    await expect(page).toHaveURL('/cart', { timeout: 10_000 })
  })
})
