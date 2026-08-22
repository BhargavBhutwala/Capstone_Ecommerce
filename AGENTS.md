# AGENTS.md

## Project Purpose

Build the backend for the IBM AI Specialist Capstone E-Bookstore platform.

The backend implementation must be derived from and remain consistent with the project's three authoritative design artifacts:

* `docs/01-requirements-specification.md`
* `docs/02-data-model-design.md`
* `docs/03-openapi-specification.yaml`

The implementation must not redefine or silently contradict the documented requirements, data model, or API contract.

The project should be developed as a maintainable Spring Boot modular monolith with clear domain boundaries, strong validation, secure authentication, transactional business operations, and comprehensive automated testing.

---

## Project Status

The repository begins as a design-first project.

Current design artifacts:

* Requirements specification
* Data model design
* OpenAPI specification

The Spring Boot application is generated and implemented incrementally after design consistency has been verified.

Do not assume that undocumented functionality is required.

Do not generate Phase-2 functionality while implementing the MVP unless explicitly requested.

---

## Document Dependency Order

The project follows this strict dependency chain:

```text
01-requirements-specification.md
            ↓
02-data-model-design.md
            ↓
03-openapi-specification.yaml
            ↓
Spring Boot implementation
```

### Rules

1. Requirements define what the system must do.
2. The data model defines how required business data is persisted.
3. OpenAPI defines the REST API contract.
4. Implementation must conform to the existing artifacts.
5. Code must not silently redefine business requirements.
6. If implementation reveals a genuine contradiction or missing requirement, stop and report it before changing the design.

The implementation is **not** the source of truth for requirements or API behavior.

---

# Technology Stack

## Backend

* Java 21
* Spring Boot 3.x
* Maven
* Spring Web
* Spring Data JPA
* Spring Security
* Jakarta Bean Validation

## Database

* PostgreSQL
* Flyway database migrations

## Security

* Spring Security
* Stateless JWT Bearer authentication
* BCrypt password hashing

## API Documentation

* OpenAPI 3.0.3
* Springdoc OpenAPI

## Testing

* JUnit 5
* Mockito
* Spring Boot testing support
* Integration/API tests where appropriate

## Core Java Conventions

* Use `BigDecimal` for monetary values.
* Do not use `double` or `float` for currency.
* Use appropriate Java time types consistently.
* Prefer immutable DTOs where practical.
* Prefer explicit, readable code over unnecessary abstractions.

---

# Architecture

Use a modular-monolith architecture with domain-oriented packages.

Recommended structure:

```text
com.yourcompany.ebookstore
├── auth
├── user
├── catalog
├── cart
├── address
├── order
├── payment
├── shipping
├── loyalty
├── coupon
├── returns
├── security
├── config
└── common
```

Each domain should organize its components by responsibility, for example:

```text
order/
├── controller/
├── service/
├── repository/
├── entity/
└── dto/
```

## Layering

Use the following application dependency flow:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
JPA Entity
    ↓
PostgreSQL
```

### Controller

Responsible for:

* HTTP request/response handling
* request DTO validation
* authentication/authorization boundaries
* mapping service results to response DTOs

Controllers must not contain business logic.

### Service

Responsible for:

* business rules
* transaction boundaries
* orchestration of multiple repositories
* authorization checks that depend on domain state
* calculations and state transitions

Business rules must not be moved into repository queries merely to simplify service code.

### Repository

Responsible for:

* persistence concerns
* data retrieval
* persistence-specific queries

Repositories should not contain business workflows.

### Entity

JPA entities represent persistent state.

Do not expose JPA entities directly through REST APIs.

Use explicit request and response DTOs.

Avoid unnecessary bidirectional relationships and relationship graphs that cause serialization or performance problems.

Do not implement entity `equals()` / `hashCode()` in a way that traverses large or cyclic relationships.

---

# Database Rules

Use PostgreSQL as the persistence database.

Use Flyway migrations for schema creation and schema evolution.

Hibernate must validate the schema rather than silently changing it.

Recommended JPA configuration:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Do not use:

```text
ddl-auto=create
ddl-auto=create-drop
ddl-auto=update
```

as the project database migration strategy.

## Naming

### Tables

* `snake_case`
* plural nouns

Examples:

```text
users
cart_items
order_items
gift_point_transactions
```

### Columns

* `snake_case`
* descriptive names

Examples:

```text
created_at
updated_at
shipping_line1
unit_price
cancellation_deadline
```

Boolean columns should use the `is_` prefix where defined by the data model:

```text
is_default
is_active
```

### Keys

Primary keys:

```text
id
```

Foreign keys:

```text
<entity>_id
```

Primary keys use `BIGSERIAL` as defined by the data model.

---

# Monetary Data

Money must be represented accurately.

## Java

Use:

```java
BigDecimal
```

Never use:

```java
double
float
```

for monetary values.

## PostgreSQL

Use:

```text
NUMERIC(12,2)
```

for monetary columns.

Examples include:

* product price
* unit price
* order subtotal
* shipping amount
* order total
* payment amount

Server-side calculation must be authoritative.

Never trust client-provided totals or prices.

---

# Critical Data Model Rules

These rules are mandatory because they protect historical correctness and enforce documented business behavior.

## Order Item Snapshots

`order_items` must snapshot:

* `product_title`
* `unit_price`

at purchase time.

Historical orders must **never** depend on the current `products` row for historical title or price.

For example:

```text
Product today:
Title = "Java Programming"
Price = 999.00

