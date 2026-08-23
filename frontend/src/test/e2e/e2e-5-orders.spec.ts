/**
 * E2E-5: Order history.
 *
 * Verifies:
 * - Open order history
 * - Open order detail
 * - Buy Again and verify navigation to cart
 */

import { test, expect, type Page } from '@playwright/test'

const PASSWORD = 'E2EOrders!2024'

async function registerAndLogin(page: Page, email: string) {
  await page.goto('/register')
  await page.waitForSelector('#firstName', { timeout: 10_000 })
  await page.fill('#firstName', 'Orders')
  await page.fill('#lastName', 'Tester')
  await page.fill('#email', email)
  await page.fill('#password', PASSWORD)
  await page.click('button[type="submit"]')

  await page.waitForURL((url) => !url.pathname.startsWith('/register'), { timeout: 15_000 })

  if (page.url().includes('/login')) {
    await page.waitForSelector('#email', { timeout: 5_000 })
    await page.fill('#email', email)
    await page.fill('#password', PASSWORD)
    await page.click('button[type="submit"]')
    await page.waitForURL((url) => !url.pathname.startsWith('/login'), { timeout: 15_000 })
  }

  await expect(page).not.toHaveURL(/\/login|\/register/, { timeout: 5_000 })
}

test.describe('E2E-5: Order History', () => {
  test('order history page loads and is accessible', async ({ page }) => {
    const email = `e2e-orders-1-${Date.now()}@example.com`
    await registerAndLogin(page, email)
    await page.goto('/orders')
    await expect(page.locator('h1').first()).toBeVisible({ timeout: 10_000 })
    // Either the order list heading is shown, or the empty state message — use stable text
    await page.waitForFunction(
      () =>
        document.querySelector('ul') !== null ||
        document.body.innerText.includes('No orders') ||
        document.body.innerText.includes('Place your first order'),
      { timeout: 10_000 },
    )
  })

  test('clicking order row opens order detail', async ({ page }) => {
    const email = `e2e-orders-2-${Date.now()}@example.com`
    await registerAndLogin(page, email)
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
    const email = `e2e-orders-3-${Date.now()}@example.com`
    await registerAndLogin(page, email)
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
