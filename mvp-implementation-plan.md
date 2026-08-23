# MVP Spring Boot Backend — Implementation Plan

## Overview

Build the E-Bookstore MVP backend as a Spring Boot 3.x modular monolith backed by
PostgreSQL. The implementation contract is defined by three authoritative documents:

- `docs/01-requirements-specification.md` — what the system must do
- `docs/02-data-model-design.md` — how data is persisted
- `docs/03-openapi-specification.yaml` — the REST API contract

The implementation must not redefine, contradict, or extend beyond these documents.
Phase-2 features (gift points, coupons, shipments, returns, refunds) must not be
implemented during MVP.

---

## Technology Decisions

| Decision | Resolution |
|---|---|
| Java | 21 |
| Framework | Spring Boot 3.x |
| Build | Maven |
| Database | PostgreSQL |
| Persistence | Spring Data JPA (Hibernate underneath) |
| Schema management | Flyway (not Hibernate DDL) |
| Authentication | Stateless JWT Bearer |
| Password hashing | BCrypt via Spring Security |
| Roles | `CUSTOMER`, `ADMIN` |
| Currency (Java) | `BigDecimal` — never `double` or `float` |
| Currency (DB) | `NUMERIC(12,2)` |
| Payment | Simulated — no real gateway |
| API documentation | Springdoc OpenAPI |
| Test framework | JUnit 5 + Mockito |
| OpenAPI source-of-truth | `docs/03-openapi-specification.yaml` |
| Order history sort | `placed_at DESC` (fixed, no client sort param) |

---

## MVP Scope

**Implement:**
- Authentication (register, login, logout)
- User profile (current user)
- Public catalog (categories, brands, products with search/filter)
- Related products
- Cart management
- Addresses
- Checkout and order creation
- Order history and Buy Again
- Simulated payment
- Purchase confirmation
- 48-hour order cancellation
- Centralized validation, authorization, and error handling
- Automated unit and integration tests

**Do not implement (Phase 2):**
- Gift points (`gift_point_accounts`, `gift_point_transactions` tables)
- Coupons (`coupons` table)
- Shipments (`shipments` table)
- Returns (`return_requests` table)
- Refunds (`refunds` table)

**Phase-2 implementation boundary — strictly enforced:**
Do not create any controller, service, repository, entity, migration, test, or enum for
gift points, coupons, shipments, returns, or refunds during MVP.

Do not create Phase-2 packages:
- `com.ebookstore.loyalty`
- `com.ebookstore.coupon`
- `com.ebookstore.shipping`
- `com.ebookstore.returns`
- `com.ebookstore.refund`

The following OpenAPI paths may exist for documentation only — they must not trigger
any implementation:
- `GET/POST /gift-points`, `GET /gift-points/transactions`
- `GET /coupons/{code}`
- `GET /shipments/{shipmentId}`
- `POST /returns`, `GET/PUT /returns/{returnId}`

---

## Module Implementation Order

```
Task 1  → Project scaffold and configuration
Task 2  → Flyway database migrations
Task 3  → JPA entities and MVP enums
Task 4  → Spring Data repositories
Task 5  → Common exception/response infrastructure
Task 6  → JWT security infrastructure
Task 7  → Authentication and user profile
Task 8  → Catalog
Task 9  → Addresses
Task 10 → Cart
Task 11 → Orders and checkout
Task 12 → Payment
Task 13 → Springdoc OpenAPI configuration
Task 14 → Integration tests
```

---

## Task 1 — Project Scaffold and Configuration

**Intent:** Create the Maven project structure, all configuration files, and dependency
declarations that every subsequent module will depend on. Nothing runs after this task
except a bare Spring Boot startup with a database connection.

**Expected Outcomes:**
- `mvn spring-boot:run` starts without errors (database present)
- `mvn test` runs and passes zero tests (no tests yet)
- Flyway connects and reports no migrations pending (no scripts yet)
- Actuator health endpoint responds `UP`
- Package root `com.ebookstore` exists

**Todo List:**

1. Create `pom.xml` at the project root with the dependency list below.
2. Create the main application class `EbookstoreApplication.java` in
   `src/main/java/com/ebookstore/`.
3. Create `src/main/resources/application.yml` with database, JPA, Flyway, and
   server configuration.
4. Create `src/main/resources/application-local.yml` for local PostgreSQL overrides.
5. Create `src/test/resources/application-test.yml` for the test database connection.
6. Create the package skeleton directories (no Java files yet, just `package-info.java`
   or empty marker) for:
   - `com.ebookstore.auth`
   - `com.ebookstore.user`
   - `com.ebookstore.catalog`
   - `com.ebookstore.cart`
   - `com.ebookstore.address`
   - `com.ebookstore.order`
   - `com.ebookstore.payment`
   - `com.ebookstore.security`
   - `com.ebookstore.config`
   - `com.ebookstore.common`
7. Create `src/main/resources/db/migration/` directory (Flyway script home).
8. Verify `mvn compile` succeeds.

**Dependencies (pom.xml):**

| Artifact | Scope | Justification |
|---|---|---|
| `spring-boot-starter-web` | compile | Spring MVC REST controllers |
| `spring-boot-starter-data-jpa` | compile | Spring Data JPA repositories |
| `spring-boot-starter-security` | compile | Spring Security filter chain |
| `spring-boot-starter-validation` | compile | Bean Validation (Jakarta) |
| `spring-boot-starter-actuator` | compile | Health endpoint (`/actuator/health`) only |
| `flyway-core` | compile | Database migrations |
| `flyway-database-postgresql` | compile | PostgreSQL Flyway dialect |
| `postgresql` | runtime | PostgreSQL JDBC driver |
| `springdoc-openapi-starter-webmvc-ui` | compile | Swagger UI + OpenAPI docs |
| `jjwt-api` | compile | JWT API (io.jsonwebtoken 0.12.x) |
| `jjwt-impl` | runtime | JWT implementation |
| `jjwt-jackson` | runtime | JWT JSON serialization |
| `spring-boot-starter-test` | test | JUnit 5, Mockito, MockMvc |
| `spring-security-test` | test | `@WithMockUser`, security test support |
| `org.testcontainers:testcontainers` | test | Testcontainers core — container lifecycle |
| `org.testcontainers:postgresql` | test | PostgreSQL Testcontainer for integration tests |
| `org.testcontainers:junit-jupiter` | test | `@Testcontainers` / `@Container` JUnit 5 support |

**Configuration (`application.yml`):**

```yaml
server:
  port: 8080
  servlet:
    context-path: /api            # matches OpenAPI servers[0].url

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate           # Flyway owns schema; Hibernate only validates
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false

app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-ms: ${JWT_EXPIRATION_MS:86400000}  # 24 h default

management:
  endpoints:
    web:
      exposure:
        include: health             # expose only /actuator/health
  endpoint:
    health:
      show-details: never

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

**Actuator constraint:** Only `GET /actuator/health` is exposed. Do not expose
`/actuator/info`, `/actuator/metrics`, or any other management endpoint. Actuator
is a health-check tool, not part of the business API.

**Relevant Context:**
- `AGENTS.md` §Database Rules: `ddl-auto: validate` is mandatory
- `AGENTS.md` §Secrets: all sensitive values via environment variables
- OpenAPI `servers[0].url`: `http://localhost:8080/api`

**Status:** [ ] pending

---

## Task 2 — Flyway Database Migrations

**Intent:** Create all 11 MVP Flyway migration scripts that produce the exact schema
defined in `docs/02-data-model-design.md`. Flyway owns the schema — Hibernate only
validates against it (`spring.jpa.hibernate.ddl-auto=validate`). Hibernate must not
create, alter, or drop any table.

**Expected Outcomes:**
- `mvn flyway:migrate` applies all 11 scripts in order with no errors
- `mvn spring-boot:run` with `ddl-auto: validate` starts without Hibernate validation
  errors
- All tables, columns, types, constraints, and indexes match the data model exactly
- Phase-2 tables (`shipments`, `gift_point_accounts`, `gift_point_transactions`,
  `coupons`, `return_requests`, `refunds`) are absent from the schema entirely

**Todo List:**

1. Create `V1__create_users.sql` — `users` table:
   - `id BIGSERIAL PRIMARY KEY`
   - All columns from §7.1 with `snake_case` names
   - `UNIQUE(email)`
2. Create `V2__create_addresses.sql` — `addresses` table:
   - `id BIGSERIAL PRIMARY KEY`
   - FK to `users(id)`
3. Create `V3__create_categories.sql` — `categories` table:
   - `id BIGSERIAL PRIMARY KEY`
   - `UNIQUE(name)`
4. Create `V4__create_brands.sql` — `brands` table:
   - `id BIGSERIAL PRIMARY KEY`
   - `UNIQUE(name)`
5. Create `V5__create_products.sql` — `products` table:
   - `id BIGSERIAL PRIMARY KEY`
   - FK to `categories(id)`, FK to `brands(id)`
   - `price NUMERIC(12,2) NOT NULL`
   - `CHECK (price >= 0)`, `CHECK (stock_quantity >= 0)`,
     `CHECK (delivery_days_min >= 0)`,
     `CHECK (delivery_days_max >= delivery_days_min)`
6. Create `V6__create_carts.sql` — `carts` table:
   - `id BIGSERIAL PRIMARY KEY`
   - `UNIQUE(user_id)` — enforces 1:1 with users
7. Create `V7__create_cart_items.sql` — `cart_items` table:
   - `id BIGSERIAL PRIMARY KEY`
   - FK to `carts(id)`, FK to `products(id)`
   - `unit_price NUMERIC(12,2) NOT NULL`
   - `UNIQUE(cart_id, product_id)`
   - `CHECK (quantity > 0)`
8. Create `V8__create_orders.sql` — `orders` table:
   - `id BIGSERIAL PRIMARY KEY`
   - All 20 columns from §7.8 with `snake_case` names
   - 7 shipping-snapshot columns: `shipping_name`, `shipping_line1`, `shipping_line2`,
     `shipping_city`, `shipping_state`, `shipping_postal_code`, `shipping_country`
   - Monetary columns: `subtotal`, `shipping_amount`, `discount_amount`,
     `total_amount` — all `NUMERIC(12,2) NOT NULL`
   - `gift_points_used INTEGER NOT NULL DEFAULT 0`
   - `discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0`
   - `UNIQUE(order_number)`
   - `CHECK (subtotal >= 0)`, `CHECK (total_amount >= 0)`
9. Create `V9__create_order_items.sql` — `order_items` table:
   - `id BIGSERIAL PRIMARY KEY`
   - FK to `orders(id)`, FK to `products(id)`
   - `unit_price NUMERIC(12,2) NOT NULL`, `subtotal NUMERIC(12,2) NOT NULL`
10. Create `V10__create_payments.sql` — `payments` table:
    - `id BIGSERIAL PRIMARY KEY`
    - `amount NUMERIC(12,2) NOT NULL`
    - `UNIQUE(order_id)` — enforces 1:1 with orders
    - `UNIQUE(payment_reference)`
11. Create `V11__create_indexes.sql` — additional query-path indexes only; do NOT
    duplicate indexes that are already created implicitly by UNIQUE constraints:
    - `products(title)` — product title search
    - `products(category_id)` — category browsing
    - `products(brand_id)` — brand browsing
    - `orders(user_id)` — order history lookup
    - `orders(status)` — order status filtering
    - `cart_items(cart_id)` — cart item retrieval
    - `order_items(order_id)` — order detail retrieval
    - Do NOT add indexes for: `users.email`, `products.isbn`, `orders.order_number`,
      `payments.payment_reference`, `carts.user_id`, `payments.order_id`,
      `cart_items(cart_id, product_id)` — these are already covered by their respective
      UNIQUE constraints and do not need a separate index entry
