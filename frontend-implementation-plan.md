# Frontend MVP Implementation Plan

## Overview

This plan covers only the frontend implementation for the E-Bookstore MVP. The backend Spring Boot API remains the source of truth for all integration behavior, request/response contracts, authentication behavior, pagination, and error handling as defined in [`docs/03-openapi-specification.yaml`](docs/03-openapi-specification.yaml).

The repository currently contains no frontend application and no authoritative frontend framework selection in the project documents. Based on the explicit design decision provided for this planning session, the frontend plan targets React, TypeScript, and Vite. This is an implementation choice for the frontend only and does not change the backend contract defined in [`docs/03-openapi-specification.yaml`](docs/03-openapi-specification.yaml).

The plan stays within the strict MVP boundary established by [`AGENTS.md`](AGENTS.md), [`docs/01-requirements-specification.md`](docs/01-requirements-specification.md), [`docs/02-data-model-design.md`](docs/02-data-model-design.md), and [`mvp-implementation-plan.md`](mvp-implementation-plan.md). It excludes gift points, coupons, shipments, returns, and refunds.

## Sub-Task 1 — Frontend foundation and build setup

- **Intent** — Establish a standalone frontend application with the minimum tooling needed to build, run, and test the MVP UI against the existing backend API.
- **Expected Outcomes**
  - A new frontend app exists in a dedicated top-level directory.
  - React, TypeScript, and Vite are configured for local development.
  - Environment-based backend API configuration is available.
  - Basic quality tooling is defined for linting, formatting, and unit/component tests.
- **Todo List**
  1. Create a new top-level [`frontend`](frontend) application folder to avoid mixing Node tooling into the Spring Boot build.
  2. Initialize Vite with React and TypeScript.
  3. Add essential frontend tooling only: router, HTTP client, test runner, DOM testing utilities, and form validation library.
  4. Add environment files for local API base URL configuration pointing to [`http://localhost:8080/api`](docs/03-openapi-specification.yaml:15).
  5. Add scripts for dev, build, preview, lint, test, and integration test execution.
  6. Add a README section for frontend local startup alongside the existing backend setup guidance in [`mvp-implementation-plan.md`](mvp-implementation-plan.md).
- **Relevant Context**
  - [`AGENTS.md`](AGENTS.md)
  - [`mvp-implementation-plan.md`](mvp-implementation-plan.md)
  - [`docs/03-openapi-specification.yaml`](docs/03-openapi-specification.yaml:14)
- **Status** — [ ] pending

## Sub-Task 2 — Frontend project directory and package structure

- **Intent** — Define a clear, domain-oriented frontend structure aligned with the backend domains so implementation stays modular and reviewable.
- **Expected Outcomes**
  - A stable directory structure exists for app shell, routes, features, API, shared UI, and tests.
  - Shared types and utilities are separated from feature-specific code.
- **Todo List**
  1. Create a structure under [`frontend/src`](frontend/src) with `app`, `routes`, `features`, `api`, `components`, `hooks`, `types`, `utils`, and `test`.
  2. Organize feature folders around frontend domains: `auth`, `catalog`, `cart`, `address`, `checkout`, `payment`, and `orders`.
  3. Keep page components, feature components, hooks, and API helpers close to their owning domain.
  4. Reserve a shared `types` layer for OpenAPI-aligned DTOs used across multiple features.
  5. Reserve a shared `components` layer only for truly reusable UI states such as loading, empty, error, form field, and pagination controls.
- **Relevant Context**
  - Backend modular structure guidance in [`AGENTS.md`](AGENTS.md:110)
  - Backend feature sequencing in [`mvp-implementation-plan.md`](mvp-implementation-plan.md:85)
- **Status** — [ ] pending

## Sub-Task 3 — Routing and navigation shell

- **Intent** — Define public and protected navigation that matches the MVP customer journey without introducing unsupported flows.
- **Expected Outcomes**
  - Public catalog browsing routes exist.
  - Authenticated customer routes exist for cart, addresses, checkout, payments, and orders.
  - Route guards distinguish public endpoints from authenticated workflows.
- **Todo List**
  1. Create a root application shell with top-level layout, header, footer, and route outlet.
  2. Define public routes for home/catalog, category listing, brand listing, search results, product detail, login, and registration.
  3. Define protected routes for cart, addresses, checkout, payment, orders list, and order detail.
  4. Add route-level not-found handling.
  5. Add redirect behavior so protected route access without authentication leads to login and preserves intended destination.
  6. Keep catalog routes public because the backend contract explicitly exposes these endpoints with `security: []` in [`docs/03-openapi-specification.yaml`](docs/03-openapi-specification.yaml:42).
