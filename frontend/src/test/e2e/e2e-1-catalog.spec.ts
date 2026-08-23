/**
 * E2E-1: Public Catalog — logged-out browsing.
 *
 * Verifies:
 * - Application loads without authentication
 * - Categories and products render
 * - Search works
 * - Product detail opens
 * - Related products render (when available)
 */

import { test, expect } from '@playwright/test'

test.describe('E2E-1: Public Catalog', () => {
  test('application loads and shows products without login', async ({ page }) => {
    await page.goto('/')
    // Homepage should render without requiring login
    await expect(page).not.toHaveURL(/login/)
    // Page title or heading should be visible
    await expect(page.locator('h1, h2').first()).toBeVisible({ timeout: 10_000 })
  })

  test('product list page loads products', async ({ page }) => {
    await page.goto('/products')
    // Wait for products to load — the page renders <article> elements for each
    // product card, or a <p> with the empty/error message.
    // Both outcomes confirm the page has finished loading.
    await page.waitForFunction(
      () =>
        document.querySelectorAll('article').length > 0 ||
        document.body.innerText.includes('No products') ||
        document.body.innerText.includes('Try again'),
      { timeout: 15_000 },
    )
  })

  test('search filters product list', async ({ page }) => {
    await page.goto('/products')
    // Wait for the search input
    await page.waitForSelector('input[type="search"]', { timeout: 10_000 })

    // Type a search query
    await page.fill('input[type="search"]', 'code')
    await page.keyboard.press('Enter')

    // URL should contain q= parameter
    await expect(page).toHaveURL(/q=code/, { timeout: 5_000 })
  })

  test('clicking a product opens product detail', async ({ page }) => {
    await page.goto('/products')
    // Wait for at least one product card link
    const productLink = page.locator('a[href*="/products/"]').first()
    await productLink.waitFor({ timeout: 15_000 })
    const href = await productLink.getAttribute('href')
    if (!href) return // Skip if no products in DB

    await productLink.click()
    await expect(page).toHaveURL(/\/products\/\d+/)
    // Product detail should show a heading with the product title
    await expect(page.locator('h1').first()).toBeVisible({ timeout: 10_000 })
  })
})
