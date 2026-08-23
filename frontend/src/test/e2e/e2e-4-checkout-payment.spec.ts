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
  await page.waitForSelector('#firstName', { timeout: 10_000 })
  await page.fill('#firstName', 'Checkout')
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

async function addProductToCart(page: Page) {
  await page.goto('/products')
  const addBtn = page.locator('button', { hasText: /add to cart/i }).first()
  await addBtn.waitFor({ timeout: 15_000 })
  await addBtn.click()
  await page.waitForTimeout(500)
}

async function createAddress(page: Page) {
  await page.goto('/addresses')
  // Wait for page to load, then click Add address to reveal the form
  await page.waitForSelector('h1', { timeout: 10_000 })
  await page.locator('button', { hasText: /\+\s*add address/i }).click()
  await page.waitForSelector('#addr-line1', { timeout: 10_000 })
  await page.fill('#addr-line1', '123 Main Street')
  await page.fill('#addr-city', 'Springfield')
  await page.fill('#addr-state', 'IL')
  await page.fill('#addr-postal', '62701')
  await page.fill('#addr-country', 'USA')
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