Order placed:
Title snapshot = "Java Programming"
Unit price snapshot = 799.00
```

If the product later changes to:

```text
Title = "Java Programming 2nd Edition"
Price = 1099.00
```

the original order must still display:

```text
Java Programming
799.00
```

---

## Order Shipping Address Snapshot

Orders must preserve the complete shipping address as seven flat columns on the `orders` row.

The order must not depend on a foreign-key relationship to the user's current `addresses` record for historical display.

The snapshot includes the documented fields:

```text
shipping_name
shipping_line1
shipping_line2
shipping_city
shipping_state
shipping_postal_code
shipping_country
```

Do not replace the documented order snapshot with:

```text
orders.address_id
```

unless the design documents are explicitly changed first.

---

## Cancellation Deadline

The order contains:

```text
cancellation_deadline
```

This value represents:

```text
placed_at + 48 hours
```

The deadline must be persisted on the order.

Cancellation authorization must be checked in the service layer against this persisted deadline.

Do not calculate cancellation eligibility from unrelated timestamps or hard-code `48` in multiple locations.

---

## Cart Item Uniqueness

`cart_items` must enforce:

```text
UNIQUE(cart_id, product_id)
```

The same product must not appear as multiple cart rows for the same cart.

When a product already exists in the cart:

```text
existing quantity
        +
requested quantity
        =
new quantity
```

The service layer must update the existing cart item instead of creating a duplicate row.

---

## Payment Relationship

`payments.order_id` is unique.

There is a 1-to-1 relationship between an order and its payment record in the MVP model.

Do not create multiple unrelated payment rows for the same order unless the documented design is explicitly changed.

---

# MVP vs Phase 2

The MVP must be implemented first.

## MVP Tables

```text
users
addresses
categories
brands
products
carts
cart_items
orders
order_items
payments
```

The core customer journey is:

```text
Authentication
    ↓
Catalog
    ↓
Product
    ↓
Cart
    ↓
Address
    ↓
Checkout
    ↓
Order
    ↓
Payment
    ↓
Confirmation
    ↓
