/**
 * E2E-2: Authentication.
 *
 * Verifies:
 * - Register or login with a test user
 * - Protected route becomes accessible after login
 * - Logout returns to unauthenticated state
 */

import { test, expect } from '@playwright/test'
import { TEST_USER } from './helpers'

test.describe('E2E-2: Authentication', () => {
  // Use a unique email per run so registration doesn't conflict
  const email = `e2e-auth-${Date.now()}@example.com`

  test('registers a new user and accesses protected route', async ({ page }) => {
    await page.goto('/register')
    await page.waitForSelector('#firstName', { timeout: 10_000 })

    await page.fill('#firstName', TEST_USER.firstName)
    await page.fill('#lastName', TEST_USER.lastName)
    await page.fill('#email', email)
    await page.fill('#password', TEST_USER.password)
    await page.click('button[type="submit"]')

    // After registration the app redirects to /login — accept any redirect away from /register
    await expect(page).not.toHaveURL('/register', { timeout: 10_000 })
  })

  test('login with valid credentials and access cart', async ({ page }) => {
    // Register first (if needed) then login
    await page.goto('/login')
    await expect(page.locator('h1, h2').first()).toBeVisible({ timeout: 10_000 })

    await page.fill('input[type="email"]', email)
    await page.fill('input[type="password"]', TEST_USER.password)
    await page.click('button[type="submit"]')

    // After login — should redirect away from login
    await expect(page).not.toHaveURL('/login', { timeout: 10_000 })

    // Access protected route
    await page.goto('/cart')
    await expect(page).toHaveURL('/cart', { timeout: 5_000 })
    await expect(page.locator('h1').first()).toBeVisible({ timeout: 10_000 })
  })

  test('protected route redirects unauthenticated user to /login', async ({ page }) => {
    // Fresh context — no session
    await page.goto('/cart')
    await expect(page).toHaveURL(/login/, { timeout: 5_000 })
  })

  test('logout clears session and redirects to unauthenticated state', async ({ page }) => {
    // Login first
    await page.goto('/login')
    await page.fill('input[type="email"]', email)
    await page.fill('input[type="password"]', TEST_USER.password)
    await page.click('button[type="submit"]')
    await expect(page).not.toHaveURL('/login', { timeout: 10_000 })

    // Logout
    const logoutBtn = page.locator('button', { hasText: /logout|sign out/i })
    await logoutBtn.waitFor({ timeout: 5_000 })
    await logoutBtn.click()

    // After logout, navigating to /cart should redirect to login
    await page.goto('/cart')
    await expect(page).toHaveURL(/login/, { timeout: 5_000 })
  })
})