- **Relevant Context**
  - Customer journeys in [`docs/01-requirements-specification.md`](docs/01-requirements-specification.md:545)
  - Public catalog security exceptions in [`docs/03-openapi-specification.yaml`](docs/03-openapi-specification.yaml:116)
- **Status** — [ ] pending

## Sub-Task 4 — Authentication and JWT session handling

- **Intent** — Implement frontend session management that conforms to the backend JWT contract without inventing token refresh or alternative auth flows.
- **Expected Outcomes**
  - Registration, login, logout, and current-user bootstrap flows are planned around backend behavior.
  - JWT is attached only to authenticated API requests.
  - Session restore on reload is supported using the login response token.
- **Todo List**
  1. Model [`LoginResponse`](docs/03-openapi-specification.yaml:922) and [`UserResponse`](docs/03-openapi-specification.yaml:938) in frontend types.
  2. Store the bearer token for the active session and hydrate auth state on app startup.
  3. Call [`getCurrentUser`](docs/03-openapi-specification.yaml:105) after bootstrap when a token is present to validate the session.
  4. Implement login and registration forms against [`login`](docs/03-openapi-specification.yaml:71) and [`registerUser`](docs/03-openapi-specification.yaml:43).
  5. Implement logout against [`logout`](docs/03-openapi-specification.yaml:94), then clear token, user state, and any user-scoped cached data.
  6. Handle unauthorized responses centrally by clearing invalid session state and redirecting to login when appropriate.
  7. Do not plan refresh tokens because none are defined in [`docs/03-openapi-specification.yaml`](docs/03-openapi-specification.yaml:922).
- **Relevant Context**
  - Auth endpoints in [`docs/03-openapi-specification.yaml`](docs/03-openapi-specification.yaml:38)
  - Security rules in [`AGENTS.md`](AGENTS.md:884)
- **Status** — [ ] pending

## Sub-Task 5 — API client, service architecture, and shared error handling

- **Intent** — Centralize API communication so all frontend features use the same request conventions, auth header injection, pagination typing, and [`ErrorResponse`](docs/03-openapi-specification.yaml:1424) parsing.
- **Expected Outcomes**
  - A single configured API client exists.
  - Domain service modules map directly to backend operationIds.
  - Shared error normalization exists for forms, toasts, page-level errors, and retry actions.
- **Todo List**
  1. Create a base API client with request timeout, JSON parsing, and authorization header support.
  2. Define typed request helpers for GET, POST, PUT, and DELETE.
  3. Create service modules by domain: `authApi`, `catalogApi`, `addressApi`, `cartApi`, `ordersApi`, and `paymentsApi`.
  4. Model paged responses using the exact `{ content, page }` envelope from [`PagedProductResponse`](docs/03-openapi-specification.yaml:1462) and [`PagedOrderResponse`](docs/03-openapi-specification.yaml:1473).
  5. Parse backend [`ErrorResponse`](docs/03-openapi-specification.yaml:1424) into a shared frontend error model preserving `status`, `code`, `message`, `path`, and optional `fieldErrors`.
  6. Route validation errors to forms and business-rule conflicts such as `409` to contextual inline messages.
  7. Avoid inventing frontend-only API wrappers that change endpoint semantics or payload shapes.
- **Relevant Context**
  - [`ErrorResponse`](docs/03-openapi-specification.yaml:1424)
  - Pagination contract in [`AGENTS.md`](AGENTS.md:787)
- **Status** — [ ] pending

## Sub-Task 6 — Shared state management strategy

- **Intent** — Define minimal frontend state ownership so authentication, cart state, and server data remain consistent without overengineering.
- **Expected Outcomes**
  - Clear split exists between server state and client UI state.
  - Auth and cart state lifecycles are defined.
- **Todo List**
  1. Use server-state tooling for API-backed queries, caching, invalidation, and mutations.
  2. Use lightweight client state for session token, auth user snapshot, redirect intent, and transient UI state.
  3. Treat cart, addresses, products, orders, and payment status as server state rather than duplicating them in global client stores.
  4. Define cache invalidation after mutations such as add-to-cart, address create/update/delete, order creation, buy again, cancellation, and payment initiation.
  5. Keep the state model small and aligned to actual MVP workflows.
- **Relevant Context**
  - Core user journeys in [`docs/01-requirements-specification.md`](docs/01-requirements-specification.md:545)
  - Backend transactional cart/order behavior in [`AGENTS.md`](AGENTS.md:982)
