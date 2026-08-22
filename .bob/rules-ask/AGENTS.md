# Project Documentation Rules (Non-Obvious Only)

## Document Structure

- `docs/01-requirements-specification.md` — functional requirements derived from capstone wireframes; the **source of truth** for what to build.
- `docs/02-data-model-design.md` — PostgreSQL schema, constraints, entity definitions, and Spring Boot implementation guidance. Section 23 has explicit entity-name recommendations.
- `docs/03-openapi-specification.yaml` — the API contract (OpenAPI 3.0.3). All schema names, enum values, and response envelopes are defined here.

## Counterintuitive Aspects

- There is **no application code** in this repo yet — it is purely a design-phase project. Do not expect `src/`, `pom.xml`, or `build.gradle`.
- "Related products" and "recommendations" have **no dedicated table** — per the design doc they are computed from existing product/order data, not persisted.
- The `orders` table does **not store a FK to `addresses`** — it stores a flat snapshot. This is intentional and documented in §7.8 of the data model doc.
- `order_items` stores `product_title` and `unit_price` — they are snapshots, not derived from the live `products` table.
- The spec's `Sort` query parameter uses Spring's sort expression format (`title,asc`), not a separate `sortField`/`sortDir` pair.

## Scope Limits (Do Not Invent)

Sections 10 and 22 of the requirements and data model docs explicitly list open/TBD decisions. Do not treat any of these as resolved requirements:
- JWT implementation details, roles matrix, payment gateway, shipping rate rules, coupon logic, gift-point earning rate, partial return rules.
