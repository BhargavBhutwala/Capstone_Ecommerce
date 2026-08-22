# Project Coding Rules (Non-Obvious Only)

## Before Writing Any Code

The OpenAPI spec (`docs/03-openapi-specification.yaml`) and data model (`docs/02-data-model-design.md`) are the authoritative contracts. Generated Spring Boot code **must match** these — do not redesign the schema or API shape.

## Critical Implementation Rules

- **Snapshot pattern is mandatory:** `order_items.product_title` and `order_items.unit_price` must be copied from the current product at checkout — never reference live product price for historical orders.
- **Shipping address is flat on `orders`:** Do NOT store a `shipping_address_id` FK on orders. Copy all 7 address fields directly as snapshot columns (`shipping_name`, `shipping_line1`, `shipping_line2`, `shipping_city`, `shipping_state`, `shipping_postal_code`, `shipping_country`).
- **Cancellation deadline:** Set `cancellation_deadline = placed_at + 48h` at order creation. The service layer must check this field — do not compute the deadline on the fly from `placed_at` elsewhere.
- **Cart uniqueness:** `UNIQUE(cart_id, product_id)` is a DB constraint. Adding an existing product to the cart must increment quantity, not insert a new row.
- **Monetary types:** Use `BigDecimal` in Java / `NUMERIC(12,2)` in PostgreSQL — never `double` or `float` for prices/amounts.
- **Checkout transaction boundary:** Wrap the full checkout flow (validate stock → snapshot address → copy items → create order → decrement stock → create payment) in a single `@Transactional` service method.
- **Business rule location:** 48-hour cancellation check, product availability check, and cart-uniqueness merge logic belong in `@Service` classes, NOT in repositories or database triggers.

## Pagination Response Shape

All paginated responses must use this exact structure (not Spring's `Page<T>` directly serialized):

```json
{ "content": [...], "page": { "page": 0, "size": 20, "totalElements": 100, "totalPages": 5 } }
```

## API Field Naming

DB columns are `snake_case`; JSON fields in request/response are `camelCase`. Use `@JsonProperty` or configure Jackson's `PropertyNamingStrategies.LOWER_CAMEL_CASE` — do not expose `snake_case` in the JSON API.

## Security

- Catalog endpoints (`/products`, `/categories`, `/brands`) are **public** (`security: []` in the spec) — do not require auth.
- All cart, order, address, payment, gift-point, and return endpoints require Bearer JWT auth.
- Use `NUMERIC(12,2)` for all monetary DB columns; never store raw payment credentials.

## MVP Scope Boundary

Do not implement `recommendations`, `product_relations`, `wishlists`, or `reviews` tables — these are intentionally deferred. Related-product logic uses same-category/brand product queries from existing tables.
