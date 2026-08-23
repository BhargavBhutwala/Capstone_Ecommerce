/**
 * React Router configuration.
 *
 * Defines all routes for the MVP:
 * - Public routes: catalog, categories, brands, product search, product detail
 * - Public-only routes: login, register
 * - Protected routes: cart, addresses, checkout, orders, payments
 */

import { createBrowserRouter } from 'react-router-dom'
import { AppLayout } from '../app/layout/AppLayout'
import { ProtectedRoute } from './ProtectedRoute'
import { PublicOnlyRoute } from './PublicOnlyRoute'
import { NotFoundPage } from '../components/states/NotFoundPage'
import { LoginPage } from '../features/auth/LoginPage'
import { RegisterPage } from '../features/auth/RegisterPage'
import { HomePage } from '../features/catalog/HomePage'
import { ProductListPage } from '../features/catalog/ProductListPage'
import { CategoryPage } from '../features/catalog/CategoryPage'
import { BrandPage } from '../features/catalog/BrandPage'
import { ProductDetailPage } from '../features/catalog/ProductDetailPage'
import { CartPage } from '../features/cart/CartPage'
import { AddressesPage } from '../features/address/AddressesPage'
import { CheckoutPage } from '../features/checkout/CheckoutPage'
import { OrdersPage } from '../features/orders/OrdersPage'
import { PaymentPage } from '../features/payment/PaymentPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      // ── Public routes ────────────────────────────────────────────────────
      { index: true, element: <HomePage /> },

      // Catalog — FE-03
      { path: 'products', element: <ProductListPage /> },
      { path: 'products/:productId', element: <ProductDetailPage /> },
      { path: 'categories/:categoryId', element: <CategoryPage /> },
      { path: 'brands/:brandId', element: <BrandPage /> },

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
          { path: 'checkout', element: <CheckoutPage /> },
          { path: 'orders', element: <OrdersPage /> },
          // FE-07: /orders/:orderId
          { path: 'orders/:orderId/payment', element: <PaymentPage /> },
        ],
      },

      // ── Not found ───────────────────────────────────────────────────────
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