- **Status** — [ ] pending

## Sub-Task 7 — Catalog, product detail, and related product experience

- **Intent** — Implement all public browsing flows defined by the MVP: categories, brands, search, product detail, and related products.
- **Expected Outcomes**
  - Guest users can browse categories and brands.
  - Users can search and filter products with backend-driven pagination and sort.
  - Product detail pages show related products and add-to-cart entry points.
- **Todo List**
  1. Build a landing/catalog page using [`listCategories`](docs/03-openapi-specification.yaml:121), [`listBrands`](docs/03-openapi-specification.yaml:154), and [`searchProducts`](docs/03-openapi-specification.yaml:187).
  2. Add category result pages backed by [`getProductsByCategory`](docs/03-openapi-specification.yaml:137).
  3. Add brand result pages backed by [`getProductsByBrand`](docs/03-openapi-specification.yaml:170).
  4. Add search/filter UI for `q`, `categoryId`, `brandId`, `minPrice`, `maxPrice`, `availableOnly`, `page`, `size`, and `sort` exactly as defined in [`docs/03-openapi-specification.yaml`](docs/03-openapi-specification.yaml:188).
  5. Add product detail page backed by [`getProduct`](docs/03-openapi-specification.yaml:234).
  6. Add related products section backed by [`getRelatedProducts`](docs/03-openapi-specification.yaml:252).
  7. Respect product availability indicators from [`ProductSummary`](docs/03-openapi-specification.yaml:988) and [`ProductResponse`](docs/03-openapi-specification.yaml:1009).
- **Relevant Context**
  - Catalog requirements in [`docs/01-requirements-specification.md`](docs/01-requirements-specification.md:162)
  - Related-product design in [`docs/02-data-model-design.md`](docs/02-data-model-design.md:679)
- **Status** — [ ] pending

## Sub-Task 8 — Cart workflow

- **Intent** — Implement the authenticated cart experience against the backend active-cart model without introducing client-side cart authority.
- **Expected Outcomes**
  - Users can view cart contents, add items, update quantities, and remove items.
  - Server-calculated pricing and availability remain authoritative.
- **Todo List**
  1. Build cart page backed by [`getCart`](docs/03-openapi-specification.yaml:352).
  2. Add add-to-cart actions using [`addCartItem`](docs/03-openapi-specification.yaml:367) from catalog and product pages.
  3. Add quantity update controls using [`updateCartItem`](docs/03-openapi-specification.yaml:398).
  4. Add remove-item controls using [`removeCartItem`](docs/03-openapi-specification.yaml:430).
  5. Display server-returned subtotal and total from [`CartResponse`](docs/03-openapi-specification.yaml:1126) rather than recalculating purchase totals independently.
  6. Display backend `409` conflicts for stock or availability changes inline near the affected cart action.
  7. Show `recommendedProducts` if present in [`CartResponse`](docs/03-openapi-specification.yaml:1143), but do not invent new recommendation endpoints.
- **Relevant Context**
  - Cart requirements in [`docs/01-requirements-specification.md`](docs/01-requirements-specification.md:228)
  - Cart price rules in [`AGENTS.md`](AGENTS.md:488)
- **Status** — [ ] pending

## Sub-Task 9 — Address management

- **Intent** — Implement authenticated CRUD for delivery addresses so checkout can use backend-managed address records.
- **Expected Outcomes**
  - Users can list, create, update, delete, and choose default addresses.
  - Address forms align with backend request validation.
- **Todo List**
  1. Build addresses page backed by [`listAddresses`](docs/03-openapi-specification.yaml:272).
  2. Add address create form using [`createAddress`](docs/03-openapi-specification.yaml:288).
  3. Add address edit form using [`updateAddress`](docs/03-openapi-specification.yaml:311).
  4. Add address delete action using [`deleteAddress`](docs/03-openapi-specification.yaml:337).
  5. Reflect `isDefault` behavior from [`AddressRequest`](docs/03-openapi-specification.yaml:1034) and [`AddressResponse`](docs/03-openapi-specification.yaml:1073).
  6. Reuse the same address form component for create and update flows.
  7. Keep checkout address selection based on stored addresses only because order creation requires `addressId` in [`CreateOrderRequest`](docs/03-openapi-specification.yaml:1152).
- **Relevant Context**
  - Address entity design in [`docs/02-data-model-design.md`](docs/02-data-model-design.md:171)
  - Checkout address rule in [`docs/01-requirements-specification.md`](docs/01-requirements-specification.md:292)
