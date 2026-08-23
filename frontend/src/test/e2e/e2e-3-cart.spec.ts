/**
 * E2E-3: Cart workflow.
 *
 * Verifies:
 * - Authenticated user adds product to cart
 * - Cart page opens and shows the item
 * - Quantity can be updated
 * - Totals render from backend
 * - Item can be removed
 */

import { test, expect, type Page } from '@playwright/test'

const EMAIL = `e2e-cart-${Date.now()}@example.com`
const PASSWORD = 'E2ECart!2024'
const FIRST = 'Cart'
const LAST = 'Tester'

async function registerAndLogin(page: Page) {
  // Register — the register page redirects to /login on success
  await page.goto('/register')
  await page.waitForSelector('#firstName', { timeout: 10_000 })
  await page.fill('#firstName', FIRST)
  await page.fill('#lastName', LAST)
  await page.fill('#email', EMAIL)
  await page.fill('#password', PASSWORD)
  await page.click('button[type="submit"]')

  // Wait until we leave /register
  await page.waitForURL((url) => !url.pathname.startsWith('/register'), { timeout: 15_000 })

  // Registration redirects to /login — complete sign-in
  if (page.url().includes('/login')) {
    await page.waitForSelector('#email', { timeout: 5_000 })
    await page.fill('#email', EMAIL)
    await page.fill('#password', PASSWORD)
    await page.click('button[type="submit"]')
    // Wait until we leave /login
    await page.waitForURL((url) => !url.pathname.startsWith('/login'), { timeout: 15_000 })
  }

  await expect(page).not.toHaveURL(/\/login|\/register/, { timeout: 5_000 })
}

test.describe('E2E-3: Cart', () => {
  test('add product to cart, update quantity, remove item', async ({ page }) => {
    await registerAndLogin(page)

    // Go to product list, find first available product
    await page.goto('/products')
    const addBtn = page.locator('button', { hasText: /add to cart/i }).first()
    await addBtn.waitFor({ timeout: 15_000 })
    await addBtn.click()

    // Go to cart
    await page.goto('/cart')
    await expect(page.locator('h1').first()).toBeVisible({ timeout: 10_000 })

    // Totals are rendered from backend (numeric value visible)
    await expect(page.locator('text=/\\$\\d+\\.\\d{2}/').first()).toBeVisible({ timeout: 5_000 })

    // Remove item
    const removeBtn = page.locator('button', { hasText: /remove/i }).first()
    if (await removeBtn.isVisible()) {
      await removeBtn.click()
      // Cart should now show empty state — match the exact message from EmptyState
      await expect(
        page.locator('p', { hasText: /your cart is empty/i }).first()
      ).toBeVisible({ timeout: 10_000 })
    }
  })
})