12. Verify `mvn spring-boot:run` passes Hibernate schema validation with no errors.

**Migration File List (exactly 11 — no Phase-2 tables):**
`V1__create_users.sql`, `V2__create_addresses.sql`, `V3__create_categories.sql`,
`V4__create_brands.sql`, `V5__create_products.sql`, `V6__create_carts.sql`,
`V7__create_cart_items.sql`, `V8__create_orders.sql`, `V9__create_order_items.sql`,
`V10__create_payments.sql`, `V11__create_indexes.sql`

**Important Constraints:**
- All primary keys: `BIGSERIAL` — not `SERIAL` or `INTEGER`
- All column names: `snake_case` exactly as defined in `docs/02-data-model-design.md`
- All monetary columns: `NUMERIC(12,2)` — never `DECIMAL` without precision, `FLOAT`,
  or `DOUBLE PRECISION`
- `orders.gift_points_used`: `INTEGER NOT NULL DEFAULT 0` — column present for Phase-2
  compatibility; default enforces zero in MVP
- `orders.discount_amount`: `NUMERIC(12,2) NOT NULL DEFAULT 0`
- `payments.order_id`: UNIQUE constraint enforces 1:1 with orders
- `cart_items`: `UNIQUE(cart_id, product_id)` required
- No Phase-2 tables in any migration script
- `ddl-auto: validate` — Hibernate must only validate, never alter the schema

**Relevant Context:**
- `docs/02-data-model-design.md` §7.1–§7.10 — complete column definitions
- `docs/02-data-model-design.md` §13 — constraints
- `docs/02-data-model-design.md` §14 — indexes

**Status:** [ ] pending

---

## Task 3 — JPA Entities and MVP Enums

**Intent:** Create all 10 JPA entities and exactly 6 MVP Java enums. Entities map
exactly to the Flyway schema. Entities are never exposed through REST APIs.

**Expected Outcomes:**
- All 10 entities compile without errors
- `mvn spring-boot:run` with `ddl-auto: validate` passes (Hibernate validates entities
  against the Flyway-created schema)
- No entity is serializable to JSON by default (no `@JsonIgnore` required — DTOs will
  handle serialization)

**Todo List:**

1. Create exactly **6 MVP enums** in `com.ebookstore.common.domain` — do NOT create
   `ShipmentStatus`, `GiftPointTransactionType`, or `ReturnStatus` (Phase 2):
   - `UserRole`: `CUSTOMER`, `ADMIN`
   - `UserStatus`: `ACTIVE`, `INACTIVE`, `LOCKED`
   - `CartStatus`: `ACTIVE`, `CHECKED_OUT`, `ABANDONED`
   - `OrderStatus`: `PENDING_PAYMENT`, `PAID`, `CONFIRMED`, `SHIPPED`, `DELIVERED`,
     `CANCELLED`, `RETURN_REQUESTED`, `RETURNED`, `REFUNDED`
     (all 9 values kept exactly; `CONFIRMED` remains in the enum but is not the
     successful-payment transition state in MVP — that is `PAID`)
   - `PaymentMethod`: `CREDIT_CARD`, `DEBIT_CARD`
   - `PaymentStatus`: `INITIATED`, `PROCESSING`, `SUCCESS`, `FAILED`, `REFUNDED`

2. Create `User` entity in `com.ebookstore.user.entity`:
   - All columns from §7.1 mapped exactly
   - `role` mapped as `@Enumerated(EnumType.STRING)` using `UserRole`
   - `status` mapped as `@Enumerated(EnumType.STRING)` using `UserStatus`
   - `@OneToOne(mappedBy = "user")` to `Cart` (lazy)
   - `@OneToMany(mappedBy = "user")` to `Address` (lazy)
   - Do not include `passwordHash` in any `toString()` or logging

3. Create `Address` entity in `com.ebookstore.address.entity`:
   - All columns from §7.2 mapped exactly
   - Column `is_default` maps to Java field `isDefault` (boolean)
   - `@ManyToOne` to `User`

4. Create `Category` entity in `com.ebookstore.catalog.entity`:
   - All columns from §7.3 mapped exactly
   - `active` is `boolean` (no `is_` prefix in Java field name is acceptable)

5. Create `Brand` entity in `com.ebookstore.catalog.entity`:
   - All columns from §7.4 mapped exactly

6. Create `Product` entity in `com.ebookstore.catalog.entity`:
   - All columns from §7.5 mapped exactly
   - `price` is `BigDecimal`
   - `@ManyToOne` to `Category`, `@ManyToOne` to `Brand`

7. Create `Cart` entity in `com.ebookstore.cart.entity`:
   - All columns from §7.6 mapped exactly
   - `status` as `@Enumerated(EnumType.STRING)` using `CartStatus`
   - `@OneToOne` to `User`
   - `@OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)`
     to `CartItem`

8. Create `CartItem` entity in `com.ebookstore.cart.entity`:
   - All columns from §7.7 mapped exactly
   - `unit_price` is `BigDecimal`
   - `@ManyToOne` to `Cart`
   - `@ManyToOne` to `Product`
   - `@Table` annotation includes `uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "product_id"})`

9. Create `Order` entity in `com.ebookstore.order.entity` (use `@Table(name = "orders")`
   to avoid SQL reserved word collision):
   - All 20 columns from §7.8 mapped exactly
   - All monetary columns as `BigDecimal`
   - `status` as `@Enumerated(EnumType.STRING)` using `OrderStatus`
   - `@ManyToOne` to `User`
   - `@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)` to `OrderItem`
   - `@OneToOne(mappedBy = "order")` to `Payment` (lazy)

10. Create `OrderItem` entity in `com.ebookstore.order.entity`:
    - All columns from §7.9 mapped exactly
    - `unit_price` and `subtotal` as `BigDecimal`
    - `@ManyToOne` to `Order`
    - `@ManyToOne(fetch = FetchType.LAZY)` to `Product`
    - Extends `BaseCreatedEntity` — `order_items` has `created_at` but NO `updated_at`;
      do NOT add an `updatedAt` field or `updated_at` column mapping to this entity

11. Create `Payment` entity in `com.ebookstore.payment.entity`:
    - All columns from §7.10 mapped exactly
    - `amount` as `BigDecimal`
    - `paymentMethod` as `@Enumerated(EnumType.STRING)` using `PaymentMethod`
    - `status` as `@Enumerated(EnumType.STRING)` using `PaymentStatus`
    - `@OneToOne` to `Order` (with `@JoinColumn(unique = true)`)

12. Create two base classes in `com.ebookstore.common.entity` — do NOT use Spring Data
    auditing (`@EnableJpaAuditing`, `@CreatedDate`, `@LastModifiedDate`):

    **`BaseCreatedEntity`** — for entities that have only `created_at`:
    - Annotated `@MappedSuperclass`
    - Field: `createdAt` (`LocalDateTime`)
    - `@Column(name = "created_at", nullable = false, updatable = false)`
    - `@PrePersist` method sets `createdAt = LocalDateTime.now()`
    - Used by: `OrderItem`

    **`BaseEntity extends BaseCreatedEntity`** — for entities that have both timestamps:
    - Annotated `@MappedSuperclass`
    - Additional field: `updatedAt` (`LocalDateTime`)
    - `@Column(name = "updated_at", nullable = false)`
    - `@PrePersist` also sets `updatedAt = LocalDateTime.now()`
    - `@PreUpdate` sets `updatedAt = LocalDateTime.now()`
    - Used by: `User`, `Address`, `Category`, `Brand`, `Product`, `Cart`, `CartItem`,
      `Order`, `Payment` (all 9 entities that have both `created_at` and `updated_at`)

**Important Constraints:**
- `Order` entity class name must not conflict with `java.sql.Order` — use
  `com.ebookstore.order.entity.Order` with explicit `@Table(name = "orders")`
- All entity IDs: `Long` — maps to `BIGSERIAL` column
- All monetary entity fields: `BigDecimal` — never `double` or `float`
- All enum fields: `@Enumerated(EnumType.STRING)` — never `ORDINAL`
- Column names in `@Column(name = "...")` annotations must exactly match the
  `snake_case` names from the Flyway migration
- Entities are internal implementation — never serialized to JSON directly; use DTOs
- Do NOT use `@JsonIgnore` as a substitute for proper DTO separation
- Avoid unnecessary bidirectional relationships; do not introduce collections that
  cause N+1 or circular serialization problems
- `equals()` / `hashCode()` on entities: based on `id` field only — must never
  traverse relationships
- Do NOT create entities for Phase-2 tables (`shipments`, `gift_point_accounts`,
  `gift_point_transactions`, `coupons`, `return_requests`, `refunds`)

**Relevant Context:**
- `docs/02-data-model-design.md` §7.1–§7.10 — column definitions
- `AGENTS.md` §Entity — JPA entity rules
- `AGENTS.md` §Monetary Data — `BigDecimal` requirement

**Status:** [ ] pending

---

## Task 4 — Repositories

**Intent:** Create Spring Data JPA repositories for all 10 MVP entities. Include only
query methods needed by the service layer — no business logic.

**Expected Outcomes:**
- All 10 repository interfaces compile
- Each repository extends `JpaRepository<EntityType, Long>`
- Application starts without repository-related errors

**Todo List:**

1. `UserRepository` in `com.ebookstore.user.repository`:
   - `Optional<User> findByEmail(String email)` — used by login and JWT filter
   - `boolean existsByEmail(String email)` — used by registration duplicate check

2. `AddressRepository` in `com.ebookstore.address.repository`:
   - `List<Address> findByUserId(Long userId)` — list user's addresses
   - `Optional<Address> findByIdAndUserId(Long id, Long userId)` — ownership check

3. `CategoryRepository` in `com.ebookstore.catalog.repository`:
   - `List<Category> findByActiveTrue()` — list active categories
   - `boolean existsByName(String name)` (optional, for admin use)

4. `BrandRepository` in `com.ebookstore.catalog.repository`:
   - `List<Brand> findByActiveTrue()` — list active brands

5. `ProductRepository` in `com.ebookstore.catalog.repository`:
   - Extend `JpaRepository` and `JpaSpecificationExecutor<Product>` — needed for
     dynamic search/filter queries
   - `List<Product> findByCategoryIdAndActiveTrueAndIdNot(Long categoryId, Long productId)`
     — related products by category
   - `List<Product> findByBrandIdAndActiveTrueAndIdNot(Long brandId, Long productId)`
     — related products by brand (fallback)

6. `CartRepository` in `com.ebookstore.cart.repository`:
   - `Optional<Cart> findByUserId(Long userId)` — get user's cart

7. `CartItemRepository` in `com.ebookstore.cart.repository`:
   - `Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId)` —
     check for existing item before add (merge vs insert)

8. `OrderRepository` in `com.ebookstore.order.repository`:
   - `Page<Order> findByUserId(Long userId, Pageable pageable)` — order history
   - `Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable)`
     — filtered order history
   - `Optional<Order> findByIdAndUserId(Long id, Long userId)` — ownership check

9. `OrderItemRepository` in `com.ebookstore.order.repository`:
   - Standard `findByOrderId(Long orderId)` (or rely on cascade from entity)