- **Status** — [ ] pending

## Sub-Task 10 — Checkout and order creation flow

- **Intent** — Build the MVP checkout flow that creates an order from the active cart using a selected saved address.
- **Expected Outcomes**
  - Checkout page summarizes cart items and requires address selection.
  - Successful checkout creates an order and transitions the user to payment.
  - Empty-cart and conflict scenarios are handled explicitly.
- **Todo List**
  1. Build checkout page that loads current cart and saved addresses together.
  2. Require selection of an existing address ID for order creation using [`createOrder`](docs/03-openapi-specification.yaml:462).
  3. Display order preview values based on the latest cart response and clearly label that final server validation occurs during checkout.
  4. Handle `409` errors from [`createOrder`](docs/03-openapi-specification.yaml:480) as cart/order conflicts requiring user review.
  5. On success, route to payment initiation for the returned order.
  6. Do not include gift point or coupon inputs because they are outside MVP scope per [`AGENTS.md`](AGENTS.md:594) and [`AGENTS.md`](AGENTS.md:619).
- **Relevant Context**
  - Checkout journey in [`docs/01-requirements-specification.md`](docs/01-requirements-specification.md:566)
  - Atomic checkout rule in [`AGENTS.md`](AGENTS.md:982)
- **Status** — [ ] pending

## Sub-Task 11 — Payment flow

- **Intent** — Implement the MVP payment UI around simulated payment initiation and payment status retrieval.
- **Expected Outcomes**
  - Users can initiate payment for a newly created order using supported methods only.
  - Payment result and status can be viewed from the frontend.
- **Todo List**
  1. Build payment page for an order using [`initiatePayment`](docs/03-openapi-specification.yaml:560).
  2. Restrict payment method selection to [`CREDIT_CARD`](docs/03-openapi-specification.yaml:1528) and [`DEBIT_CARD`](docs/03-openapi-specification.yaml:1528).
  3. Use backend amount and order linkage from [`PaymentResponse`](docs/03-openapi-specification.yaml:1268).
  4. Build payment status retrieval using [`getPayment`](docs/03-openapi-specification.yaml:589) for refresh/revisit scenarios.
  5. Show payment status values exactly as defined in [`PaymentStatus`](docs/03-openapi-specification.yaml:1534).
  6. Handle duplicate or invalid payment initiation conflicts through shared error presentation.
- **Relevant Context**
  - Payment requirements in [`docs/01-requirements-specification.md`](docs/01-requirements-specification.md:324)
  - MVP payment simulation rule in [`AGENTS.md`](AGENTS.md:1061)
- **Status** — [ ] pending

## Sub-Task 12 — Order history, order detail, buy again, and cancellation

- **Intent** — Implement the post-purchase customer workflows supported by the MVP backend.
- **Expected Outcomes**
  - Users can browse paginated order history.
  - Users can inspect order details, buy again, and cancel eligible orders.
  - Order state and cancellation eligibility are displayed from backend data.
- **Todo List**
  1. Build order history page using [`listOrders`](docs/03-openapi-specification.yaml:445) with backend pagination envelope and optional `status` filter only.
  2. Preserve backend order sort expectations; do not add unsupported client-side sort contract for order history.
  3. Build order detail page using [`getOrder`](docs/03-openapi-specification.yaml:491).
  4. Show shipping address snapshot from [`OrderResponse`](docs/03-openapi-specification.yaml:1210) as historical order data, not editable address state.
  5. Add buy-again action using [`buyAgain`](docs/03-openapi-specification.yaml:510) and redirect to cart on success.
  6. Add cancel-order action using [`cancelOrder`](docs/03-openapi-specification.yaml:535) only when the current order data indicates it is still eligible.
  7. Display `cancellationDeadline` from [`OrderResponse`](docs/03-openapi-specification.yaml:1252) and handle backend `409` cancellation denial gracefully.
- **Relevant Context**
  - Order history requirements in [`docs/01-requirements-specification.md`](docs/01-requirements-specification.md:260)
  - Cancellation rule in [`AGENTS.md`](AGENTS.md:442)
  - Buy-again rule in [`AGENTS.md`](AGENTS.md:1045)
- **Status** — [ ] pending

## Sub-Task 13 — Shared UX states, validation, and route protection behavior

- **Intent** — Standardize UX behavior for loading, empty, error, validation, and protected-route access so all screens behave consistently.
- **Expected Outcomes**
  - Shared loading, empty, error, and retry patterns exist.
  - Forms validate against backend constraints before submit while still honoring server validation.
  - Protected/public route behavior is consistent across the app.
