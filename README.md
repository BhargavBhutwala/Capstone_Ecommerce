# E-Bookstore — IBM AI Specialist Capstone

A full-stack online bookstore built as the IBM AI Specialist Capstone project.
The backend is a Spring Boot 3 modular monolith backed by PostgreSQL; the frontend is a React 18 + TypeScript single-page application.
The project was designed and implemented using a design-first workflow: requirements → data model → OpenAPI contract → implementation.

**Status: Full-stack MVP complete and validated.**

---

## Table of Contents

1. [MVP Features](#mvp-features)
2. [Tech Stack](#tech-stack)
3. [Architecture and Project Structure](#architecture-and-project-structure)
4. [Prerequisites](#prerequisites)
5. [Local Database Setup](#local-database-setup)
6. [Backend Startup](#backend-startup)
7. [Frontend Startup](#frontend-startup)
8. [API Documentation](#api-documentation)
9. [Testing](#testing)
10. [Test Results](#test-results)
11. [MVP Scope Boundary](#mvp-scope-boundary)
12. [Important Implementation Notes](#important-implementation-notes)
13. [Repository Documentation](#repository-documentation)

---

## MVP Features

### Authentication
- User registration with BCrypt-hashed passwords
- Login returning a stateless JWT Bearer token
- Logout (client-side token discard)

### Product Catalog
- Browse products with pagination and server-side sorting
- Full-text keyword search
- Filter by category, brand, price range, and availability
- Category listing
- Brand listing
- Product detail page with related-product suggestions (same category/brand)

### Shopping Cart
- Add products to cart; duplicate products increment quantity rather than create a new row
- Update item quantity
- Remove items
- View cart summary with server-authoritative pricing

### Addresses
- Add, update, and delete delivery addresses
- Set a default address
- Addresses are available for selection at checkout

### Checkout and Orders
- Full transactional checkout: stock validation → address snapshot → price snapshot → order creation → stock decrement
- Payment is initiated separately after order creation through the simulated payment endpoint
- Order total calculated server-side; client-supplied totals are never trusted
- Shipping address copied as seven flat snapshot columns onto the order (never a live FK)
- Product title and unit price snapshotted onto each order item at purchase time

### Simulated Payment
- Simulated payment flow: payment is initiated and completed immediately as `SUCCESS`
- No real card processing; no raw payment credentials stored
- Duplicate successful payment prevention per order

### Order History and Buy Again
- List all orders for the authenticated customer, newest first
- Order detail including historical snapshot prices and address
- Buy Again: re-adds historical order items to the active cart after validating current stock, availability, and pricing

### Order Cancellation
- Cancellation allowed while `current time ≤ cancellation_deadline` (`placed_at + 48 h`)
- Deadline persisted on the order at creation; enforced server-side in the service layer

---

## Tech Stack

### Backend

| Component | Technology |
|-----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.1 |
| Build tool | Maven |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL |
| Schema migrations | Flyway |
| Security | Spring Security + Stateless JWT Bearer (jjwt 0.12.6) |
| Validation | Jakarta Bean Validation |
| API docs | Springdoc OpenAPI 2.7.0 / Swagger UI |
| Testing | JUnit 5, Mockito, Spring Boot Test, Testcontainers 1.21.4 |

### Frontend

| Component | Technology |
|-----------|-----------|
| Language | TypeScript 5.6 |
| Framework | React 18.3 |
| Bundler | Vite 6 |
| Routing | React Router v7 |
| HTTP | Native `fetch` API |
| Unit/component tests | Vitest 4 + React Testing Library 16 |
| E2E/integration tests | Playwright 1.62 |
| Linting | ESLint 10 + typescript-eslint |

---

## Architecture and Project Structure

The backend follows a **domain-oriented modular monolith**. Each domain owns its controller, service, repository, entity, and DTO layers. Controllers handle HTTP concerns only; business rules live exclusively in service classes.

```
Capstone Project/
├── docs/
│   ├── 01-requirements-specification.md
│   ├── 02-data-model-design.md
│   └── 03-openapi-specification.yaml
├── src/
│   ├── main/
│   │   ├── java/com/ebookstore/
│   │   │   ├── auth/            # Registration, login, JWT issuance
│   │   │   ├── user/            # User profile
│   │   │   ├── catalog/         # Products, categories, brands, search
│   │   │   ├── cart/            # Cart and cart-item management
│   │   │   ├── address/         # Delivery addresses
│   │   │   ├── order/           # Checkout, order lifecycle, Buy Again
│   │   │   ├── payment/         # Simulated payment processing
│   │   │   ├── security/        # JWT filter, UserDetailsService
│   │   │   ├── config/          # Security config, Jackson, OpenAPI beans
│   │   │   └── common/          # Error response, exception hierarchy, pagination
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       └── db/migration/    # Flyway SQL migrations (V1–V11)
│   └── test/
│       └── java/com/ebookstore/ # Unit tests (*Test.java) and IT tests (*IT.java)
├── frontend/
│   ├── src/
│   │   ├── api/                 # fetch-based API client modules
│   │   ├── app/                 # App shell, providers, layout
│   │   ├── features/
│   │   │   ├── auth/
│   │   │   ├── catalog/
│   │   │   ├── cart/
│   │   │   ├── address/
│   │   │   ├── checkout/
│   │   │   ├── payment/
│   │   │   └── orders/
│   │   ├── components/          # Shared UI primitives and form components
│   │   ├── hooks/               # Shared custom React hooks
│   │   ├── routes/              # Route definitions and guards
│   │   ├── types/               # OpenAPI-aligned TypeScript DTOs
│   │   └── utils/
│   ├── vite.config.ts           # Dev proxy: /api → http://localhost:8080
│   └── package.json
├── AGENTS.md                    # Project coding rules and AI workflow guidelines
└── pom.xml
```

### Database schema (Flyway migrations)

| Migration | Table |
|-----------|-------|
| V1 | `users` |
| V2 | `addresses` |
| V3 | `categories` |
| V4 | `brands` |
| V5 | `products` |
| V6 | `carts` |
| V7 | `cart_items` |
| V8 | `orders` |
| V9 | `order_items` |
| V10 | `payments` |
| V11 | indexes |

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21 |
| Maven | 3.9+ |
| PostgreSQL | 15+ |
| Node.js | 20 LTS |
| npm | 10+ (bundled with Node 20) |
| Docker Desktop | Required by Testcontainers (backend tests only) |

---

## Local Database Setup

The `local` Spring profile (`application-local.yml`) provides defaults that work with a standard local PostgreSQL installation:

| Setting | Default value |
|---------|---------------|
| JDBC URL | `jdbc:postgresql://localhost:5432/ebookstore` |
| Username | `postgres` |
| Password | `postgres` |
| JWT secret | development fallback (do not use in production) |
| JWT expiry | 86400000 ms (24 h) |

These defaults can be overridden by setting the environment variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and `JWT_EXPIRATION_MS` before starting the application.

Create the database before first startup:

```sql
CREATE DATABASE ebookstore;
```

Flyway runs all migrations automatically on startup; no manual schema setup is required.

> The `local` Spring profile must be active. Without it, the application expects `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` to be supplied as environment variables and will fail to start if they are absent.

---

## Backend Startup

Run from the project root using Windows PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
mvn spring-boot:run
```

The backend starts on:

```
http://localhost:8080/api
```

Health check endpoint:

```
http://localhost:8080/api/actuator/health
```

---

## Frontend Startup

Run from the project root:

```powershell
cd frontend
npm install
npm run dev
```

The frontend is available at:

```
http://localhost:5173
```

The Vite dev server proxies all `/api` requests to `http://localhost:8080`, so no CORS configuration changes are needed during local development.

> The frontend is independent of the Maven build. Running `mvn` from the project root does not install Node dependencies or build the frontend.

---

## API Documentation

Swagger UI is available when the backend is running:

```
http://localhost:8080/api/swagger-ui.html
```

OpenAPI JSON:

```
http://localhost:8080/api/v3/api-docs
```

The authoritative API contract is the hand-authored specification at [`docs/03-openapi-specification.yaml`](docs/03-openapi-specification.yaml).

---

## Testing

### Backend

Runs both unit tests (`*Test.java`) and Testcontainers-based integration tests (`*IT.java`). Docker Desktop must be running for the integration tests.

```powershell
mvn clean test
```

### Frontend — unit and component tests

```powershell
cd frontend
npm run lint
npm run build
npm run test:run
```

### Frontend — Playwright integration tests

Playwright tests exercise the real application end-to-end. Both the Spring Boot backend and the Vite dev server must be running before executing:

```powershell
cd frontend
npm run integration
```

---

## Test Results

The following results reflect the validated MVP state.

### Backend — Maven Surefire

| Result | Count |
|--------|-------|
| Tests passed | 268 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |

### Frontend — Vitest (unit/component)

| Result | Count |
|--------|-------|
| Tests passed | 89 / 89 |
| Test files | 10 |

### Frontend — Playwright

| Result | Count |
|--------|-------|
| Total | 14 |
| Passed | 12 |
| Skipped | 2 |
| Failed | 0 |

### Frontend — static checks

| Check | Status |
|-------|--------|
| ESLint | Clean |
| TypeScript / Vite build | Successful |

---

## MVP Scope Boundary

The completed MVP intentionally excludes the following Phase-2 features. None of the items below have been implemented:

- **Gift points** — earning, redemption, account management, and transaction history
- **Coupons** — coupon codes, discount calculation, and stacking rules
- **Shipments** — shipment lifecycle, tracking, and status management
- **Returns** — return request creation, approval, and processing
- **Refunds** — refund calculation and processing

These domains have no controllers, services, repositories, entities, or Flyway migrations in the current codebase. They appear in the OpenAPI specification as future documentation only.

---

## Important Implementation Notes

- **Schema management:** Flyway owns all schema creation and evolution. Hibernate is configured with `ddl-auto: validate` and will not modify the schema.
- **JWT:** Authentication is fully stateless. No server-side session storage is used.
- **Monetary values:** All prices and amounts use `BigDecimal` in Java and `NUMERIC(12,2)` in PostgreSQL.
- **Payments:** The payment processor is simulated. No real card gateway is integrated. Raw card credentials are never stored.
- **Server authority:** The backend is authoritative for product prices, order totals, stock levels, checkout processing, payment processing, and cancellation eligibility. Client-supplied prices and totals are ignored.
- **Order snapshots:** Order item `productTitle` and `unitPrice` are copied from the product at checkout time. The shipping address is snapshotted as seven flat columns on the `orders` row. Historical orders never depend on the current state of a product or address record.
- **Cancellation deadline:** `cancellationDeadline = placedAt + 48 h` is persisted on the order at creation and checked server-side at cancellation time.
- **Cart uniqueness:** `UNIQUE(cart_id, product_id)` is enforced by a database constraint. The service layer merges quantities when the same product is added to an existing cart item.

---

## Repository Documentation

| Document | Purpose |
|----------|---------|
| [`AGENTS.md`](AGENTS.md) | Project coding rules, architecture decisions, and AI workflow guidelines |
| [`docs/01-requirements-specification.md`](docs/01-requirements-specification.md) | Functional and technical requirements baseline |
| [`docs/02-data-model-design.md`](docs/02-data-model-design.md) | Entity design, relationships, and column definitions |
| [`docs/03-openapi-specification.yaml`](docs/03-openapi-specification.yaml) | Authoritative REST API contract |
| [`frontend/README.md`](frontend/README.md) | Frontend-specific setup and script reference |