10. `PaymentRepository` in `com.ebookstore.payment.repository`:
    - `Optional<Payment> findByOrderId(Long orderId)` — check for existing payment
    - `boolean existsByOrderIdAndStatusIn(Long orderId, List<PaymentStatus> statuses)` —
      duplicate payment guard

**Important Constraints:**
- Repositories contain only data-retrieval queries — no business logic
- Business rules (48-hour cancellation check, stock validation) stay in service classes
- `ProductRepository` must implement `JpaSpecificationExecutor` for search/filter

**Relevant Context:**
- `docs/02-data-model-design.md` §14 — indexes to inform query design
- `AGENTS.md` §Repository — repositories must not contain business workflows

**Status:** [ ] pending

---

## Task 5 — Common Exception/Response Infrastructure

**Intent:** Build the centralized error-handling and exception infrastructure that every
subsequent module depends on. All domain exceptions and the `GlobalExceptionHandler`
must exist before any service or controller is written, so that error responses conform
to the documented `ErrorResponse` shape from the first endpoint implemented.

**Expected Outcomes:**
- All domain exception classes compile
- `ErrorResponse` DTO is available in `com.ebookstore.common.dto`
- `GlobalExceptionHandler` handles all mapped exception types and returns well-formed
  `ErrorResponse` JSON with the correct HTTP status codes
- Bean Validation failures return `400` with `fieldErrors` populated
- Unhandled exceptions return `500` with a generic message (no stack trace exposed)
- `GlobalExceptionHandlerTest` passes

**Files to Create:**

Exceptions in `com.ebookstore.common.exception`:
- `ResourceNotFoundException` (extends `RuntimeException`)
- `InsufficientStockException` (extends `RuntimeException`)
- `DuplicatePaymentException` (extends `RuntimeException`)
- `OrderCancellationNotAllowedException` (extends `RuntimeException`)
- `BusinessRuleViolationException` (extends `RuntimeException`)
- `UnauthorizedResourceAccessException` (extends `RuntimeException`)
- `InvalidRequestException` (extends `RuntimeException`)

DTO in `com.ebookstore.common.dto`:
- `ErrorResponse`: `timestamp` (OffsetDateTime), `status` (int), `code` (String),
  `message` (String), `path` (String), `fieldErrors` (Map<String, String>, nullable)

Handler in `com.ebookstore.common.exception`:
- `GlobalExceptionHandler` annotated `@RestControllerAdvice`:
  - `ResourceNotFoundException` → `404`
  - `InsufficientStockException` → `409`
  - `DuplicatePaymentException` → `409`
  - `OrderCancellationNotAllowedException` → `409`
  - `BusinessRuleViolationException` → `409`
  - `InvalidRequestException` → `400`
  - `MethodArgumentNotValidException` → `400` with `fieldErrors`
  - `Exception` → `500` generic message
  - Each handler builds `ErrorResponse` with timestamp, status, short machine-readable
    `code` (e.g., `"RESOURCE_NOT_FOUND"`), exception message, and request URI as `path`

**Important Constraints:**
- Never expose stack traces or internal details to API consumers
- `fieldErrors` maps field name → constraint violation message
- Security-specific `401`/`403` handling (via `AuthEntryPoint` and `AccessDeniedHandler`)
  remains in Task 6 (JWT security infrastructure) — it depends on the `ErrorResponse`
  DTO defined here
- `UnauthorizedResourceAccessException` maps to `403`; in practice the ownership pattern
  uses `ResourceNotFoundException` to avoid revealing resource existence

**Unit Tests:**
- `GlobalExceptionHandlerTest`: each exception type produces correct HTTP status and
  `ErrorResponse` shape; `MethodArgumentNotValidException` populates `fieldErrors`

**Dependencies:** Task 1 (Maven project compiles — no entity or repository dependency required)

**Status:** [ ] pending

---

## Task 6 — JWT Security Infrastructure

**Intent:** Build the stateless JWT security layer. This task depends on the
`ErrorResponse` DTO from Task 5 so that `AuthEntryPoint` and `AccessDeniedHandlerImpl`
can return properly structured `401`/`403` responses. No business logic here — only
token generation, validation, and the Spring Security filter chain.

**Expected Outcomes:**
- Any authenticated endpoint called without a valid `Authorization: Bearer <token>` header
  returns `401 Unauthorized` with an `ErrorResponse` body (not Spring Security's default
  HTML error page)
- `403 Forbidden` responses use the same `ErrorResponse` shape — no stack traces or
  Spring Security internal detail exposed
- The public endpoints defined by the OpenAPI contract (`POST /auth/register`,
  `POST /auth/login`, `GET /categories/**`, `GET /brands/**`, `GET /products/**`) are
  accessible without a token; all other endpoints require a valid token
- `GET /actuator/health` is permitted without authentication
- `JwtTokenProviderTest` and `UserDetailsServiceImplTest` pass

**Files to Create:**

All in `com.ebookstore.security`:

1. `AuthenticatedUser.java` — custom `UserDetails` implementation:
   - Fields: `Long id`, `String email`, `String password`, `UserRole role`
   - Implements `UserDetails`: `getUsername()` returns `email`;
     `getPassword()` returns the BCrypt hash; `getAuthorities()` returns a single
     `GrantedAuthority` derived from `role`
   - This is the object placed in the `SecurityContextHolder` after successful JWT
     authentication; controllers retrieve the authenticated user's `Long id` via
     `((AuthenticatedUser) authentication.getPrincipal()).getId()`
   - Must NOT expose the password hash through any `toString()` or serialization path

2. `JwtTokenProvider.java` — generates and validates JWT tokens:
   - `generateToken(AuthenticatedUser principal): String`
   - `extractEmail(String token): String`
   - `isTokenValid(String token, UserDetails userDetails): boolean`
   - Algorithm: `HS256`
   - Secret from environment variable via `app.jwt.secret` — never hardcoded
   - Expiry from `app.jwt.expiration-ms`
   - Token claims: `sub` = email, `iat`, `exp` — do NOT add `userId` as a claim

3. `JwtAuthenticationFilter.java` — `OncePerRequestFilter`:
   - Reads `Authorization: Bearer <token>` header
   - Validates token via `JwtTokenProvider`
   - Loads `AuthenticatedUser` from `UserDetailsServiceImpl`
   - Sets `SecurityContextHolder` with `UsernamePasswordAuthenticationToken` whose
     principal is the `AuthenticatedUser` instance
   - On any token parse or validation error: does not throw — lets the request
     continue unauthenticated; Spring Security will reject it with `401`

4. `UserDetailsServiceImpl.java` — implements `UserDetailsService`:
   - `loadUserByUsername(String email)` delegates to `UserRepository.findByEmail(email)`
   - Returns an `AuthenticatedUser` constructed from the `User` entity: copies `id`,
     `email`, `passwordHash`, and `role`
   - Must NOT log the password hash at any level

5. `SecurityConfig.java` in `com.ebookstore.config`:
   - `@Configuration @EnableWebSecurity`
   - `SecurityFilterChain` bean
   - Session management: `STATELESS` — no `HttpSession` created or used
   - CSRF: disabled (stateless JWT)
   - `permitAll()` for exactly:
     `POST /auth/register`, `POST /auth/login`,
     `GET /categories/**`, `GET /brands/**`, `GET /products/**`,
     `GET /actuator/health`
   - `authenticated()` for all other paths
   - Adds `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`
   - Configures `AuthenticationManager` with `DaoAuthenticationProvider` using
     `UserDetailsServiceImpl` and `BCryptPasswordEncoder`
   - Do NOT add OAuth2, refresh-token endpoints, token-blacklist infrastructure, or
     external identity-provider configuration

6. `AuthEntryPoint.java` — implements `AuthenticationEntryPoint`:
   - Returns `401` with `ErrorResponse` JSON body (uses `ErrorResponse` from Task 5)
   - Must not expose stack traces or Spring Security internal detail
   - Registered in `SecurityConfig` as the `exceptionHandling().authenticationEntryPoint`

7. `AccessDeniedHandlerImpl.java` — implements `AccessDeniedHandler`:
   - Returns `403` with `ErrorResponse` JSON body
   - Must not expose stack traces or Spring Security internal detail
   - Registered in `SecurityConfig` as the `exceptionHandling().accessDeniedHandler`

8. `BCryptPasswordEncoderConfig.java` (or inline in `SecurityConfig`) — defines
   `PasswordEncoder` bean using `BCryptPasswordEncoder`

**Important Constraints:**
- JWT secret: environment variable `JWT_SECRET` only — never hardcoded or in a
  committed config file
- Token claims: `sub` = email, `iat`, `exp` only — do NOT add `userId` as a JWT claim;
  the database user ID is available through `AuthenticatedUser`, not the token
- Token storage: stateless — no server-side session, no token blacklist, no Redis cache
- No refresh-token infrastructure in MVP
- No OAuth2, no external identity providers
- No ADMIN-only endpoints invented beyond what the OpenAPI contract defines
- `UserDetailsServiceImpl` must return `AuthenticatedUser`, not the generic Spring
  `User`; the `Long id` field is required so controllers can pass it to services
- `UserDetailsServiceImpl` must NOT log the password hash
- `401` and `403` responses must use the documented `ErrorResponse` shape; no HTML,
  no stack trace, no Spring Security default error format

**Transaction Boundaries:** None — this module is stateless filter logic only.

**Unit Tests:**
- `JwtTokenProviderTest`: generate token from `AuthenticatedUser`, validate valid token,
  reject expired token, reject tampered token, reject token signed with wrong secret;
  verify generated token does NOT contain a `userId` claim
- `UserDetailsServiceImplTest`: load existing user by email returns `AuthenticatedUser`
  with correct `id`, `email`, `role`; throw `UsernameNotFoundException` when user not found

**Relevant Context:**
- `AGENTS.md` §Security — JWT stateless, BCrypt, no secrets in code
- `docs/03-openapi-specification.yaml` — `securitySchemes.bearerAuth`, `security: []`
  on public endpoints

**Dependencies:** Task 4 (UserRepository), Task 5 (ErrorResponse DTO)

**Status:** [ ] pending

---

## Task 7 — Authentication and User Profile

**Intent:** Implement registration, login, logout, and current-user profile. These are
the first business-logic endpoints. Login returns the JWT token; all other modules
depend on the authenticated principal provided by Task 6.

**Expected Outcomes:**
- `POST /api/auth/register` atomically creates a `User` (role `CUSTOMER`, status
  `ACTIVE`) and an empty `Cart` in a single transaction; returns `201 UserResponse`
- `POST /api/auth/login` returns `200 LoginResponse` with `accessToken`
- `POST /api/auth/logout` returns `204` with no body (stateless — no server-side
  revocation; JWT expiry is the logout mechanism)
- `GET /api/users/me` returns `200 UserResponse` for the authenticated user
- Duplicate email registration returns `409` with `ErrorResponse`
- Invalid login credentials return `401` with `ErrorResponse`
- All error responses conform to the `ErrorResponse` shape from Task 5

**Files to Create:**

DTOs in `com.ebookstore.auth.dto` and `com.ebookstore.user.dto`:
- `RegisterRequest`: `firstName`, `lastName`, `email`, `password` (all validated with
  Bean Validation matching OpenAPI constraints: `minLength:1/maxLength:100` for name,
  `format: email` for email, `minLength:8/maxLength:100` for password)
- `LoginRequest`: `email`, `password`
- `LoginResponse`: `accessToken`, `tokenType` (= `"Bearer"`), `expiresIn` (seconds),
  `user` (embedded `UserResponse`)