- **Todo List**
  1. Create shared page and section state components for loading, empty, inline error, and retry.
  2. Define empty states for catalog no-results, empty cart, no addresses, and no orders.
  3. Add client validation aligned with OpenAPI field constraints for registration, login, addresses, cart quantity, order creation, and payment method selection.
  4. Preserve server-side validation authority by surfacing backend `fieldErrors` from [`ErrorResponse`](docs/03-openapi-specification.yaml:1439).
  5. Add public-route behavior for login/register when already authenticated, such as redirecting away from auth pages.
  6. Add protected-route behavior for expired or missing token sessions.
- **Relevant Context**
  - [`ErrorResponse`](docs/03-openapi-specification.yaml:1424)
  - Validation rules in request schemas such as [`RegisterRequest`](docs/03-openapi-specification.yaml:889), [`AddressRequest`](docs/03-openapi-specification.yaml:1034), [`AddCartItemRequest`](docs/03-openapi-specification.yaml:1083), [`CreateOrderRequest`](docs/03-openapi-specification.yaml:1152), and [`CreatePaymentRequest`](docs/03-openapi-specification.yaml:1257)
- **Status** — [ ] pending

## Sub-Task 14 — OperationId mapping, environment config, testing, definition of done, and git milestones

- **Intent** — Finalize the integration contract map, local setup, test strategy, completion criteria, and delivery sequencing.
- **Expected Outcomes**
  - Every required backend operationId is mapped to a frontend service function and consuming screen.
  - Environment setup and testing layers are defined.
  - Git milestones are scoped into small, reviewable frontend deliveries.
- **Todo List**
  1. Define the backend operationId to frontend API module mapping listed below.
  2. Define environment variables for API base URL and any frontend-only runtime flags.
  3. Define local development workflow for running backend on port 8080 and frontend dev server separately.
  4. Define unit/component, route-level, and API integration test coverage.
  5. Define MVP frontend definition of done.
  6. Define git milestones aligned to user-facing slices.
- **Relevant Context**
  - Backend server URL in [`docs/03-openapi-specification.yaml`](docs/03-openapi-specification.yaml:15)
  - Backend milestone pattern in [`mvp-implementation-plan.md`](mvp-implementation-plan.md:1737)
- **Status** — [ ] pending

## Frontend technology and build setup

- Framework: React with TypeScript and Vite, based on the user-provided frontend decision for this plan.
- Package manager: use one Node package manager consistently for the frontend workspace.
- Recommended minimum tooling:
  - React Router for routing
  - Axios or fetch-based typed wrapper for HTTP
  - React Query or equivalent server-state library for query/mutation management
  - React Hook Form with schema-based validation
  - Vitest and Testing Library for unit/component testing
  - Playwright for browser-level integration testing
- Frontend app location: top-level [`frontend`](frontend)
- Backend remains separate in the existing Maven/Spring Boot structure.

## Proposed frontend directory/package structure

```text
frontend/
  src/
    app/
      App.tsx
      providers/
      layout/
    routes/
      index.tsx
      ProtectedRoute.tsx
      PublicOnlyRoute.tsx
    api/
      client.ts
      errors.ts
      authApi.ts
      catalogApi.ts
      addressApi.ts
      cartApi.ts
      ordersApi.ts
      paymentsApi.ts
      types.ts
    features/
      auth/
      catalog/
      cart/
      address/
      checkout/
      payment/
      orders/
    components/
      ui/
      states/
      forms/
    hooks/
    utils/
    test/
    main.tsx
```

## Routing and navigation plan

Public routes:
- `/`
- `/login`
- `/register`
- `/categories/:categoryId`
- `/brands/:brandId`
- `/products`
- `/products/:productId`

Protected routes:
- `/cart`
- `/addresses`
- `/checkout`
- `/orders`
- `/orders/:orderId`
- `/orders/:orderId/payment`

Behavior:
- Public catalog routes remain accessible without authentication because backend catalog endpoints use `security: []` in [`docs/03-openapi-specification.yaml`](docs/03-openapi-specification.yaml:120).
- Protected routes require a valid JWT-backed session.
- Unauthenticated access to protected routes redirects to login and preserves intended destination.
- Authenticated users visiting login/register should be redirected to the most relevant authenticated destination.

## Authentication and JWT handling plan

