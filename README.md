# E-Bookstore — IBM AI Specialist Capstone

A full-stack e-bookstore platform built as the IBM AI Specialist Capstone project. The backend is a Spring Boot modular monolith exposing a documented REST API; the frontend is a React + TypeScript single-page application. Both layers are fully implemented, tested, and validated.

---

## 🎥 Project Demo Video

A complete walkthrough of the E-Bookstore capstone project is available in the GitHub Release:

[▶ Watch / Download E-Bookstore Capstone Demo](https://github.com/BhargavBhutwala/Capstone_Ecommerce/releases/tag/v1.0.0-mvp)

---

## MVP Features

| Domain | Capability |
|--------|-----------|
| Authentication | Register, login, logout; stateless JWT Bearer tokens |
| Catalog | Paginated product listing; search by keyword; filter by category, brand, price range; product detail |
| Related products | Same-category/brand recommendations on product detail and cart pages |
| Cart | Add, update quantity, remove items; server-authoritative subtotal and total |
| Addresses | Create, list, update, delete saved addresses; default address support |
| Checkout | Select shipping address; server-side stock validation, price snapshot, and order creation |
| Simulated payment | Initiate payment (CREDIT\_CARD or DEBIT\_CARD); simulated INITIATED → PROCESSING → SUCCESS/FAILED flow |
| Order history | Paginated order list with status filter; order detail showing immutable item and address snapshots |
| Buy Again | Re-adds all items from a historical order to the active cart with current stock/price validation |
| Order cancellation | Cancel button visible when order is in a cancellable status; backend enforces 48-hour deadline |
| User profile | Authenticated profile page showing account details and saved addresses |

---

## Tech Stack

### Backend

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.1 |
| Build | Maven |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL |
| Schema management | Flyway |
| Security | Spring Security, stateless JWT Bearer (jjwt 0.12.6) |
| API documentation | Springdoc OpenAPI 2.7.0 |
| Testing | JUnit 5, Mockito, Testcontainers 1.21.4 |

### Frontend

| Layer | Technology |
|-------|-----------|
| Language | TypeScript 5.6 |
| Framework | React 18 |
| Build tool | Vite 6 |
| Routing | React Router 7 |
| HTTP client | Native `fetch` API (no third-party client library) |
| Unit / component tests | Vitest 4, React Testing Library 16 |
| Integration tests | Playwright 1.62 |

---

## Architecture

```
com.ebookstore
├── auth          # Registration, login, JWT issuance
├── user          # User entity and profile
├── catalog       # Products, categories, brands
├── cart          # Cart and cart items
├── address       # Saved shipping addresses
├── order         # Order creation, history, cancellation, Buy Again
├── payment       # Simulated payment processing
├── security      # JWT filter, security configuration
├── config        # CORS, Jackson, application-level beans
└── common        # Shared DTOs, exceptions, error handling
```

Each domain follows the standard layering:

```
Controller → Service → Repository → JPA Entity → PostgreSQL
```

Controllers handle HTTP and validation only. Business rules live exclusively in service classes.

### Frontend structure

```
frontend/src
├── api           # Typed fetch wrappers per domain
├── app           # AppLayout, Header, root providers
├── components    # Shared UI components (ProductCard, Pagination, states)
├── features      # Domain pages (auth, catalog, cart, checkout, orders, payment, address, profile)
├── hooks         # useAsync, useAddToCart
├── routes        # React Router configuration, ProtectedRoute, PublicOnlyRoute
├── types         # TypeScript API types matching OpenAPI schemas
├── utils         # formatCurrency (INR), formatDateTime (IST)
└── test          # Vitest unit/component tests; Playwright e2e tests
```

---

## Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL 15+ running locally
- Node.js 20+
- npm 10+
- Docker Desktop (required by Testcontainers for backend integration tests)

---

## Local Database Setup

The `local` Spring profile uses the following defaults (override via environment variables):

| Variable | Default |
|----------|---------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/ebookstore` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `postgres` |

Create the database before first run:

```sql
CREATE DATABASE ebookstore;
```

Flyway applies all migrations automatically on startup. Hibernate is set to `ddl-auto: validate` — it never modifies the schema.

To load the demo catalogue (70 books across 8 categories), run the seed script once after migrations have been applied:

```powershell
psql -U postgres -d ebookstore -f scripts/seed-demo-data.sql
```

---

## Running the Backend

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
mvn spring-boot:run
```

The backend starts at:

```
http://localhost:8080/api
```

To override credentials without editing files:

```powershell
$env:DB_URL      = "jdbc:postgresql://localhost:5432/ebookstore"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "your_password"
$env:JWT_SECRET  = "your-256-bit-or-longer-secret"
$env:SPRING_PROFILES_ACTIVE = "local"
mvn spring-boot:run
```

---

## Running the Frontend

```powershell
cd frontend
npm install
npm run dev
```

The frontend starts at:

```
http://localhost:5173
```

Vite proxies all `/api` requests to `http://localhost:8080`, so the Spring Boot backend must be running for the application to function.

---

## API Documentation

Swagger UI is available at:

```
http://localhost:8080/api/swagger-ui.html
```

OpenAPI JSON is available at:

```
http://localhost:8080/api/v3/api-docs
```

The authoritative API contract is [`docs/03-openapi-specification.yaml`](docs/03-openapi-specification.yaml).

---

## Testing

### Backend

```powershell
mvn clean test
```

Backend tests use Testcontainers to spin up a real PostgreSQL instance. Docker Desktop must be running.

### Frontend unit and component tests

```powershell
cd frontend
npm run test:run
```

### Frontend lint and build check

```powershell
cd frontend
npm run lint
npm run build
```

### Playwright integration tests

```powershell
cd frontend
npm run integration
```

Playwright tests require both the Spring Boot backend and the Vite dev server to be running simultaneously before the test run begins.

---

## Verified Test Results

### Backend

| Result | Count |
|--------|-------|
| Tests passed | 268 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |

### Frontend unit / component (Vitest)

| Result | Count |
|--------|-------|
| Test files | 13 |
| Tests passed | 115 |
| Failures | 0 |

### Frontend Playwright integration

| Result | Count |
|--------|-------|
| Total | 14 |
| Passed | 12 |
| Skipped | 2 |
| Failed | 0 |

### Frontend static checks

- Lint: clean (0 errors, 0 warnings)
- TypeScript / Vite build: successful

---

## MVP Scope Boundary

The following are intentionally excluded from the current MVP and must not be treated as implemented:

- **Gift points** — no gift point earning, redemption, or transaction history
- **Coupons** — no coupon codes or discount application
- **Shipments** — no shipment lifecycle tracking beyond the order status field
- **Returns** — no return request workflow
- **Refunds** — no refund processing

These are documented Phase-2 features. The database schema does not contain the tables for any of the above.

---

## Important Implementation Notes

- **Schema management**: Flyway owns the schema. Hibernate `ddl-auto` is set to `validate` only — it will reject a mismatched schema rather than silently modify it.
- **JWT**: Stateless Bearer tokens; no server-side session state.
- **Payments**: Fully simulated. No real payment gateway is used. No card numbers, CVV, or banking data are collected or stored.
- **Price authority**: The backend is the sole authority for prices, order totals, stock levels, and all monetary calculations. Client-supplied prices are never trusted.
- **Order snapshots**: `order_items.product_title` and `order_items.unit_price` are copied at checkout and never updated. Historical orders always display the price and title at the time of purchase.
- **Shipping address snapshot**: The full 7-field shipping address is stored as flat columns on the `orders` table. Order history is independent of any subsequent changes to the user's saved addresses.
- **Cancellation deadline**: `orders.cancellation_deadline` is set to `placed_at + 48 hours` at order creation. The service layer checks this field — the deadline is never recomputed on the fly.
- **Cart uniqueness**: `UNIQUE(cart_id, product_id)` is enforced at the database level. Adding an existing product to the cart increments the quantity rather than inserting a duplicate row.
- **Monetary types**: `BigDecimal` in Java, `NUMERIC(12,2)` in PostgreSQL throughout. `double` and `float` are not used for any monetary value.

---

## Repository Documentation

| Document | Description |
|----------|-------------|
| [`docs/01-requirements-specification.md`](docs/01-requirements-specification.md) | Functional and non-functional requirements |
| [`docs/02-data-model-design.md`](docs/02-data-model-design.md) | Entity relationship model and table definitions |
| [`docs/03-openapi-specification.yaml`](docs/03-openapi-specification.yaml) | Authoritative REST API contract (OpenAPI 3.0.3) |
| [`AGENTS.md`](AGENTS.md) | AI development workflow rules and project coding standards |
| [`scripts/seed-demo-data.sql`](scripts/seed-demo-data.sql) | Optional demo catalogue seed (70 books, 8 categories, 40 publishers) |

---

## Project Status

The full-stack MVP is complete and validated. All backend and frontend tests pass. The application covers the complete customer journey from registration through catalog browsing, cart management, address management, checkout, payment, and order history.