- `UserResponse`: `id`, `firstName`, `lastName`, `email`, `role`, `status`, `createdAt`

Services:
- `AuthService` in `com.ebookstore.auth.service`:
  - `register(RegisterRequest): UserResponse` — within a single `@Transactional`:
    1. Check no existing user with the same email; throw `DuplicateEmailException`
       (or `BusinessRuleViolationException`) → `409` if duplicate
    2. Hash password with `BCryptPasswordEncoder`
    3. Save `User` with `role = CUSTOMER`, `status = ACTIVE`
    4. Create an empty `Cart` linked to the new user (enforces 1:1 User-Cart)
    5. Return `UserResponse`
  - `login(LoginRequest): LoginResponse` — authenticates via Spring
     `AuthenticationManager` (delegates to `UserDetailsServiceImpl` + BCrypt);
     the `Authentication` result's principal is an `AuthenticatedUser`;
     generates JWT via `JwtTokenProvider`; no transaction needed
- `UserService` in `com.ebookstore.user.service`:
  - `getCurrentUser(Long userId): UserResponse` — loads user by id, returns DTO

Controllers:
- `AuthController` in `com.ebookstore.auth.controller` — maps `POST /auth/register`,
  `POST /auth/login`, `POST /auth/logout`
- `UserController` in `com.ebookstore.user.controller` — maps `GET /users/me`;
  extracts the authenticated user's `id` via
  `((AuthenticatedUser) authentication.getPrincipal()).getId()`

Mappers:
- `UserMapper` in `com.ebookstore.user` — `User` → `UserResponse`
  (hand-written mapper — no MapStruct unless already in pom.xml)

**Important Constraints:**
- `register` is atomic: User + Cart created in one `@Transactional`; if Cart creation
  fails, User must also roll back
- New users always get `role = CUSTOMER` and `status = ACTIVE` — never any other
  initial role or status
- Password must never appear in logs, responses, or `toString()`
- `logout` is stateless: returns `204` with no body; no server-side token storage,
  no blacklist, no session invalidation; JWT expiry handles logout
- Do not add refresh-token endpoints, token-rotation logic, or session management
- `operationId` names must match exactly: `registerUser`, `login`, `logout`,
  `getCurrentUser`

**Transaction Boundaries:**
- `AuthService.register()`: `@Transactional` — creates User + Cart atomically
- `AuthService.login()`: no transaction needed (read only + JWT generation)

**Authorization:** `register` and `login` are public (`security: []` in OpenAPI).
`logout` and `GET /users/me` require a valid JWT (enforced by `SecurityConfig`).

**Unit Tests:**
- `AuthServiceTest`: successful registration, duplicate email throws, valid login,
  invalid credentials throws
- `UserServiceTest`: get current user found, get current user not found throws

**Integration Tests:**
- `AuthControllerIT`: register → 201, duplicate register → 409, login → 200 with token,
  bad credentials → 401, `/users/me` with token → 200, `/users/me` without token → 401

**Dependencies:** Task 3 (User entity), Task 4 (UserRepository, CartRepository),
Task 5 (ErrorResponse, exceptions), Task 6 (JwtTokenProvider, SecurityConfig)

**Status:** [ ] pending

---

## Task 8 — Catalog Module

**Intent:** Implement the fully public catalog: categories, brands, product listing with
search/filter/pagination, product detail, and related products. All these endpoints have
`security: []` — no authentication required.

*(Was Task 7 — renumbered to Task 8.)*

**Expected Outcomes:**
- `GET /api/categories` returns array of `CategorySummary`
- `GET /api/brands` returns array of `BrandSummary`
- `GET /api/products` returns `PagedProductResponse` with filter params working
- `GET /api/products/{productId}` returns `ProductResponse` with nested category,
  brand, and delivery estimate
- `GET /api/products/{productId}/related` returns array of `ProductSummary`
- `GET /api/categories/{categoryId}/products` returns `PagedProductResponse`
- `GET /api/brands/{brandId}/products` returns `PagedProductResponse`
- Products not found return `404 ErrorResponse`
- Pagination envelope is `{ content: [...], page: { page, size, totalElements, totalPages } }`
  — not Spring's default `Page` serialization

**Files to Create:**

DTOs in `com.ebookstore.catalog.dto`:
- `CategorySummary`: `id`, `name`, `description`, `active`
- `BrandSummary`: `id`, `name`, `description`, `active`
- `ProductSummary`: `id`, `title`, `isbn`, `price` (BigDecimal), `available` (boolean),
  `stockQuantity`
- `ProductResponse`: extends/includes `ProductSummary` + `description`, `category`
  (CategorySummary), `brand` (BrandSummary), `deliveryEstimate` (DeliveryEstimate)
- `DeliveryEstimate`: `minDays`, `maxDays`

Common DTOs in `com.ebookstore.common.dto`:
- `PageMetadata`: `page`, `size`, `totalElements` (long), `totalPages`
- `PagedResponse<T>`: `content: List<T>`, `page: PageMetadata`
  (used for `PagedProductResponse` and `PagedOrderResponse`)

Services:
- `CategoryService` in `com.ebookstore.catalog.service`:
  - `listActiveCategories(): List<CategorySummary>`
- `BrandService` in `com.ebookstore.catalog.service`:
  - `listActiveBrands(): List<BrandSummary>`
- `ProductService` in `com.ebookstore.catalog.service`:
  - `searchProducts(q, categoryId, brandId, minPrice, maxPrice, availableOnly, pageable): PagedResponse<ProductSummary>`
    — builds `Specification<Product>` dynamically from non-null parameters
  - `getProduct(Long id): ProductResponse`
  - `getRelatedProducts(Long productId, int size): List<ProductSummary>` — returns
    products from the same category (excluding self); falls back to same brand if
    insufficient results
  - `getProductsByCategory(Long categoryId, Pageable pageable): PagedResponse<ProductSummary>`
  - `getProductsByBrand(Long brandId, Pageable pageable): PagedResponse<ProductSummary>`

Specification class:
- `ProductSpecification` in `com.ebookstore.catalog.repository`:
  - Static factory methods for each filter predicate: `hasTitleContaining(q)`,
    `hasCategory(categoryId)`, `hasBrand(brandId)`, `hasPriceAtLeast(minPrice)`,
    `hasPriceAtMost(maxPrice)`, `isAvailable()` (active = true AND stock > 0)
  - Methods return `Specification<Product>` that can be composed with `.and()`

Controllers:
- `CategoryController` — `GET /categories`, `GET /categories/{categoryId}/products`
- `BrandController` — `GET /brands`, `GET /brands/{brandId}/products`
- `ProductController` — `GET /products`, `GET /products/{productId}`,
  `GET /products/{productId}/related`

Pagination note:
- Spring's `Pageable` supports the `sort` query parameter in Spring-style format
  (`title,asc`) — configure `SpringDataWebAutoConfiguration` (auto-enabled with
  Spring Boot Web) and map the custom `page` envelope in the service layer
- The controller receives `Pageable` from Spring; the service wraps `Page<T>` into
  `PagedResponse<T>` using `PageMetadata`

**Important Constraints:**
- `price` in `ProductSummary` is `BigDecimal` in Java — OpenAPI `format: double` is a
  presentation hint only; never use `double` or `float`
- `available` derived field: `product.active == true && product.stockQuantity > 0`
- Related products algorithm — strictly in this order:
  1. Query active, in-stock products with the same `category_id`, excluding the
     current product
  2. If result count < requested `size`, supplement from products with the same
     `brand_id` (excluding the current product and any already in step-1 results)
  3. Return combined list up to `size` — all results must be active and in stock
  4. No Elasticsearch, Redis, external search engine, or ML-based ranking
- `GET /products/{productId}` returns `404` if product does not exist (regardless of
  active status — the product record exists even if inactive)
- Sort default is `title,asc` per the OpenAPI `Sort` parameter default
- No external search infrastructure — all queries via `JpaSpecificationExecutor`
  and `ProductSpecification` static predicates

**Transaction Boundaries:** All read-only — `@Transactional(readOnly = true)` on
service methods.

**Authorization:** All endpoints public — no auth required.

**Unit Tests:**
- `ProductServiceTest`:
  - search with `q` filter, `categoryId` filter, `brandId` filter, `minPrice`,
    `maxPrice`, `availableOnly` — each individually and in combination
  - get product found, get product not found → throws `ResourceNotFoundException`
  - related products: same-category results returned first; brand supplements when
    category results are fewer than `size`; current product excluded; only
    active/in-stock included
- `ProductSpecificationTest`: each `ProductSpecification` predicate produces the
  correct JPA `Predicate`; composition with `.and()` works correctly

**Integration Tests:**
- `CatalogControllerIT`:
  - list categories, list brands — no token required
  - search with `q`, `categoryId`, `brandId`, `minPrice`, `maxPrice`, `availableOnly`
  - pagination: correct `page`, `size`, `totalElements`, `totalPages` in envelope
  - sort: `title,asc` default; client-supplied `sort` parameter respected for catalog
  - `GET /products/{id}` found → correct response shape
  - `GET /products/{id}` not found → 404 `ErrorResponse`
  - `GET /products/{id}/related` — same-category results, brand supplement

**Dependencies:** Task 3 (entities), Task 4 (ProductRepository with JpaSpecificationExecutor),
Task 5 (ErrorResponse, ResourceNotFoundException)

**Status:** [ ] pending

---

## Task 9 — Addresses

**Intent:** Implement CRUD for a user's saved delivery addresses. All endpoints are
authenticated. Ownership enforcement ensures a user can only access their own addresses.

*(Was Task 8 — renumbered to Task 9.)*

**Expected Outcomes:**
- `GET /api/addresses` returns the authenticated user's addresses
- `POST /api/addresses` creates a new address and returns `201 AddressResponse`
- `PUT /api/addresses/{addressId}` updates an address and returns `200 AddressResponse`
- `DELETE /api/addresses/{addressId}` deletes an address and returns `204`
- Accessing another user's address returns `404` (not `403` — do not reveal existence)
- Invalid request body returns `400 ErrorResponse` with `fieldErrors`

**Files to Create:**

DTOs in `com.ebookstore.address.dto`:
- `AddressRequest`: `label`, `addressLine1` (required), `addressLine2`, `city`
  (required), `state` (required), `postalCode` (required, `minLength:3`), `country`
  (required, `minLength:2`), `isDefault` (default: false) — with Bean Validation
  annotations matching OpenAPI constraints
- `AddressResponse`: `id` + all `AddressRequest` fields (use composition or inheritance)

Service:
- `AddressService` in `com.ebookstore.address.service`:
  - `listAddresses(Long userId): List<AddressResponse>`
  - `createAddress(Long userId, AddressRequest): AddressResponse`
  - `updateAddress(Long userId, Long addressId, AddressRequest): AddressResponse` —
    calls `findByIdAndUserId` to enforce ownership; throws `ResourceNotFoundException`
    if not found
  - `deleteAddress(Long userId, Long addressId): void` — same ownership check

Controller:
- `AddressController` in `com.ebookstore.address.controller` — maps all 4 operations;
  extracts `userId` via `((AuthenticatedUser) authentication.getPrincipal()).getId()`
  and passes it to the service; never reads `userId` from the request body

**Important Constraints:**
- `userId` must always come from `AuthenticatedUser` in the `SecurityContextHolder`,
  never from a request parameter or request body
- Ownership: use `findByIdAndUserId()` — do not load the address and then check ownership
  in application code (prevents information leakage)
- `operationId` names: `listAddresses`, `createAddress`, `updateAddress`, `deleteAddress`

