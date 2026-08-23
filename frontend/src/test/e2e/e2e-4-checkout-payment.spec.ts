/**
 * E2E-4: Checkout + Payment.
 *
 * Verifies:
 * - Authenticated user with items in cart
 * - Create/select delivery address
 * - Checkout creates order
 * - Navigate to payment
 * - Choose CREDIT_CARD or DEBIT_CARD
 * - Initiate simulated payment
 * - Verify SUCCESS confirmation
 */

import { test, expect, type Page } from '@playwright/test'

const EMAIL = `e2e-checkout-${Date.now()}@example.com`
const PASSWORD = 'E2ECheckout!2024'

async function registerAndLogin(page: Page) {
  await page.goto('/register')
  await page.waitForSelector('input[type="email"]', { timeout: 10_000 })
  await page.fill('input[name="firstName"], input[id*="firstName"], input[placeholder*="First"]', 'Checkout')
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

async function addProductToCart(page: Page) {
  await page.goto('/products')
  const addBtn = page.locator('button', { hasText: /add to cart/i }).first()
  await addBtn.waitFor({ timeout: 15_000 })
  await addBtn.click()
  await page.waitForTimeout(500)
}

async function createAddress(page: Page) {
  await page.goto('/addresses')
  // Click "Add address" or similar button
  const addAddrBtn = page.locator('button', { hasText: /add.*address|new address/i }).first()
  if (await addAddrBtn.isVisible({ timeout: 3_000 })) {
    await addAddrBtn.click()
  }
  await page.waitForSelector('input[id*="addr-line1"], input[autocomplete="address-line1"]', { timeout: 5_000 })
  await page.fill('input[id*="addr-line1"], input[autocomplete="address-line1"]', '123 Main Street')
  await page.fill('input[id*="addr-city"], input[autocomplete="address-level2"]', 'Springfield')
  await page.fill('input[id*="addr-state"], input[autocomplete="address-level1"]', 'IL')
  await page.fill('input[id*="addr-postal"], input[autocomplete="postal-code"]', '62701')
  await page.fill('input[id*="addr-country"], input[autocomplete="country-name"]', 'USA')
  await page.click('button[type="submit"]')
  await page.waitForTimeout(1000)
}

test.describe('E2E-4: Checkout + Payment', () => {
  test('full checkout and payment flow', async ({ page }) => {
    await registerAndLogin(page)
    await addProductToCart(page)
    await createAddress(page)

    // Navigate to checkout
    await page.goto('/checkout')
    await expect(page.locator('h1').first()).toBeVisible({ timeout: 10_000 })

    // Select address (auto-selected or click first)
    const addrRadio = page.locator('input[type="radio"][name="shippingAddress"]').first()
    if (await addrRadio.isVisible({ timeout: 3_000 })) {
      await addrRadio.click()
    }

    // Place order
    await page.locator('button', { hasText: /place order/i }).click()

    // Should navigate to payment page
    await expect(page).toHaveURL(/orders\/\d+\/payment/, { timeout: 15_000 })

    // Select CREDIT_CARD (default) and pay
    const creditCardRadio = page.locator('input[value="CREDIT_CARD"]')
    await expect(creditCardRadio).toBeVisible({ timeout: 5_000 })
    await expect(creditCardRadio).toBeChecked()

    await page.locator('button', { hasText: /pay now/i }).click()

    // Wait for payment result — should show SUCCESS
    await expect(
      page.locator('text=/payment successful/i'),
    ).toBeVisible({ timeout: 20_000 })
  })
})