- Use [`login`](docs/03-openapi-specification.yaml:71) to obtain [`accessToken`](docs/03-openapi-specification.yaml:926), [`tokenType`](docs/03-openapi-specification.yaml:928), optional [`expiresIn`](docs/03-openapi-specification.yaml:931), and [`user`](docs/03-openapi-specification.yaml:935).
- Persist the active JWT for browser session continuity.
- Attach `Authorization: Bearer <token>` to authenticated requests only.
- On app bootstrap, if a token exists, call [`getCurrentUser`](docs/03-openapi-specification.yaml:105) to validate and restore session state.
- On explicit logout, call [`logout`](docs/03-openapi-specification.yaml:94) then clear local session state.
- On `401`, central auth error handling should clear invalid session state and trigger route protection behavior.
- No refresh-token flow is planned because it is not defined in [`docs/03-openapi-specification.yaml`](docs/03-openapi-specification.yaml:922).

## API client and service architecture plan

- One base API client for base URL, headers, timeout, auth injection, and error normalization.
- One domain service module per backend area.
- Frontend DTOs mirror the OpenAPI schema names and shapes used by the MVP.
- Use exact backend request/response shapes for:
  - [`LoginResponse`](docs/03-openapi-specification.yaml:922)
  - [`UserResponse`](docs/03-openapi-specification.yaml:938)
  - [`ProductSummary`](docs/03-openapi-specification.yaml:988)
  - [`ProductResponse`](docs/03-openapi-specification.yaml:1009)
  - [`AddressResponse`](docs/03-openapi-specification.yaml:1073)
  - [`CartResponse`](docs/03-openapi-specification.yaml:1126)
  - [`OrderResponse`](docs/03-openapi-specification.yaml:1210)
  - [`PaymentResponse`](docs/03-openapi-specification.yaml:1268)
  - [`ErrorResponse`](docs/03-openapi-specification.yaml:1424)
  - [`PagedProductResponse`](docs/03-openapi-specification.yaml:1462)
  - [`PagedOrderResponse`](docs/03-openapi-specification.yaml:1473)

## Shared ErrorResponse handling plan

- Normalize all non-2xx responses into a shared error model derived from [`ErrorResponse`](docs/03-openapi-specification.yaml:1424).
- Use `fieldErrors` for form-level and field-level feedback.
- Use `status`, `code`, and `message` for banners, inline alerts, and retry prompts.
- Handle common cases consistently:
  - `400` validation or bad input
  - `401` invalid or missing auth
  - `404` missing resource or stale link
  - `409` business-rule conflicts such as stock changes, unavailable cart items, duplicate/invalid payment initiation, or cancellation denial

## Catalog pages plan

- Home page presents categories, brands, and a browse/search entry point.
- Product listing/search page supports:
  - `q`
  - `categoryId`
  - `brandId`
  - `minPrice`
  - `maxPrice`
  - `availableOnly`
  - `page`
  - `size`
  - `sort`
- Category page uses [`getProductsByCategory`](docs/03-openapi-specification.yaml:137).
- Brand page uses [`getProductsByBrand`](docs/03-openapi-specification.yaml:170).
- Search results page uses [`searchProducts`](docs/03-openapi-specification.yaml:187).
- Pagination must use the backend `{ content, page }` contract exactly.

## Product detail and related products plan

- Product detail page uses [`getProduct`](docs/03-openapi-specification.yaml:234).
- Show title, description, price, category, brand, availability, stock quantity, and delivery estimate from [`ProductResponse`](docs/03-openapi-specification.yaml:1009).
- Related products section uses [`getRelatedProducts`](docs/03-openapi-specification.yaml:252).
- Add-to-cart entry point lives on the product detail page and respects backend availability.

## Cart plan

- Cart page uses [`getCart`](docs/03-openapi-specification.yaml:352).
- Add item from product cards/details via [`addCartItem`](docs/03-openapi-specification.yaml:367).
- Update quantity via [`updateCartItem`](docs/03-openapi-specification.yaml:398).
- Remove item via [`removeCartItem`](docs/03-openapi-specification.yaml:430).
- Cart pricing displayed from backend response only.
- If [`recommendedProducts`](docs/03-openapi-specification.yaml:1143) is returned, render it as server-supplied recommendation data.

## Address management plan

- Dedicated addresses page for list/create/update/delete.
- One reusable form for create and edit using [`AddressRequest`](docs/03-openapi-specification.yaml:1034).
- Address selection for checkout uses stored addresses from [`listAddresses`](docs/03-openapi-specification.yaml:272).

## Checkout plan