**Transaction Boundaries:**
- `createAddress`, `updateAddress`, `deleteAddress`: `@Transactional`
- `listAddresses`: `@Transactional(readOnly = true)`

**Authorization:** All endpoints require JWT. Ownership enforced in service layer.

**Unit Tests:**
- `AddressServiceTest`: list addresses, create address, update address found, update
  address not found (throws ResourceNotFoundException), delete found, delete not found

**Integration Tests:**
- `AddressControllerIT`: full CRUD with valid token; GET/PUT/DELETE another user's
  address → 404

**Dependencies:** Task 3 (Address entity), Task 4 (AddressRepository),
Task 5 (ErrorResponse, ResourceNotFoundException), Task 6 (JWT auth)

**Status:** [ ] pending

---

## Task 10 — Cart

**Intent:** Implement cart retrieval, add item (with merge logic for duplicates), update
quantity, and remove item. The cart is per-user (1:1). Cart price is display-only — not
authoritative for checkout. The same Cart entity is reused after every checkout — it is
never replaced and never left in a permanent non-ACTIVE state.

*(Was Task 9 — renumbered to Task 10.)*

**Expected Outcomes:**
- `GET /api/cart` returns `CartResponse` for the authenticated user (cart created on
  registration, always exists, always `ACTIVE` between checkouts)
- `POST /api/cart/items` adds a product; if product already in cart, merges quantities;
  returns `201 CartResponse`; returns `409` if stock is insufficient
- `PUT /api/cart/items/{itemId}` updates quantity; returns `409` if stock insufficient
- `DELETE /api/cart/items/{itemId}` removes item; returns `204`
- After checkout the cart has no items and `status = ACTIVE`; new items can be added
  immediately
- `CartResponse.recommendedProducts` returns up to 4 active, in-stock products derived
  from the user's purchase history — excludes items already in the cart; returns empty
  list when no qualifying products exist

**Files to Create:**

DTOs in `com.ebookstore.cart.dto`:
- `AddCartItemRequest`: `productId` (long, min 1), `quantity` (int, min 1, max 999)
- `UpdateCartItemRequest`: `quantity` (int, min 1, max 999)
- `CartItemResponse`: `id`, `product` (ProductSummary), `quantity`, `unitPrice`
  (BigDecimal), `subtotal` (BigDecimal, = unitPrice × quantity)
- `CartResponse`: `id`, `status` (CartStatus), `items` (list of CartItemResponse),
  `subtotal` (BigDecimal), `totalAmount` (BigDecimal),
  `recommendedProducts` (list of ProductSummary, optional — may be empty list)

Service:
- `CartService` in `com.ebookstore.cart.service`:
  - `getCart(Long userId): CartResponse`
  - `addCartItem(Long userId, AddCartItemRequest): CartResponse` —
    1. Load product; throw `ResourceNotFoundException` if not found
    2. Check `product.active && product.stockQuantity >= requestedQty`; throw
       `InsufficientStockException` → `409` if not
    3. Load cart by userId; check if `CartItem` already exists for this product
    4. If exists: update `quantity = existing + requested`; if total > stock → `409`
    5. If not exists: create new `CartItem`; set `unit_price = product.price` (display
       snapshot only — not authoritative for checkout)
    6. Save; return updated `CartResponse`
  - `updateCartItem(Long userId, Long itemId, UpdateCartItemRequest): CartResponse` —
    load item (verify ownership via cart → user), check stock, update quantity
  - `removeCartItem(Long userId, Long itemId): void`
  - Private helper `buildCartResponse(Cart cart, Long userId): CartResponse`:
    - Calculate `subtotal` and `totalAmount` from `CartItem.unitPrice × quantity`
      (display values; these are NOT used for checkout totals)
    - Populate `recommendedProducts`:
      - Query `order_items` for the cart owner's past purchases to collect their
        historical category IDs
      - Find active, in-stock products in those categories
      - Exclude products already in the cart
      - Limit to 4 results
      - If no qualifying products exist, return empty list
      - No global popular-products fallback; no ML; no persistence of recommendations

Controller:
- `CartController` in `com.ebookstore.cart.controller` — maps all 4 operations;
  extracts `userId` via `((AuthenticatedUser) authentication.getPrincipal()).getId()`;
  returns `201` for add, `200` for update, `204` for delete

**Important Constraints:**
- Cart always exists (created at registration) — `getCart` never creates a cart
- Cart is a persistent, reusable entity. Its `CartItem` rows are cleared after checkout
  but the `Cart` row itself is kept and its status is reset to `ACTIVE`. A second Cart
  must never be created for the same user.
- `cart_items.unit_price` is the current product price at add-time; it is a **display
  snapshot only** — checkout must re-fetch `products.price` and must not use this field
  for order totals or `order_items.unit_price`
- Merge logic: `POST /cart/items` with an existing `productId` must UPDATE the existing
  `CartItem.quantity`, not insert a new row (enforced by `UNIQUE(cart_id, product_id)`)
- `CHECKED_OUT` is a valid `CartStatus` enum value that must remain in the enum but must
  NOT be the resting state of the persistent cart after checkout. The cart is returned to
  `ACTIVE` at the end of checkout.
- `recommendedProducts`: up to 4 active, in-stock products from purchase-history
  categories, excluding products already in cart; empty list when no qualifiers exist;
  no fallback to global popularity; no recommendation table
- `userId` must always come from `AuthenticatedUser` in the `SecurityContextHolder`,
  never from request body or query params
- `operationId` names: `getCart`, `addCartItem`, `updateCartItem`, `removeCartItem`

**Transaction Boundaries:**
- `addCartItem`, `updateCartItem`, `removeCartItem`: `@Transactional`
- `getCart`: `@Transactional(readOnly = true)`

**Authorization:** All endpoints require JWT.

**Unit Tests:**
- `CartServiceTest`: get cart, add new item, add existing item (merge), add item
  exceeds stock → throws, update item found, update exceeds stock → throws, remove item

**Integration Tests:**
- `CartControllerIT`: add item to cart, add same item again (quantity merged), add item
  with stock = 0 → 409, update quantity, remove item, get cart structure matches
  `CartResponse` schema; after checkout verify cart still exists with `status = ACTIVE`
  and zero items

**Dependencies:** Task 3 (Cart, CartItem, Product entities), Task 4 (CartRepository,
CartItemRepository), Task 5 (ErrorResponse, InsufficientStockException),
Task 6 (JWT auth), Task 8 (ProductSummary DTO)

**Status:** [ ] pending

---

## Task 11 — Orders and Checkout

**Intent:** Implement order creation (checkout), order history, get order, Buy Again,
and 48-hour order cancellation. This is the most complex module. Checkout must execute
inside a single transaction.

*(Was Task 10 — renumbered to Task 11.)*

