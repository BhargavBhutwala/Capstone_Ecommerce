/**
 * React Router configuration.
 *
 * Defines all routes for the MVP:
 * - Public routes: catalog, product detail (FE-03)
 * - Public-only routes: login, register
 * - Protected routes: cart, addresses, checkout, orders, payments
 *
 * Routes not yet implemented render placeholder pages so the route structure
 * is stable before features are added.
 */

import { createBrowserRouter } from 'react-router-dom'
import { AppLayout } from '../app/layout/AppLayout'
import { ProtectedRoute } from './ProtectedRoute'
import { PublicOnlyRoute } from './PublicOnlyRoute'
import { NotFoundPage } from '../components/states/NotFoundPage'
import { LoginPage } from '../features/auth/LoginPage'
import { RegisterPage } from '../features/auth/RegisterPage'
import { HomePage } from '../features/catalog/HomePage'
import { CartPage } from '../features/cart/CartPage'
import { AddressesPage } from '../features/address/AddressesPage'
import { OrdersPage } from '../features/orders/OrdersPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      // ── Public routes ────────────────────────────────────────────────────
      { index: true, element: <HomePage /> },
      // FE-03 will add:
      // /categories/:categoryId
      // /brands/:brandId
      // /products
      // /products/:productId

      // ── Public-only routes (redirect authenticated users away) ────────────
      {
        element: <PublicOnlyRoute />,
        children: [
          { path: 'login', element: <LoginPage /> },
          { path: 'register', element: <RegisterPage /> },
        ],
      },

      // ── Protected routes (require authentication) ─────────────────────────
      {
        element: <ProtectedRoute />,
        children: [
          { path: 'cart', element: <CartPage /> },
          { path: 'addresses', element: <AddressesPage /> },
          // FE-05: /checkout
          { path: 'orders', element: <OrdersPage /> },
          // FE-07: /orders/:orderId
          // FE-06: /orders/:orderId/payment
        ],
      },

      // ── Not found ───────────────────────────────────────────────────────
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