- Checkout page requires authentication.
- Load active cart and saved addresses.
- User selects a saved address and submits [`CreateOrderRequest`](docs/03-openapi-specification.yaml:1152) using [`createOrder`](docs/03-openapi-specification.yaml:462).
- No coupon or gift-point UI is included.
- On success, navigate to payment for the created order.

## Payment plan

- Payment page is tied to a specific created order.
- Submit [`CreatePaymentRequest`](docs/03-openapi-specification.yaml:1257) using [`initiatePayment`](docs/03-openapi-specification.yaml:560).
- Restrict payment methods to enum values in [`PaymentMethod`](docs/03-openapi-specification.yaml:1528).
- Show payment status and outcome from [`PaymentResponse`](docs/03-openapi-specification.yaml:1268).
- Allow payment status reloading using [`getPayment`](docs/03-openapi-specification.yaml:589).

## Order history plan

- Orders page uses [`listOrders`](docs/03-openapi-specification.yaml:445).
- Support backend pagination and optional status filter.
- Do not add unsupported client-controlled sorting beyond what the backend already guarantees.

## Order detail plan

- Order detail page uses [`getOrder`](docs/03-openapi-specification.yaml:491).
- Display order number, status, item snapshots, monetary totals, placed time, cancellation deadline, and shipping address snapshot.
- Treat order address and item details as historical snapshots, not editable current address/product records.

## Buy Again plan

- Buy-again action is exposed from order list items and order detail.
- Invoke [`buyAgain`](docs/03-openapi-specification.yaml:510).
- On success, refresh cart state and route to cart.
- On `409`, show the backend conflict message and keep the user in context.

## Order cancellation plan

- Expose cancel action only from order detail and optionally order list summary when current order data suggests eligibility.
- Invoke [`cancelOrder`](docs/03-openapi-specification.yaml:535).
- Use backend response as final authority on cancellation success or denial.
- Show cancellation deadline from [`OrderResponse`](docs/03-openapi-specification.yaml:1252).

## Loading, empty, and error states plan

- Global shell loading for session bootstrap.
- Page-level loading for route data.
- Section-level skeletons or loaders for lists and related products.
- Empty states for:
  - no search results
  - empty cart
  - no saved addresses
  - no orders
- Error states for:
  - failed page load
  - mutation failure
  - unauthorized session
  - not found
  - business-rule conflict

## Form validation plan

Client validation should mirror backend constraints from the OpenAPI contract, including:
- registration fields from [`RegisterRequest`](docs/03-openapi-specification.yaml:889)
- login fields from [`LoginRequest`](docs/03-openapi-specification.yaml:911)
- address fields from [`AddressRequest`](docs/03-openapi-specification.yaml:1034)
- quantity fields from [`AddCartItemRequest`](docs/03-openapi-specification.yaml:1083) and [`UpdateCartItemRequest`](docs/03-openapi-specification.yaml:1096)
- checkout address selection from [`CreateOrderRequest`](docs/03-openapi-specification.yaml:1152)
- payment fields from [`CreatePaymentRequest`](docs/03-openapi-specification.yaml:1257)

Server validation remains authoritative and must always be surfaced when returned.

## Protected and public route behavior plan

- Public routes: catalog and auth pages.
- Protected routes: all customer-owned resources and checkout/payment/order flows.
- Missing or invalid token on protected route access redirects to login.
- Existing authenticated session on login/register redirects away from public auth pages.
- Unauthorized API response during an active protected session clears session state and reroutes predictably.

## State management plan

- Server state: products, categories, brands, cart, addresses, orders, payment status.
- Client session state: token, authenticated user snapshot, intended redirect target, local UI toggles.
- Mutations invalidate or refetch the affected server queries rather than manually duplicating server state.

## Backend operationId to frontend API mapping