**Expected Outcomes:**
- `POST /api/orders` creates an order from the active cart; returns `201 OrderResponse`
- Empty-cart checkout returns `409`
- Product no longer available at checkout returns `409`
- `GET /api/orders` returns paginated order history sorted `placed_at DESC`
- `GET /api/orders/{orderId}` returns `OrderResponse` (user's own orders only)
- `POST /api/orders/{orderId}/buy-again` re-adds historically purchased products to the
  active cart; returns `200 CartResponse`
- `POST /api/orders/{orderId}/cancel` cancels eligible orders; returns `200 OrderResponse`
  with status `CANCELLED`; returns `409` if past deadline or status does not permit
- Accessing another user's order returns `404`

**Files to Create:**

DTOs in `com.ebookstore.order.dto`:
- `CreateOrderRequest`: `addressId` (long, min 1) — **only field**; do NOT add
  `couponCode`, `giftPointsToRedeem`, or any Phase-2 field
- `OrderItemResponse`: `id`, `productId`, `productTitle`, `quantity`, `unitPrice`
  (BigDecimal), `subtotal` (BigDecimal)
  — `unitPrice` is the purchase-time snapshot from `order_items.unit_price`, NOT the
  current product price
- `ShippingAddressSnapshot`: `name`, `addressLine1`, `addressLine2`, `city`, `state`,
  `postalCode`, `country`
- `OrderResponse`: `id`, `orderNumber`, `status`, `items` (list), `shippingAddress`,
  `subtotal` (BigDecimal), `shippingAmount` (BigDecimal), `discountAmount` (BigDecimal),
  `totalAmount` (BigDecimal), `placedAt`, `cancellationDeadline` (nullable)
  — do NOT include `giftPointsUsed` or any Phase-2 field in this DTO

Service:
- `OrderService` in `com.ebookstore.order.service`:

  **`createOrder(Long userId, CreateOrderRequest)`** — `@Transactional` (single
  transaction covering the entire checkout sequence):
  1. Load the user's Cart; throw `InvalidRequestException` → `409` if it has no items
  2. Load the Address by `addressId`; verify ownership via `findByIdAndUserId`;
     throw `ResourceNotFoundException` if not found or not owned by this user —
     never accept `userId` from the request body
  3. Re-fetch all Products from the database by their IDs (do NOT rely on cart
     `unit_price` snapshots for any calculation)
  4. Validate each product: `product.active == true`; throw `BusinessRuleViolationException`
     → `409` if any product is inactive
  5. Validate stock: `product.stockQuantity >= item.quantity` for each item; throw
     `InsufficientStockException` → `409` if any item fails
  6. Calculate authoritative `subtotal = sum(product.price × item.quantity)` using
     live `products.price` — not `cart_items.unit_price`
  7. Set `shippingAmount = ZERO`, `discountAmount = ZERO` (no coupon — Phase 2),
     `giftPointsUsed = 0` (no gift points — Phase 2), `totalAmount = subtotal`
     — persist these exact values; do not accept them from the client
  8. Generate unique `orderNumber` (e.g., `"ORD-" + UUID suffix`)
  9. Snapshot shipping address from the selected Address (copy all 7 fields into
     `orders` row — not a foreign key)
  10. Create `Order` entity with status `PENDING_PAYMENT`; set `placedAt = now()`;
      set `cancellationDeadline = placedAt + 48 hours`
  11. For each CartItem: create `OrderItem` with:
      - `product_title` snapshotted from current `product.title`
      - `unit_price` = current `product.price` (authoritative; this becomes the
        historical purchase-price snapshot — NOT `cart_items.unit_price`)
  12. Decrement `product.stockQuantity` by `item.quantity` for each item; save Products
  13. Clear all CartItem rows from the cart (delete them); set `cart.status = ACTIVE`
      — the Cart entity itself is retained and reused for all future shopping
  14. Save Order, OrderItems, cleared Cart, and updated Products within the same transaction
  15. Do NOT create a Payment record — Payment is created separately via
      `POST /payments` (Task 12)
  16. Return `OrderResponse`

  **Checkout sequence rule:** steps 1–14 are atomic. If any validation fails, the
  entire transaction rolls back. Stock is decremented only after all validations pass.
  Cart price (`cart_items.unit_price`) is NEVER used for order total calculations.
  The Cart row is NEVER deleted or replaced — only its items are cleared and its
  status reset to `ACTIVE`.

  **`listOrders(Long userId, OrderStatus status, Pageable pageable)`** —
  `@Transactional(readOnly = true)`:
  - Pageable must be constructed with fixed sort `placed_at DESC` (override any client
    sort)

  **`getOrder(Long userId, Long orderId)`** — `@Transactional(readOnly = true)`:
  - Use `findByIdAndUserId` for ownership

  **`buyAgain(Long userId, Long orderId)`** — `@Transactional`:
  1. Load order; verify ownership via `findByIdAndUserId`; throw
     `ResourceNotFoundException` if not found or not owned
  2. For each `OrderItem` in the historical order: re-fetch the current `Product` by
     its `productId`
  3. Check current availability: `product.active == true && product.stockQuantity > 0`
  4. Skip products that are inactive or out of stock (do not throw for individual items)
  5. If ALL products are unavailable, throw `BusinessRuleViolationException` → `409`
  6. For each available product: add to the user's active cart using the same merge
     logic as `CartService.addCartItem`:
     - Use current `product.price` (not historical `order_items.unit_price`)
     - Respect current stock limits
     - Merge quantity if product already in cart
  7. Return updated `CartResponse`

  **`cancelOrder(Long userId, Long orderId)`** — `@Transactional`:
  1. Load order with ownership check
  2. Verify `order.status` is `PENDING_PAYMENT` or `PAID`; throw
     `OrderCancellationNotAllowedException` → `409` if status does not permit cancellation
     (`CONFIRMED`, `SHIPPED`, `DELIVERED`, and other statuses block cancellation)
  3. Verify `Instant.now().isBefore(order.cancellationDeadline)`; throw
     `OrderCancellationNotAllowedException` → `409` if deadline has passed
  4. Set `order.status = CANCELLED`
  5. Restore stock: for each `OrderItem`, increment `product.stockQuantity` by
     `orderItem.quantity`; save each Product
  6. If `order.status` was `PAID` (i.e., a `Payment` record exists with `SUCCESS`):
     - Set `order.status = CANCELLED`
     - Leave `payment.status = SUCCESS` (do NOT set to `REFUNDED` — refund processing
       is Phase 2)
     - A cancelled-and-paid order may temporarily hold
       `OrderStatus = CANCELLED` / `PaymentStatus = SUCCESS`; this is the correct MVP
       state
  7. Save Order (and Products) and return `OrderResponse`

Controller:
- `OrderController` in `com.ebookstore.order.controller` — maps all 5 operations
- `listOrders` receives optional `status` query param; constructs `Pageable` with
  fixed `placed_at DESC` sort — client cannot override the sort parameter
- All `userId` values are extracted via
  `((AuthenticatedUser) authentication.getPrincipal()).getId()`, never from request
  body or path variables

**Important Constraints:**
- `CreateOrderRequest` has ONLY `addressId` — do NOT add `couponCode`,
  `giftPointsToRedeem`, or any Phase-2 field
- `orders.gift_points_used`: always persist `0`; never validate, calculate, or expose
  this field in the MVP response — the column exists for future Phase-2 compatibility
- `orders.discount_amount`: always persist `ZERO`; no coupon validation or persistence
- `order_items.unit_price` = authoritative snapshot from `products.price` at checkout;
  `cart_items.unit_price` is NEVER used as the order price source
- `OrderResponse` does NOT include `giftPointsUsed`
- `GET /orders` sort is fixed `placed_at DESC`; no client-controlled sort parameter
- `cancellationDeadline = placedAt + 48 hours` stored in DB
- MVP cancellation allows `PENDING_PAYMENT → CANCELLED` and `PAID → CANCELLED` only,
  and only while `now <= cancellationDeadline`; `CONFIRMED` does NOT permit cancellation
- For `PAID → CANCELLED`: restore stock, set order to `CANCELLED`, leave
  `PaymentStatus = SUCCESS`; do NOT set `PaymentStatus = REFUNDED`
- Both status and deadline conditions enforced in service layer, not controller
- `POST /orders` must NOT create a Payment record
- Stock is decremented inside the checkout transaction after all validations pass;
  stock is restored on cancellation
- `orderNumber` must be unique and server-generated
- Ownership for orders and payments: use `findByIdAndUserId`; return `404` (not `403`)
  when a resource is not found or not owned
- `userId` always from `AuthenticatedUser` principal — never from request body or
  query params; never decoded from JWT claims
- `operationId` names: `listOrders`, `createOrder`, `getOrder`, `buyAgain`, `cancelOrder`
- Buy Again: always use current `products.price`; never use historical `order_items.unit_price`
- `cancelOrder` uses injected `java.time.Clock` to evaluate `now <= cancellationDeadline`;
  production uses `Clock.systemUTC()`; tests inject a fixed `Clock`

**Transaction Boundaries:**
- `createOrder`: single `@Transactional` covering validation + order creation +
  stock decrement + cart items cleared + cart status reset to `ACTIVE`
- `cancelOrder`: single `@Transactional` covering status update + stock restore
- `buyAgain`: `@Transactional` covering cart updates

**Authorization:** All endpoints require JWT. Ownership enforced via `findByIdAndUserId`.

**Unit Tests:**
- `OrderServiceTest`:
  - successful checkout: validates cart → validates address → re-fetches products →
    validates active/stock → calculates totals → snapshots address → creates order with
    `PENDING_PAYMENT` → decrements stock → clears cart items → sets cart status to
    `ACTIVE` → returns `OrderResponse` with no Payment created; verify the same
    Cart entity is retained (not deleted, not replaced)
  - empty cart → throws `InvalidRequestException`
  - inactive product at checkout → throws `BusinessRuleViolationException`
  - insufficient stock → throws `InsufficientStockException`
  - order history returned paginated sorted `placed_at DESC`
  - order history filtered by status
  - get own order, get another user's order → throws
  - cancel `PENDING_PAYMENT` within deadline → `CANCELLED`, stock restored
  - cancel `PAID` within deadline → `CANCELLED`, stock restored, payment stays `SUCCESS`
  - cancel after deadline → throws `OrderCancellationNotAllowedException`
  - cancel `CONFIRMED` → throws `OrderCancellationNotAllowedException`
  - buy again: all available products added to cart using current price
  - buy again: some products unavailable → available ones added, unavailable skipped
  - buy again: all products unavailable → throws `BusinessRuleViolationException`
  - buy again: historical price NOT used (assert `CartItem.unitPrice == product.price`,
    not `orderItem.unitPrice`)

**Integration Tests:**
- `OrderControllerIT`:
  - full checkout flow: register → add to cart → checkout → 201 `OrderResponse` with
    `status = PENDING_PAYMENT`; verify no Payment record created by `POST /orders`
  - **Cart lifecycle after checkout:** after `POST /orders` succeeds, call `GET /cart`
    and verify: cart still exists, `status = ACTIVE`, `items` is empty; verify the cart
    `id` is the same cart created at registration (no new cart created)
  - **Cart is immediately reusable:** add another item after checkout → `POST /cart/items`
    succeeds on the same cart; no error
  - empty cart checkout → 409
  - price snapshot: update product price after cart add; verify order uses checkout-time
    `products.price`, not `cart_items.unit_price`
  - address snapshot: update address after order; verify order retains original snapshot
  - **Stock decrement:** verify `products.stock_quantity` decremented after checkout
  - get order history sorted `placed_at DESC`; no client sort override accepted
  - filter order history by status
  - `GET /orders/{id}` ownership check → 404 for another user's order
  - cancel `PENDING_PAYMENT` within 48h → 200 `CANCELLED`, stock restored
  - cancel `PAID` within 48h → 200 `CANCELLED`, stock restored, payment stays `SUCCESS`
  - cancel after deadline → 409
  - buy again: verify `CartItem.unitPrice` reflects current product price (not historical);
    products are added to the existing persistent cart
  - buy again: some products out of stock → only available ones added

**Dependencies:** Task 3 (Order, OrderItem, Cart, CartItem, Product, Address entities),
Task 4 (OrderRepository, CartRepository), Task 5 (ErrorResponse, exceptions),
Task 6 (JWT), Task 8 (ProductSummary), Task 10 (cart logic for Buy Again)

**Status:** [ ] pending

---

## Task 12 — Payment

**Intent:** Implement the simulated payment processor. Payment is a separate step after
order creation. `POST /orders` leaves the order in `PENDING_PAYMENT` with no payment
record. `POST /payments` creates the payment, simulates processing, and transitions the
order to `PAID`. No real payment gateway is integrated.

*(Was Task 11 — renumbered to Task 12.)*

**Expected Outcomes:**
- `POST /api/payments` creates a payment for a `PENDING_PAYMENT` order owned by the
  authenticated user; simulated processing immediately succeeds; returns
  `201 PaymentResponse` with `status = SUCCESS` and `paidAt` populated
- After successful payment: `order.status = PAID` (not `CONFIRMED`)
- `GET /api/payments/{paymentId}` returns `PaymentResponse` for the authenticated
  user's payment
- A second `POST /api/payments` for the same order that already has a `SUCCESS` or
  `PROCESSING` payment returns `409 ErrorResponse`
- Payment for an order not owned by the authenticated user returns `404`
- Payment amount is always `order.totalAmount` — the client must not provide an amount

**Files to Create:**

DTOs in `com.ebookstore.payment.dto`:
- `CreatePaymentRequest`: `orderId` (long, min 1), `paymentMethod` (PaymentMethod enum)
  — amount is NOT a field; it is read from `order.totalAmount` server-side
- `PaymentResponse`: `id`, `orderId`, `paymentReference`, `paymentMethod`, `amount`
  (BigDecimal), `status` (PaymentStatus), `paidAt` (nullable)

Service:
- `PaymentService` in `com.ebookstore.payment.service`:

  **`initiatePayment(Long userId, CreatePaymentRequest)`** — `@Transactional`:
  1. Load Order by `orderId`; verify ownership with `findByIdAndUserId`; throw
     `ResourceNotFoundException` if not found or not owned — `userId` from JWT only
  2. Verify `order.status == PENDING_PAYMENT`; throw `BusinessRuleViolationException`
     → `409` if not (order may already be `PAID`, `CANCELLED`, etc.)
  3. Check no existing payment in `SUCCESS` or `PROCESSING` state:
     `existsByOrderIdAndStatusIn(orderId, List.of(SUCCESS, PROCESSING))`;
     throw `DuplicatePaymentException` → `409` if true
  4. Generate `paymentReference` (UUID)
  5. Set `payment.amount = order.totalAmount` — never from the client request
  6. Create `Payment` entity: `status = INITIATED`
  7. Save payment
  8. Delegate to `PaymentProcessor.process(payment)`:
     `SimulatedPaymentProcessor` always returns `SUCCESS` (synchronous, no external call)
  9. Update `payment.status = SUCCESS`, `payment.paidAt = now()`
  10. Update `order.status = PAID`
      (successful-payment transition is `PENDING_PAYMENT → PAID`; not `CONFIRMED`)
  11. Save both payment and order within the same transaction
  12. Return `PaymentResponse`

  **`getPayment(Long userId, Long paymentId)`** — `@Transactional(readOnly = true)`:
  1. Load Payment by id; throw `ResourceNotFoundException` if not found
  2. Load associated Order via `payment.getOrder()`; verify `order.userId == userId`;
     throw `ResourceNotFoundException` if not owned
  3. Return `PaymentResponse`

Controller:
- `PaymentController` in `com.ebookstore.payment.controller` — maps
  `POST /payments`, `GET /payments/{paymentId}`
- `userId` extracted via `((AuthenticatedUser) authentication.getPrincipal()).getId()`
  — never from request body

**Payment Processor Abstraction:**
- `PaymentProcessor` interface in `com.ebookstore.payment.service`:
  - single method: `PaymentResult process(Payment payment)`
- `SimulatedPaymentProcessor` implements `PaymentProcessor`:
  - always returns `SUCCESS` immediately (synchronous, no external SDK or gateway)
  - this abstraction allows a real gateway to be substituted in a later phase without
    redesigning the order/payment domain
- Do NOT add external gateway SDKs, HTTP clients, or integration infrastructure

**Important Constraints:**
- `payment.amount` = `order.totalAmount` — server-side only; client must not provide
  or influence the payment amount
- Duplicate-payment guard checks both `SUCCESS` and `PROCESSING` statuses
- `paymentReference` is a server-generated UUID — unique per payment
- Do NOT set `PaymentStatus = REFUNDED` in MVP (refund processing is Phase 2)
- `userId` always from `AuthenticatedUser` principal — never from JWT claims
- `operationId` names: `initiatePayment`, `getPayment`

**Transaction Boundaries:**
- `initiatePayment`: single `@Transactional` — payment creation + simulation result +
  `payment.status` update + `order.status` update; all atomic

**Authorization:** All endpoints require JWT. Ownership verified via order's `userId`.

**Unit Tests:**
- `PaymentServiceTest`:
  - successful payment: `PENDING_PAYMENT → PAID`, payment `SUCCESS`, `paidAt` set,
    amount equals `order.totalAmount`
  - order already `PAID` (or non-`PENDING_PAYMENT` status) → throws
    `BusinessRuleViolationException`
  - existing `SUCCESS` payment → throws `DuplicatePaymentException`
  - `amount` in created payment matches `order.totalAmount` (not any client value)
  - get payment found with correct owner, get payment not owned → throws
    `ResourceNotFoundException`

**Integration Tests:**
- `PaymentControllerIT`:
  - checkout → `POST /payments` → 201 `PaymentResponse`:
    `status = SUCCESS`, `paidAt` not null, `amount = order.totalAmount`
  - verify `order.status = PAID` after successful payment (not `CONFIRMED`)
  - second `POST /payments` for same order → 409
  - `POST /payments` for another user's order → 404

**Dependencies:** Task 3 (Payment, Order entities), Task 4 (PaymentRepository,
OrderRepository), Task 5 (ErrorResponse, exceptions), Task 6 (JWT auth),
Task 11 (order creation flow)

**Status:** [ ] pending

---

## Task 13 — Springdoc OpenAPI Configuration

**Intent:** Expose the OpenAPI documentation via Springdoc and configure it to match
the design contract. The running application's generated docs must be consistent with
`docs/03-openapi-specification.yaml`.

**Expected Outcomes:**
- `GET /api/v3/api-docs` returns valid OpenAPI JSON
- `GET /api/swagger-ui.html` serves the Swagger UI
- The `bearerAuth` security scheme is registered
- All MVP endpoints appear in the generated spec with correct operationIds and tags
- Phase-2 endpoints do not appear (they have no controllers)

**Files to Create:**

- `OpenApiConfig.java` in `com.ebookstore.config`:
  - `@Bean OpenAPI customOpenAPI()` configures:
    - `info.title = "E-Bookstore API"`, `version = "1.0.0"`
    - `securityScheme` named `bearerAuth` of type HTTP bearer with JWT bearerFormat
    - Global `security` requirement `bearerAuth: []`
  - Controllers for public endpoints will override security via
    `@SecurityRequirements({})` or the Springdoc `security = {}` annotation on
    specific operations

**Important Constraints:**
- Do not manually duplicate the entire OpenAPI YAML into Springdoc annotations —
  let Springdoc generate from controllers and add `@Operation(operationId = "...")` to
  match the contract operationIds
- The `operationId` on each controller method must exactly match the OpenAPI contract

**Dependencies:** All controller tasks (Tasks 7–12)

**Status:** [ ] pending

---

## Task 14 — Integration Tests

**Intent:** Write integration tests that exercise the full stack (Controller → Service →
Repository → Test Database) for all critical flows. These tests verify the system works
end-to-end, not just in unit isolation.

**Expected Outcomes:**
- `mvn test` passes all unit tests and integration tests
- The complete purchase flow is covered by at least one end-to-end integration test
- Every scenario listed in the test matrix below is implemented
- No tests exist for Phase-2 features (gift points, coupons, shipments, returns,
  refunds)

**Test Infrastructure:**

Use `@SpringBootTest(webEnvironment = RANDOM_PORT)`. The test database must be a real
PostgreSQL instance running the Flyway-migrated schema — no mocking of the database,
no H2, no in-memory substitute.

**Preferred approach — Testcontainers PostgreSQL:**
- Declare a single shared `@Container` `PostgreSQLContainer` in a base test class or
  `@TestConfiguration`; annotate with `@Testcontainers`
- Pass the container's JDBC URL, username, and password to Spring via
  `DynamicPropertySource` (`@DynamicPropertySource` static method)
- Flyway runs its migrations against the container automatically on startup
- The container starts once per test run (or per class, depending on lifecycle
  annotation) — no external database needed for CI

**Fallback:** dedicated test PostgreSQL configured in `application-test.yml` with
`DB_URL` / `DB_USERNAME` / `DB_PASSWORD` pointing to a local test database; same
Flyway migration applies.

**State cleanup between tests:**
- Do NOT use `@Transactional` on integration test methods to rely on rollback —
  with `RANDOM_PORT` the HTTP request executes in a separate thread/transaction from
  the test, so rollback does not clean up server-side state
- Instead, use deterministic cleanup before each test: a `@BeforeEach` method that
  truncates or deletes test data via `JdbcTemplate` or repository delete calls, or
  relies on Testcontainers container restart between classes (if configured that way)
- Each integration test class is responsible for setting up its own fixtures and
  tearing them down

**Clock injection for cancellation tests:**
- `OrderService` (and any other service that checks `now()`) accepts an injected
  `java.time.Clock` bean
- `application.yml` / `config` provides `Clock.systemUTC()` as the default bean
- A test configuration class (`ClockTestConfig`) provides a `Clock` fixed to a chosen
  instant, allowing cancellation deadline tests without `Thread.sleep()`
- Do NOT use `Thread.sleep()` to simulate time passing

**Test Classes and Required Scenarios:**

- `AuthControllerIT`:
  - `POST /auth/register` → 201; duplicate email → 409
  - `POST /auth/login` → 200 with `accessToken`; wrong credentials → 401
  - `GET /users/me` with token → 200; without token → 401
  - `POST /auth/logout` → 204

- `CatalogControllerIT`:
  - `GET /categories` without token → 200
  - `GET /brands` without token → 200
  - `GET /products` — default pagination, `q` filter, `categoryId`, `brandId`,
    `minPrice`, `maxPrice`, `availableOnly` filters
  - Pagination envelope shape: `{ content, page: { page, size, totalElements, totalPages } }`
  - `GET /products/{id}` found; unknown id → 404
  - `GET /products/{id}/related` — same-category results; brand supplement

- `AddressControllerIT`:
  - Full CRUD with valid token
  - Another user's address → 404 (not 403)
  - Invalid request body → 400 with `fieldErrors`

- `CartControllerIT`:
  - Add new item → 201 `CartResponse`
  - Add same product again → quantity merged (no duplicate row)
  - Add product with `stockQuantity = 0` → 409
  - Update quantity; remove item

- `OrderControllerIT`:
  - Full checkout: register → add to cart → `POST /orders` → 201 `OrderResponse`
    with `status = PENDING_PAYMENT`; verify no `Payment` record exists
  - Empty cart checkout → 409
  - **Live price snapshot:** change `products.price` after cart add; checkout must
    use updated `products.price`, not stale `cart_items.unit_price`; verify
    `order_items.unit_price` equals post-change `products.price`
  - **Address snapshot:** update address after order; order must still carry original
    7-field snapshot; not a FK lookup
  - **Stock decrement:** verify `products.stock_quantity` decremented after checkout
  - **Order-item snapshot:** verify `order_items.product_title` = product title at
    checkout time
  - `GET /orders` sorted `placed_at DESC` — verify ordering with multiple orders;
    client sort parameter ignored
  - `GET /orders?status=PENDING_PAYMENT` — filtered result
  - `GET /orders/{id}` for another user's order → 404
  - **Buy Again:** `POST /orders/{id}/buy-again` → products added to cart; verify
    `CartItem.unitPrice` = current `products.price` (not historical `order_items.unit_price`)
  - **Buy Again partial unavailability:** one product out of stock → still added for
    available ones
  - **Cancellation within deadline (`PENDING_PAYMENT`):** inject fixed `Clock` set
    before deadline; `POST /orders/{id}/cancel` → 200 `CANCELLED`; verify stock restored
  - **Cancellation within deadline (`PAID`):** same but order in `PAID` state;
    `payment.status` remains `SUCCESS`; stock restored
  - **Cancellation after deadline:** inject fixed `Clock` set after
    `cancellationDeadline`; cancel → 409

- `PaymentControllerIT`:
  - Checkout → `POST /payments` → 201 `PaymentResponse`:
    `status = SUCCESS`, `paidAt` not null
  - Verify `payment.amount` = `order.totalAmount` (not client-provided)
  - Verify `order.status = PAID` after payment (not `CONFIRMED`)
  - Second `POST /payments` for same order → 409
  - `POST /payments` for another user's order → 404

**Testing Utilities in `src/test/java/com/ebookstore/util`:**

- `TestDataBuilder` — factory helpers for creating test `User`, `Product`, `Category`,
  `Brand`, `Cart`, `Order` instances with sensible defaults
- `JwtTestHelper` — generates valid JWT tokens signed with the test secret for use in
  `Authorization: Bearer` headers
- `ClockTestConfig` — `@TestConfiguration` that registers a fixed `Clock` bean;
  injected into services that need time-based logic; controls the 48-hour deadline
  without `Thread.sleep()`
- `AbstractIntegrationTest` (optional base class) — declares the shared
  `@Container PostgreSQLContainer`, the `@DynamicPropertySource` wiring, and a
  `@BeforeEach` cleanup hook via `JdbcTemplate`; all `*IT` classes may extend it

**Important Constraints:**
- Test database must be a real PostgreSQL instance with Flyway-migrated schema —
  no H2, no mocked `DataSource`, no mocked database layer
- Testcontainers is the preferred database strategy; fallback is a dedicated local
  test DB via `application-test.yml`
- Do NOT use `@Transactional` on `@SpringBootTest(RANDOM_PORT)` integration test
  methods to achieve rollback — the HTTP layer runs in a separate transaction; rollback
  does not clean server-side state; use explicit `@BeforeEach` cleanup instead
- Never use `Thread.sleep()` for time simulation — use injected `Clock`
- Do not create tests for Phase-2 features
- Integration tests must exercise the full HTTP → Controller → Service → Repository
  → Database stack; do not mock the service layer in integration tests

**Dependencies:** All implementation tasks (Tasks 1–13)

**Status:** [ ] pending

---

## Package and File Map Summary

```
src/main/java/com/ebookstore/
├── EbookstoreApplication.java
├── auth/
│   ├── controller/AuthController.java
│   ├── dto/RegisterRequest.java, LoginRequest.java, LoginResponse.java
│   └── service/AuthService.java
├── user/
│   ├── controller/UserController.java
│   ├── dto/UserResponse.java
│   ├── entity/User.java
│   ├── repository/UserRepository.java
│   └── service/UserService.java
├── catalog/
│   ├── controller/CategoryController.java, BrandController.java, ProductController.java
│   ├── dto/CategorySummary.java, BrandSummary.java, ProductSummary.java,
│   │       ProductResponse.java, DeliveryEstimate.java
│   ├── entity/Category.java, Brand.java, Product.java
│   ├── repository/CategoryRepository.java, BrandRepository.java,
│   │             ProductRepository.java, ProductSpecification.java
│   └── service/CategoryService.java, BrandService.java, ProductService.java
├── address/
│   ├── controller/AddressController.java
│   ├── dto/AddressRequest.java, AddressResponse.java
│   ├── entity/Address.java
│   ├── repository/AddressRepository.java
│   └── service/AddressService.java
├── cart/
│   ├── controller/CartController.java
│   ├── dto/AddCartItemRequest.java, UpdateCartItemRequest.java,
│   │       CartItemResponse.java, CartResponse.java
│   ├── entity/Cart.java, CartItem.java
│   ├── repository/CartRepository.java, CartItemRepository.java
│   └── service/CartService.java
├── order/
│   ├── controller/OrderController.java
│   ├── dto/CreateOrderRequest.java, OrderItemResponse.java,
│   │       ShippingAddressSnapshot.java, OrderResponse.java
│   ├── entity/Order.java, OrderItem.java
│   ├── repository/OrderRepository.java, OrderItemRepository.java
│   └── service/OrderService.java
├── payment/
│   ├── controller/PaymentController.java
│   ├── dto/CreatePaymentRequest.java, PaymentResponse.java
│   ├── entity/Payment.java
│   ├── repository/PaymentRepository.java
│   ├── service/PaymentProcessor.java
│   ├── service/PaymentService.java
│   └── service/SimulatedPaymentProcessor.java
├── security/
│   ├── AuthenticatedUser.java          # UserDetails impl with Long id, email, role
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   ├── UserDetailsServiceImpl.java
│   ├── AuthEntryPoint.java
│   └── AccessDeniedHandlerImpl.java
├── common/
│   ├── domain/UserRole.java, UserStatus.java, CartStatus.java, OrderStatus.java,
│   │            PaymentMethod.java, PaymentStatus.java
│   │            (6 MVP enums only — no ShipmentStatus, GiftPointTransactionType,
│   │             ReturnStatus)
│   ├── dto/ErrorResponse.java, PageMetadata.java, PagedResponse.java
│   ├── entity/BaseCreatedEntity.java      # @MappedSuperclass; createdAt only (@PrePersist)
│   ├── entity/BaseEntity.java             # extends BaseCreatedEntity; adds updatedAt (@PrePersist + @PreUpdate)
│   └── exception/GlobalExceptionHandler.java,
│                 ResourceNotFoundException.java,
│                 InsufficientStockException.java,
│                 DuplicatePaymentException.java,
│                 OrderCancellationNotAllowedException.java,
│                 BusinessRuleViolationException.java,
│                 UnauthorizedResourceAccessException.java,
│                 InvalidRequestException.java
└── config/
    ├── SecurityConfig.java
    ├── OpenApiConfig.java
    └── ClockConfig.java               # registers Clock.systemUTC() bean for production

src/main/resources/
├── application.yml
├── application-local.yml
└── db/migration/
    ├── V1__create_users.sql
    ├── V2__create_addresses.sql
    ├── V3__create_categories.sql
    ├── V4__create_brands.sql
    ├── V5__create_products.sql
    ├── V6__create_carts.sql
    ├── V7__create_cart_items.sql
    ├── V8__create_orders.sql
    ├── V9__create_order_items.sql
    ├── V10__create_payments.sql
    └── V11__create_indexes.sql

src/test/java/com/ebookstore/
├── auth/AuthControllerIT.java, AuthServiceTest.java
├── catalog/CatalogControllerIT.java, ProductServiceTest.java,
│          ProductSpecificationTest.java
├── address/AddressControllerIT.java, AddressServiceTest.java
├── cart/CartControllerIT.java, CartServiceTest.java
├── order/OrderControllerIT.java, OrderServiceTest.java
├── payment/PaymentControllerIT.java, PaymentServiceTest.java
├── security/JwtTokenProviderTest.java, UserDetailsServiceImplTest.java
├── common/GlobalExceptionHandlerTest.java
└── util/TestDataBuilder.java, JwtTestHelper.java, ClockTestConfig.java,
         AbstractIntegrationTest.java
```

---

## Git Milestone Strategy

| Commit | Scope |
|---|---|
| `docs: add requirements, data model, OpenAPI specification` | (already done) |
| `chore: initialize IBM Bob project context and AGENTS.md` | (already done) |
| `chore: initialize Spring Boot Maven project scaffold` | Task 1 |
| `feat: add Flyway database migrations (11 scripts)` | Task 2 |
| `feat: add JPA entities and 6 MVP enums` | Task 3 |
| `feat: add Spring Data JPA repositories` | Task 4 |
| `feat: add common exception and error-response infrastructure` | Task 5 |
| `feat: add JWT security infrastructure` | Task 6 |
| `feat: implement authentication and user profile` | Task 7 |
| `feat: implement public catalog with search and filtering` | Task 8 |
| `feat: implement address management` | Task 9 |
| `feat: implement cart management` | Task 10 |
| `feat: implement checkout and order management` | Task 11 |
| `feat: implement simulated payment processor` | Task 12 |
| `feat: configure Springdoc OpenAPI` | Task 13 |
| `test: add integration test suite` | Task 14 |
| `refactor: review and consistency pass` | Post all tasks |

Each commit should include only the files changed by that task. Do not mix concerns
across commits.

---

## Local PostgreSQL Setup

Before running the application locally:

```bash
# Start PostgreSQL (Docker or local install)
docker run --name ebookstore-db \
  -e POSTGRES_DB=ebookstore \
  -e POSTGRES_USER=ebookstore \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  -d postgres:16

# Set environment variables (in shell or .env — never commit .env with real values)
export DB_URL=jdbc:postgresql://localhost:5432/ebookstore
export DB_USERNAME=ebookstore
export DB_PASSWORD=password
export JWT_SECRET=your-local-dev-secret-at-least-32-characters
export JWT_EXPIRATION_MS=86400000

# Run
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Startup Verification Checklist:**
- [ ] Application starts without errors
- [ ] Flyway reports all 11 migrations applied (V1–V11, no Phase-2 tables)
- [ ] `GET http://localhost:8080/api/actuator/health` returns `{"status":"UP"}`
- [ ] `GET http://localhost:8080/api/swagger-ui.html` loads the Swagger UI
- [ ] `POST http://localhost:8080/api/auth/register` with valid body returns `201`
- [ ] `POST http://localhost:8080/api/auth/login` with registered credentials returns
  `200` with `accessToken`
- [ ] `GET http://localhost:8080/api/products` returns `200` with empty
  `PagedProductResponse`
- [ ] `GET http://localhost:8080/api/users/me` without token returns `401 ErrorResponse`

---

## Cross-Cutting Constraints (Apply to Every Task)

- **No `double` or `float` for money.** Use `BigDecimal` in Java, `NUMERIC(12,2)` in
  PostgreSQL. OpenAPI `format: double` does not override this.
- **No Hibernate DDL.** `ddl-auto: validate`. Flyway owns the schema; Hibernate must
  only validate — never create, alter, or drop tables.
- **BIGSERIAL PKs.** All primary key columns use `BIGSERIAL`; all Java entity IDs use
  `Long`.
- **EnumType.STRING.** All JPA enum fields use `@Enumerated(EnumType.STRING)` — never
  `ORDINAL`.
- **Entity timestamp inheritance.** `OrderItem` extends `BaseCreatedEntity` (only
  `created_at`). All other 9 MVP entities extend `BaseEntity` (both `created_at` and
  `updated_at`). Do NOT map an `updated_at` column on `OrderItem` — the `order_items`
  table has no such column and `ddl-auto: validate` will fail if one is declared.
- **snake_case columns.** All `@Column(name = "...")` annotations must exactly match
  the `snake_case` names defined by the Flyway migration.
- **No entity exposure.** Entities are internal. DTOs are the only objects returned
  through REST APIs. Do NOT use `@JsonIgnore` as a substitute for a proper DTO.
- **No hardcoded secrets.** Environment variables only. Never commit `.env` files
  containing real credentials.
- **No Phase-2 code.** Do not create any controller, service, repository, entity,
  migration, test, or enum for gift points, coupons, shipments, returns, or refunds.
  Do not create packages `loyalty`, `coupon`, `shipping`, `returns`, or `refund`.
  Phase-2 OpenAPI paths exist for documentation only.
- **Ownership enforcement.** Every authenticated resource access verifies ownership
  using a repository query (`findByIdAndUserId` pattern). `userId` is always extracted
  from `AuthenticatedUser` in the `SecurityContextHolder` — never from request body,
  query parameters, or JWT claims. Controllers cast `authentication.getPrincipal()` to
  `AuthenticatedUser` and call `.getId()`. Return `404` (not `403`) when a resource is
  not found or not owned, to avoid revealing existence.
- **No `userId` in JWT claims.** The JWT token contains only `sub` (email), `iat`, and
  `exp`. The database user ID is resolved by `UserDetailsServiceImpl` when the token is
  validated on each request and stored in the `AuthenticatedUser` principal. Never add
  `userId` as a custom JWT claim.
- **Server-side totals.** Order amounts are always computed from live `products.price`
  at checkout time — never from `cart_items.unit_price` or client-provided values.
- **Cart price is display-only.** `cart_items.unit_price` is a snapshot set at
  add-to-cart time for display purposes only. Checkout always re-fetches `products.price`.
  `order_items.unit_price` is the authoritative historical purchase snapshot.
- **Cart is permanently reusable.** The Cart row created at registration is the user's
  only Cart for the lifetime of the account. After checkout: all `CartItem` rows are
  deleted, the Cart's status is reset to `ACTIVE`, and the Cart row is kept. A second
  Cart must never be created. `CHECKED_OUT` is a valid enum value but must not be the
  persistent resting state of the cart after checkout completes.
- **`gift_points_used` always 0 in MVP.** The column exists in the schema for Phase-2
  compatibility. Always persist `0`. Never validate, calculate, or expose it.
- **`discount_amount` always ZERO in MVP.** No coupon field in `CreateOrderRequest`.
  Never validate or persist coupon data.
- **Buy Again uses current prices.** Always re-fetch `products.price`. Never use
  `order_items.unit_price` as the new cart item price.
- **Order history sort is fixed.** `GET /orders` always sorts `placed_at DESC`.
  No client-controlled sort parameter.
- **Recommendations are stateless.** Up to 4 active, in-stock products from
  purchase-history categories, excluding current cart items. No recommendation table,
  no ML, no popularity engine, no global fallback. No Elasticsearch or Redis.
- **Clock injection for time-dependent logic.** `OrderService.cancelOrder` evaluates
  `now() <= cancellationDeadline` via an injected `java.time.Clock` bean. Production
  uses `Clock.systemUTC()` (registered in `ClockConfig`). Tests inject a fixed
  `Clock` via `ClockTestConfig`. Never use `Thread.sleep()` to simulate time passing.
- **Real PostgreSQL for integration tests.** Integration tests use Testcontainers
  (preferred) or a dedicated local test DB. No H2, no in-memory substitutes, no
  mocked `DataSource`. The Flyway-migrated schema must be the exact schema under test.
- **`operationId` must match.** Controller method OpenAPI annotations must produce the
  exact operationIds from `docs/03-openapi-specification.yaml`.
- **`ErrorResponse` shape must match.** All errors return the documented
  `{timestamp, status, code, message, path, fieldErrors?}` structure.
- **Pagination envelope must match.** `{content: [...], page: {page, size,
  totalElements, totalPages}}` — not Spring's default `Page` serialization.