Order History
```

## Phase-2 Tables

These must not be implemented during the initial MVP unless explicitly requested:

```text
shipments
gift_point_accounts
gift_point_transactions
coupons
return_requests
refunds
```

## Intentionally Deferred Features

The following do not require dedicated database tables in the current design:

```text
recommendations
product_relations
wishlists
reviews
```

Where applicable, recommendation-related behavior should be implemented using application logic based on existing data such as:

* category
* brand
* order history

Do not invent recommendation tables merely because they might be useful later.

---

# OpenAPI Contract

The authoritative API contract is:

```text
docs/03-openapi-specification.yaml
```

Implementation must conform to it.

Do not silently:

* rename endpoints
* change HTTP methods
* change request/response structures
* change enum values
* add undocumented endpoints
* remove documented endpoints
* change status codes

If an API change appears necessary, explain the reason and resolve the design change before implementation.

---

# OpenAPI Conventions

## JSON Naming

JSON request and response properties use `camelCase`.

Examples:

```text
orderNumber
addressLine1
unitPrice
createdAt
```

Database columns remain `snake_case`.

Therefore:

```text
JSON: orderNumber
DB:   order_number
```

---

## IDs

All documented IDs use:

```text
integer
format: int64
minimum: 1
```

Java persistence identifiers should use an appropriate `Long` type.

---

## Pagination

Paginated responses must use the documented custom envelope:

```json
{
  "content": [],
  "page": {
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

Do not expose Spring Data's default `Page` JSON representation directly.

The response DTO must match the OpenAPI contract.

---

## Sorting

Use the documented Spring-style sort parameter:

```text
title,asc
```

Examples:

```text
sort=title,asc
sort=price,desc
```

Do not invent a different sorting syntax.

---

## Error Response

All API errors must conform to the documented:

```text
ErrorResponse
```

shape:

```json
{
  "timestamp": "...",
  "status": 400,
  "code": "...",
  "message": "...",
  "path": "..."
}
```

Do not expose stack traces or internal implementation details to API consumers.

---

# Exact Enum Values

These enum values must be preserved exactly between the OpenAPI contract, Java implementation, and API JSON representation.

| Enum                       | Values                                                                                                                  |
| -------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| `UserRole`                 | `CUSTOMER`, `ADMIN`                                                                                                     |
| `UserStatus`               | `ACTIVE`, `INACTIVE`, `LOCKED`                                                                                          |
| `CartStatus`               | `ACTIVE`, `CHECKED_OUT`, `ABANDONED`                                                                                    |
| `OrderStatus`              | `PENDING_PAYMENT`, `PAID`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `RETURN_REQUESTED`, `RETURNED`, `REFUNDED` |
| `PaymentMethod`            | `CREDIT_CARD`, `DEBIT_CARD`                                                                                             |
| `PaymentStatus`            | `INITIATED`, `PROCESSING`, `SUCCESS`, `FAILED`, `REFUNDED`                                                              |
| `ShipmentStatus`           | `PENDING`, `SHIPPED`, `IN_TRANSIT`, `DELIVERED`, `RETURNED`                                                             |
| `GiftPointTransactionType` | `EARNED`, `REDEEMED`, `EXPIRED`, `ADJUSTED`                                                                             |
| `ReturnStatus`             | `REQUESTED`, `APPROVED`, `REJECTED`, `COMPLETED`                                                                        |

Do not change the capitalization, spelling, or underscore usage.

---

# Security

## Authentication

Use:

```text
Stateless JWT Bearer authentication
```

as the project authentication mechanism.

The OpenAPI contract defines Bearer JWT security as the global default.

Catalog/product/brand/category endpoints that explicitly define:

```yaml
security: []
```

must remain accessible without authentication where specified by the API contract.

## Passwords

Never store plain-text passwords.

Use BCrypt or an equivalent strong password hashing mechanism through Spring Security.

## Secrets

Never hardcode:

* database passwords
* JWT secrets
* API keys
* credentials
* private keys

Use environment variables or secure configuration.

Example:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION
```

Never commit `.env` files containing real secrets.

## Authorization

Do not assume permissions beyond the documented role model.

The roles currently defined are:

```text
CUSTOMER
ADMIN
```

Any additional permissions or role rules must be explicitly defined before implementation.

---

# Business Rules

## Order Totals

Order totals must be calculated server-side.

Never trust the following values when supplied by the client:

* item price
* subtotal
* shipping amount
* total

The server must retrieve authoritative product prices and calculate the order.

---

## Product Availability

Before checkout:

1. Validate that every product still exists and is active.
2. Revalidate stock.
3. Calculate authoritative prices.
4. Create the order from validated data.
5. Decrement inventory consistently within the checkout transaction.

Do not rely solely on the cart's previous state.

---

## Checkout Transaction

Checkout must execute within a single transaction.

Conceptually:

```text
Validate cart
    ↓
Validate stock
    ↓
Validate products/prices
    ↓
Snapshot shipping address
    ↓
Copy cart items into order items
    ↓
Create order
    ↓
Decrement stock
    ↓
Create payment record
    ↓
Update cart state
```

The entire workflow must preserve consistency.

Use an appropriate Spring transaction boundary, normally at the service layer.

---

## Order Cancellation

Cancellation is allowed only while:

```text
current time <= cancellation_deadline
```

and only when the order's current status permits cancellation.

The service layer must enforce this rule.

Do not rely on the controller alone.

---

## Order Ownership

Authenticated customers may access only resources they are authorized to access.

A customer must not be able to:

* view another customer's order
* modify another customer's cart
* access another customer's address
* manipulate another customer's payment

Authorization must be enforced server-side.

---

## Buy Again

Buy Again must use historical order information to reconstruct the requested products.

It must:

* verify products still exist
* verify products remain available
* verify current prices
* respect current inventory
* add valid products to the customer's active cart

Do not assume historical price or stock remains valid for the new purchase.

---

## Payment

The MVP uses a simulated payment processor because no real external payment gateway is required by the current design.

Payment processing should be abstracted behind a service/interface so a real provider can be introduced later without redesigning the order domain.

The simulated flow is:

```text
INITIATED
    ↓
PROCESSING
    ↓
SUCCESS / FAILED
```

Payment amount must equal the authoritative payable order amount.

Prevent duplicate successful payment processing for the same order.

---

# Resolved Implementation Decisions

The following decisions are resolved for the current implementation:

| Decision                | Resolution                                       |
| ----------------------- | ------------------------------------------------ |
| Java version            | Java 21                                          |
| Framework               | Spring Boot 3.x                                  |
| Database                | PostgreSQL                                       |
| Persistence             | Spring Data JPA                                  |
| Schema management       | Flyway                                           |
| Authentication          | Stateless JWT Bearer                             |
| Password hashing        | BCrypt                                           |
| Roles                   | `CUSTOMER`, `ADMIN`                              |
| Payment                 | Simulated payment processor for MVP              |
| Currency representation | `BigDecimal` / `NUMERIC(12,2)`                   |
| Architecture            | Domain-oriented modular monolith                 |
| API style               | REST / OpenAPI 3.0.3                             |
| DTO strategy            | Request/response DTOs, no direct entity exposure |
| Recommendations         | Stateless application logic for MVP              |
| Gift points             | Phase 2                                          |
| Coupons                 | Phase 2                                          |
| Returns/refunds         | Phase 2                                          |
| Shipment management     | Phase 2                                          |

---

# Open Decisions

Do not invent or silently assume behavior for unresolved requirements.

The following remain open unless explicitly resolved in the project documentation or implementation decision log:

* Exact role/permission matrix beyond `CUSTOMER` and `ADMIN`
* Shipping rate calculation rules
* Gift-point earning rate
* Gift-point expiry rules
* Coupon stacking rules
* Partial/line-level return requirements
* Detailed refund workflow
* Detailed shipment workflow

When one of these decisions becomes necessary for implementation:

1. Identify the affected requirement/API/data model.
2. Explain the decision required.
3. Record the decision before implementing dependent functionality.
4. Do not invent a permanent business rule merely to make code compile.

---

# Code Quality Rules

* Prefer small, focused classes.
* Keep methods reasonably cohesive.
* Avoid duplicated business logic.
* Avoid speculative abstractions.
* Do not introduce frameworks or libraries without a clear reason.
* Do not add microservices unless explicitly required.
* Do not add Kafka, Redis, MongoDB, Elasticsearch, Kubernetes, GraphQL, or other infrastructure without an explicit project requirement.
* Prefer existing project patterns over introducing competing approaches.
* Do not rewrite unrelated code while implementing a feature.
* Keep changes small and reviewable.
* Preserve backward compatibility with the documented API contract.

---

# Validation and Exception Handling

Use centralized exception handling with:

```java
@RestControllerAdvice
```

Create domain/application exceptions as appropriate.

At minimum, support meaningful handling for cases such as:

```text
ResourceNotFoundException
InvalidRequestException
UnauthorizedResourceAccessException
InsufficientStockException
BusinessRuleViolationException
OrderCancellationNotAllowedException
DuplicatePaymentException
```

HTTP status mappings must follow the OpenAPI contract.

Validation should occur at both appropriate boundaries:

* request validation
* service/business validation
* database constraints where appropriate

Do not rely exclusively on database constraints for business rules.

---

# Logging

Use useful application logging for important events and failures.

Appropriate examples:

```text
INFO
- user registration
- successful authentication event
- order creation
- payment processing outcome

WARN
- insufficient stock
- invalid state transition
- unauthorized resource access

ERROR
- unexpected application failures
- integration failures
```

Never log:

* passwords
* JWT tokens
* database credentials
* CVV
* full payment card numbers
* other secrets or sensitive credentials

---

# Testing Rules

Each major feature should include automated tests.

At minimum:

## Unit Tests

Test:

* business rules
* validation
* calculations
* state transitions
* service behavior
* exceptional paths

## Integration Tests

Test important interactions between:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

Especially for:

* authentication
* checkout
* stock changes
* order persistence
* payment persistence
* cancellation

## Important Scenarios

Tests should cover both successful and failing cases.

Examples:

```text
successful registration
duplicate email
successful login
invalid credentials
catalog pagination
catalog filtering
add product to cart
duplicate product cart merge
insufficient stock
empty-cart checkout
successful checkout
order total calculation
historical price snapshot
historical address snapshot
order ownership
cancellation within 48 hours
cancellation after 48 hours
duplicate payment
```

---

# AI Development Workflow

AI-assisted development must follow a controlled workflow.

Before implementing a major feature:

1. Explain the implementation plan.
2. Identify impacted files.
3. Identify relevant requirements.
4. Identify relevant OpenAPI endpoints.
5. Identify relevant data model entities.
6. Identify business rules.
7. Implement only the requested scope.
8. Run compilation and relevant tests.
9. Review the generated changes.
10. Report changes, tests, and unresolved issues.

## Development Modes

Use IBM Bob modes intentionally:

### Ask Mode

Use for:

* repository exploration
* requirements analysis
* consistency checks
* code explanations
* reviews that should not modify files

Do not modify files during design analysis unless explicitly requested.

### Plan Mode

Use for:

* architecture planning
* implementation planning
* identifying impacted files
* defining dependencies
* transaction and module design

Do not begin implementation until the plan has been reviewed.

### Agent Mode

Use for:

* creating files
* modifying code
* running tests
* implementing approved changes
* fixing identified issues

Prefer small, focused implementation tasks.

---

# Implementation Sequence

The backend should be built incrementally in this general order:

```text
1. Spring Boot foundation
        ↓
2. PostgreSQL configuration
        ↓
3. Flyway schema/migrations
        ↓
4. JPA entities
        ↓
5. Repositories
        ↓
6. DTOs
        ↓
7. Authentication/security
        ↓
8. Catalog
        ↓
9. Cart
        ↓
10. Addresses
        ↓
11. Orders
        ↓
12. Payments
        ↓
13. Order history / Buy Again
        ↓
14. Supporting MVP behavior
        ↓
15. Testing and review
        ↓
16. Phase-2 features
```

Do not generate the complete application in one step.

---

# Git and Change Management

Keep commits small and meaningful.

Recommended milestones include:

```text
docs: add requirements, data model and OpenAPI specification
chore: initialize IBM Bob project context
chore: define project rules
chore: initialize Spring Boot project
feat: add PostgreSQL and Flyway schema
feat: implement authentication
feat: implement catalog
feat: implement cart
feat: implement checkout and orders
feat: implement payment
test: add integration coverage
refactor: improve backend consistency
```

Do not mix unrelated changes into a feature commit.

Before committing:

```text
- review the diff
- run relevant tests
- confirm no secrets are included
- confirm no unrelated files changed
```

---

# Change Safety Rules

Before changing an existing design rule:

1. Identify which authoritative document is affected.
2. Determine whether the change is a requirement, data model, API, or implementation change.
3. Update the appropriate artifact first when necessary.
4. Re-check downstream artifacts for consistency.
5. Only then modify implementation code.

Never allow generated code to become the accidental source of truth.

---

# Definition of Done for a Feature

A major feature is not complete merely because its code compiles.

A feature is considered complete when:

```text
Requirements identified
        ↓
API contract verified
        ↓
Data model verified
        ↓
Implementation completed
        ↓
Validation implemented
        ↓
Authorization verified
        ↓
Tests added
        ↓
Tests pass
        ↓
API behavior reviewed
        ↓
Database behavior verified
        ↓
Git diff reviewed
```

---

# Final Principle

The project follows this rule:

```text
Design first.
Verify consistency.
Plan before coding.
Implement incrementally.
Test continuously.
Review AI-generated changes.
Keep requirements, data model, API, and implementation aligned.
```

IBM Bob is an implementation partner, not the source of truth.

Human review is required for:

* architecture
* business rules
* security decisions
* API changes
* database design changes
* unresolved requirements
* major dependency additions