| Backend operationId | Backend path | Frontend API module | Primary frontend consumer |
|---|---|---|---|
| `registerUser` | `/auth/register` | `authApi.registerUser` | Register page |
| `login` | `/auth/login` | `authApi.login` | Login page, session bootstrap |
| `logout` | `/auth/logout` | `authApi.logout` | Header/account actions |
| `getCurrentUser` | `/users/me` | `authApi.getCurrentUser` | App bootstrap, protected-route validation |
| `listCategories` | `/categories` | `catalogApi.listCategories` | Home page, catalog filters |
| `getProductsByCategory` | `/categories/{categoryId}/products` | `catalogApi.getProductsByCategory` | Category page |
| `listBrands` | `/brands` | `catalogApi.listBrands` | Home page, catalog filters |
| `getProductsByBrand` | `/brands/{brandId}/products` | `catalogApi.getProductsByBrand` | Brand page |
| `searchProducts` | `/products` | `catalogApi.searchProducts` | Product listing/search page |
| `getProduct` | `/products/{productId}` | `catalogApi.getProduct` | Product detail page |
| `getRelatedProducts` | `/products/{productId}/related` | `catalogApi.getRelatedProducts` | Product detail related-products section |
| `listAddresses` | `/addresses` | `addressApi.listAddresses` | Addresses page, checkout |
| `createAddress` | `/addresses` | `addressApi.createAddress` | Address create form |
| `updateAddress` | `/addresses/{addressId}` | `addressApi.updateAddress` | Address edit form |
| `deleteAddress` | `/addresses/{addressId}` | `addressApi.deleteAddress` | Addresses page |
| `getCart` | `/cart` | `cartApi.getCart` | Cart page, cart badge, checkout |
| `addCartItem` | `/cart/items` | `cartApi.addCartItem` | Product cards, product detail |
| `updateCartItem` | `/cart/items/{itemId}` | `cartApi.updateCartItem` | Cart page |
| `removeCartItem` | `/cart/items/{itemId}` | `cartApi.removeCartItem` | Cart page |
| `listOrders` | `/orders` | `ordersApi.listOrders` | Orders page |
| `createOrder` | `/orders` | `ordersApi.createOrder` | Checkout page |
| `getOrder` | `/orders/{orderId}` | `ordersApi.getOrder` | Order detail page |
| `buyAgain` | `/orders/{orderId}/buy-again` | `ordersApi.buyAgain` | Orders page, order detail page |
| `cancelOrder` | `/orders/{orderId}/cancel` | `ordersApi.cancelOrder` | Order detail page, order list summary |
| `initiatePayment` | `/payments` | `paymentsApi.initiatePayment` | Payment page |
| `getPayment` | `/payments/{paymentId}` | `paymentsApi.getPayment` | Payment status view |

## Environment configuration plan

Frontend environment variables should be limited to frontend runtime configuration, for example:
- `VITE_API_BASE_URL=http://localhost:8080/api`
- optional frontend-only feature flags for non-business UI behavior if needed later

Do not duplicate backend secrets in the frontend.

## Local development setup plan

- Run the Spring Boot backend locally on the OpenAPI server URL [`http://localhost:8080/api`](docs/03-openapi-specification.yaml:15).
- Run the Vite frontend dev server separately from the backend process.
- Configure local frontend-to-backend communication using the API base URL and, if desired, a local dev proxy for browser convenience.
- Keep frontend startup instructions separate from Maven commands.

## Frontend testing strategy

Unit and component testing should cover:
- auth forms and route guards
- catalog filter/query behavior
- cart mutation components
- address form validation
- checkout submission behavior
- payment method selection
- order list/detail rendering
- buy-again and cancellation action states
- shared error/loading/empty state components

## Integration and API testing strategy

Browser-level and integration coverage should verify:
- guest browsing public catalog routes
- login and authenticated route entry
- add to cart and cart quantity updates
- address CRUD flows
- checkout from cart to created order
- payment initiation and payment status display
- order history navigation to order detail
- buy again updates cart
- cancellation success and cancellation denial handling
- consistent handling of backend `400`, `401`, `404`, and `409` responses

These tests should run against the real backend API or a contract-faithful test environment, without inventing unsupported backend behavior.

## Definition of done

The frontend MVP is complete when:
- the React + TypeScript + Vite app is created and builds successfully
- all planned MVP routes are implemented
- all required operationIds listed in this plan are integrated
- catalog public access works without authentication
- protected routes enforce authenticated access
- auth session bootstrap/logout behavior works correctly
- cart, addresses, checkout, payment, orders, buy again, and cancellation flows work against the backend API
- loading, empty, validation, and error states are implemented consistently
- no Phase 2 features are exposed in the UI
- unit/component and browser-level tests cover the critical MVP flows
- local developer setup is documented

## Git milestone strategy

Recommended incremental milestones:
1. `feat(frontend): scaffold React TypeScript Vite application`
2. `feat(frontend): add app shell routing auth foundation and api client`
3. `feat(frontend): implement public catalog browsing and product detail`
4. `feat(frontend): implement cart workflows`
5. `feat(frontend): implement address management and checkout`
6. `feat(frontend): implement payment flow`
7. `feat(frontend): implement order history detail buy again and cancellation`
8. `test(frontend): add component and browser integration coverage`
9. `docs(frontend): document local development and environment setup`
