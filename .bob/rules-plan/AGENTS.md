# Project Architecture Rules (Non-Obvious Only)

## Strict Document-Driven Design

All architecture decisions must flow from the three docs in order:
`01-requirements-specification.md` → `02-data-model-design.md` → `03-openapi-specification.yaml`

Do not propose a data model or API shape that contradicts these docs; improve or extend them only when the capstone explicitly leaves something open.

## Non-Obvious Architectural Constraints

- **Address is NOT a relationship on Order:** The order stores a flattened 7-column address snapshot. Planning address versioning, polymorphic address types, or separate address FKs on orders would violate the established design.
- **One payment per order:** `payments.order_id` has a UNIQUE constraint — the model does not support multiple payment attempts or split payments without a schema change.
- **Checkout is a single atomic operation:** All cart-to-order conversion (validate, snapshot, create order/items/payment, decrement stock) must occur within one transaction. Planning a multi-step checkout API would break this atomicity requirement.
- **Recommendations are stateless:** No `recommendations` or `product_relations` table. Related products come from same-category/brand queries. Planning a recommendation service that persists state requires an explicit schema change not in scope.
- **Catalog endpoints are intentionally unauthenticated:** `/products`, `/categories`, `/brands` and sub-paths have `security: []`. Any authentication middleware must skip these paths.

## MVP vs Phase 2 Boundary

Planning must respect this boundary — do not mix Phase 2 tables into MVP milestones:

| MVP | Phase 2 |
|---|---|
| users, addresses, categories, brands, products, carts, cart_items, orders, order_items, payments | shipments, gift_point_accounts, gift_point_transactions, coupons, return_requests, refunds |

## Pagination Contract

Any paginated endpoint must return `{ content, page: { page, size, totalElements, totalPages } }`. Do not plan a pagination design that uses Spring's default `Page` serialization — the OpenAPI schema defines the exact wrapper shape.

## Open Decisions (Require Explicit Resolution Before Planning)

Do not propose implementation plans for: JWT mechanism, roles/permissions matrix, real payment gateway, shipping rate formula, coupon stacking rules, gift-point earning rate, or partial return line items — these are unresolved TBDs per the requirements doc §10.
