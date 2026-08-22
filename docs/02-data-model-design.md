# E-Bookstore Capstone
## Data Model Design

**Document:** `02-data-model-design.md`  
**Project:** AI Specialist - Cloud FullStack Capstone  
**Database:** PostgreSQL  
**Primary backend domain:** E-Bookstore / E-Commerce

---

## 1. Purpose

This document defines the proposed relational data model for the E-Bookstore capstone project.

The capstone instructs the developer to analyze the supplied wireframes, identify entities such as **Product, User, Order**, and prepare the data model manually before generating the OpenAPI specification and AI-assisted Spring Boot backend. The specified database technology is **PostgreSQL**. [Source: Capstone instructions, Technology Components and Workflow; Step 1 and Step 3.] 

This document is therefore the database design baseline that will be used to create the OpenAPI contract and, subsequently, the Spring Boot implementation.

> **Design principle:** the data model is manually designed first. IBM BOB / AWS KIRO may assist with implementation later, but the generated code must conform to this model rather than redefine it.

---

## 2. Source Scope

The model is derived from the capabilities represented in the capstone wireframes and architecture, including:

- Registered/guest users and authentication
- Product catalogue
- Categories and brands
- Product selection and related products
- Shopping cart
- Order history and Buy Again
- Checkout and delivery address selection
- Payment and purchase confirmation
- Gift points
- Order cancellation within 48 hours
- Shipping and delivery information
- Returns/refunds
- Recommendations and related-product functionality

The capstone explicitly identifies PostgreSQL as the database and asks for manual data-model design from the wireframes. It then calls for OpenAPI generation followed by AI-assisted Spring Boot generation. fileciteturn0file0L169-L177 fileciteturn0file0L183-L202

---

## 3. Design Approach

### 3.1 Relational model

PostgreSQL will store normalized business entities and their relationships using primary keys, foreign keys, unique constraints, and validation constraints.

### 3.2 Core-first implementation

The model is divided into:

1. **Core MVP entities** required for the main purchase journey.
2. **Supporting entities** for capabilities that appear in the architecture but have less detailed wireframe information.

### 3.3 Historical transaction integrity

Transaction data must preserve what the customer purchased even if the current catalogue later changes. Therefore, `order_items` stores purchase-time values such as product title and unit price.

Likewise, the order stores a shipping-address snapshot rather than relying only on a mutable saved address.

### 3.4 Avoid premature over-engineering

Capabilities such as recommendations, related products, coupons, returns, and refunds are represented without introducing unnecessary tables where the capstone does not provide enough detail to justify them.

For example, recommendations can initially be computed by application logic from order history and product/category relationships rather than being persisted in a dedicated recommendation table.

---

# 4. Domain Overview

```text
USER
 ├── ADDRESS
 ├── CART
 │    └── CART_ITEM
 ├── ORDER
 │    ├── ORDER_ITEM
 │    ├── PAYMENT
 │    ├── SHIPMENT
 │    └── RETURN_REQUEST
 └── GIFT_POINT_ACCOUNT
      └── GIFT_POINT_TRANSACTION

CATALOG
 ├── CATEGORY
 ├── BRAND
 └── PRODUCT

PROMOTION
 └── COUPON

PAYMENT
 └── REFUND
```

---

# 5. Core MVP Entities

The following tables form the minimum database required to support the main customer purchase journey:

| Table | Purpose |
|---|---|
| `users` | Registered customer identity and authorization data |
| `addresses` | Customer delivery addresses |
| `categories` | Product categories |
| `brands` | Product brands |
| `products` | Books/products available in the catalogue |
| `carts` | Customer shopping carts |
| `cart_items` | Products and quantities in a cart |
| `orders` | Customer purchase transactions |
| `order_items` | Products captured in an order |
| `payments` | Payment transaction state and reference |

---

# 6. Supporting Entities

These tables support additional capabilities shown in the capstone architecture:

| Table | Purpose |
|---|---|
| `shipments` | Shipping method, delivery estimate, tracking and shipping state |
| `gift_point_accounts` | Customer gift-point balance |
| `gift_point_transactions` | Gift points earned/redeemed/adjusted |
| `coupons` | Promotional coupon definitions |
| `return_requests` | Return-request lifecycle |
| `refunds` | Refund transactions associated with payments |

The capstone architecture includes shipping, gift points, coupons, returns, refund processing, and recommendations. fileciteturn0file0

---

# 7. Entity Definitions

## 7.1 `users`

### Purpose

Stores registered user accounts. The capstone architecture includes guest/registered users, login/logout, and role/entitlement capability.

### Columns

| Column | PostgreSQL Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | User identifier |
| `first_name` | `VARCHAR(100)` | NOT NULL | User first name |
| `last_name` | `VARCHAR(100)` | NOT NULL | User last name |
| `email` | `VARCHAR(255)` | NOT NULL, UNIQUE | Login/account email |
| `password_hash` | `VARCHAR(255)` | NOT NULL | Hashed password |
| `role` | `VARCHAR(50)` | NOT NULL | User role |
| `status` | `VARCHAR(30)` | NOT NULL | Account status |
| `created_at` | `TIMESTAMP` | NOT NULL | Creation timestamp |
| `updated_at` | `TIMESTAMP` | NOT NULL | Last update timestamp |

### Initial values

**Role:** `CUSTOMER`  
**Possible future role:** `ADMIN`

**Status:**

- `ACTIVE`
- `INACTIVE`
- `LOCKED`

> The capstone identifies role/entitlement functionality but does not define the complete role matrix. Therefore, the authorization model remains intentionally minimal at this stage.

---

## 7.2 `addresses`

### Purpose

Stores reusable customer delivery addresses. The checkout wireframe explicitly requires selecting an address for delivery. fileciteturn0file0L144-L149

### Columns

| Column | PostgreSQL Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | Address identifier |
| `user_id` | `BIGINT` | FK, NOT NULL | Owning user |
| `label` | `VARCHAR(50)` | | Example: Home, Office |
| `address_line1` | `VARCHAR(255)` | NOT NULL | Primary address line |
| `address_line2` | `VARCHAR(255)` | | Secondary address line |
| `city` | `VARCHAR(100)` | NOT NULL | City |
| `state` | `VARCHAR(100)` | NOT NULL | State/region |
| `postal_code` | `VARCHAR(20)` | NOT NULL | Postal/ZIP code |
| `country` | `VARCHAR(100)` | NOT NULL | Country |
| `is_default` | `BOOLEAN` | NOT NULL | Default delivery address |
| `created_at` | `TIMESTAMP` | NOT NULL | Creation timestamp |
| `updated_at` | `TIMESTAMP` | NOT NULL | Last update timestamp |

### Relationship

```text
USER 1 ─────────── N ADDRESS
```

---

## 7.3 `categories`

### Purpose

Represents catalogue categories. The capstone explicitly requires users to select product categories and access a catalogue for each category. fileciteturn0file0L123-L130

### Columns

| Column | PostgreSQL Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | Category identifier |
| `name` | `VARCHAR(100)` | NOT NULL, UNIQUE | Category name |
| `description` | `TEXT` | | Category description |
| `active` | `BOOLEAN` | NOT NULL | Whether category is available |
| `created_at` | `TIMESTAMP` | NOT NULL | Creation timestamp |
| `updated_at` | `TIMESTAMP` | NOT NULL | Last update timestamp |

### Relationship

```text
CATEGORY 1 ─────────── N PRODUCT
```

---

## 7.4 `brands`

### Purpose

Represents the brands/publishers through which customers can browse products.

### Columns

| Column | PostgreSQL Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | Brand identifier |
| `name` | `VARCHAR(150)` | NOT NULL, UNIQUE | Brand name |
| `description` | `TEXT` | | Brand description |
| `active` | `BOOLEAN` | NOT NULL | Whether brand is available |
| `created_at` | `TIMESTAMP` | NOT NULL | Creation timestamp |
| `updated_at` | `TIMESTAMP` | NOT NULL | Last update timestamp |

### Relationship

```text
BRAND 1 ─────────── N PRODUCT
```

The wireframe explicitly includes browsing brands. fileciteturn0file0L126-L130

---

## 7.5 `products`

### Purpose

Represents books/products that can be browsed, selected, added to a cart, and purchased.

The customer journey also shows a selected product tagged with a tentative delivery date. fileciteturn0file0L90-L93

### Columns

| Column | PostgreSQL Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | Product identifier |
| `title` | `VARCHAR(255)` | NOT NULL | Book/product title |
| `isbn` | `VARCHAR(20)` | UNIQUE | ISBN when applicable |
| `description` | `TEXT` | | Product description |
| `price` | `NUMERIC(12,2)` | NOT NULL, >= 0 | Current catalogue price |
| `stock_quantity` | `INTEGER` | NOT NULL, >= 0 | Available inventory |
| `category_id` | `BIGINT` | FK, NOT NULL | Product category |
| `brand_id` | `BIGINT` | FK, NOT NULL | Product brand |
| `delivery_days_min` | `INTEGER` | >= 0 | Minimum estimated delivery time |
| `delivery_days_max` | `INTEGER` | >= `delivery_days_min` | Maximum estimated delivery time |
| `active` | `BOOLEAN` | NOT NULL | Whether product is purchasable |
| `created_at` | `TIMESTAMP` | NOT NULL | Creation timestamp |
| `updated_at` | `TIMESTAMP` | NOT NULL | Last update timestamp |

### Relationships

```text
CATEGORY 1 ─────────── N PRODUCT N ─────────── 1 BRAND
```

### Design note: delivery estimate

`delivery_days_min` and `delivery_days_max` are a proposed representation of the capstone's tentative delivery-date requirement. The capstone does not specify the exact database representation, so this is a design decision rather than a source-defined schema.

---

## 7.6 `carts`

### Purpose

Represents a customer's shopping cart.

The cart wireframe supports adding products and updating the basket. fileciteturn0file0L137-L143

### Columns

| Column | PostgreSQL Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | Cart identifier |
| `user_id` | `BIGINT` | FK, UNIQUE, NOT NULL | Cart owner |
| `status` | `VARCHAR(30)` | NOT NULL | Cart state |
| `created_at` | `TIMESTAMP` | NOT NULL | Creation timestamp |
| `updated_at` | `TIMESTAMP` | NOT NULL | Last update timestamp |

### Initial statuses

- `ACTIVE`
- `CHECKED_OUT`
- `ABANDONED`

### Relationship

```text
USER 1 ─────────── 1 CART
CART 1 ─────────── N CART_ITEM
```

The unique constraint on `user_id` ensures one active cart record per user in the initial model.

---

## 7.7 `cart_items`

### Purpose

Contains the individual products and quantities in a cart.

### Columns

| Column | PostgreSQL Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | Cart-item identifier |
| `cart_id` | `BIGINT` | FK, NOT NULL | Parent cart |
| `product_id` | `BIGINT` | FK, NOT NULL | Product in cart |
| `quantity` | `INTEGER` | NOT NULL, > 0 | Requested quantity |
| `unit_price` | `NUMERIC(12,2)` | NOT NULL, >= 0 | Current cart price snapshot |
| `created_at` | `TIMESTAMP` | NOT NULL | Creation timestamp |
| `updated_at` | `TIMESTAMP` | NOT NULL | Last update timestamp |

### Constraints

```text
UNIQUE(cart_id, product_id)
```

This ensures that adding the same product twice updates quantity rather than creating duplicate cart rows.

### Relationship

```text
CART 1 ─────────── N CART_ITEM N ─────────── 1 PRODUCT
```

---

## 7.8 `orders`

### Purpose

Represents a customer purchase. The architecture explicitly includes create/modify order, checkout, confirmation, cancellation, order history, and returns. fileciteturn0file0

### Columns

| Column | PostgreSQL Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | Internal order identifier |
| `order_number` | `VARCHAR(50)` | NOT NULL, UNIQUE | Customer-facing order number |
| `user_id` | `BIGINT` | FK, NOT NULL | Purchasing user |
| `shipping_name` | `VARCHAR(200)` | NOT NULL | Name captured at checkout |
| `shipping_line1` | `VARCHAR(255)` | NOT NULL | Shipping address snapshot |
| `shipping_line2` | `VARCHAR(255)` | | Shipping address snapshot |
| `shipping_city` | `VARCHAR(100)` | NOT NULL | Shipping city |
| `shipping_state` | `VARCHAR(100)` | NOT NULL | Shipping state/region |
| `shipping_postal_code` | `VARCHAR(20)` | NOT NULL | Shipping postal code |
| `shipping_country` | `VARCHAR(100)` | NOT NULL | Shipping country |
| `status` | `VARCHAR(40)` | NOT NULL | Order lifecycle state |
| `subtotal` | `NUMERIC(12,2)` | NOT NULL, >= 0 | Sum of item subtotals |
| `shipping_amount` | `NUMERIC(12,2)` | NOT NULL, >= 0 | Shipping charge |
| `discount_amount` | `NUMERIC(12,2)` | NOT NULL, >= 0 | Coupon/discount reduction |
| `gift_points_used` | `INTEGER` | NOT NULL, >= 0 | Gift points redeemed |
| `total_amount` | `NUMERIC(12,2)` | NOT NULL, >= 0 | Final order total |
| `placed_at` | `TIMESTAMP` | | Order placement timestamp |
| `cancellation_deadline` | `TIMESTAMP` | | Last time cancellation is allowed |
| `created_at` | `TIMESTAMP` | NOT NULL | Creation timestamp |
| `updated_at` | `TIMESTAMP` | NOT NULL | Last update timestamp |

### Recommended initial statuses

- `PENDING_PAYMENT`
- `PAID`
- `CONFIRMED`
- `CANCELLED`
- `SHIPPED`
- `DELIVERED`
- `RETURN_REQUESTED`
- `RETURNED`
- `REFUNDED`

The exact lifecycle may be simplified for the MVP.

### Cancellation business rule

The capstone explicitly requires **Cancel Order Within 48 hrs**. fileciteturn0file0L97-L99

Therefore:

```text
cancellation_deadline = placed_at + 48 hours
```

The service layer must verify the deadline before allowing cancellation.

### Important design decision: shipping-address snapshot

The order stores a copy of the selected shipping address. This prevents future edits to a user's saved address from changing the historical address of an existing order.

---

## 7.9 `order_items`

### Purpose

Represents the products actually purchased as part of an order.

### Columns

| Column | PostgreSQL Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | Order-item identifier |
| `order_id` | `BIGINT` | FK, NOT NULL | Parent order |
| `product_id` | `BIGINT` | FK, NOT NULL | Original product reference |
| `product_title` | `VARCHAR(255)` | NOT NULL | Purchase-time product title |
| `quantity` | `INTEGER` | NOT NULL, > 0 | Purchased quantity |
| `unit_price` | `NUMERIC(12,2)` | NOT NULL, >= 0 | Purchase-time unit price |
| `subtotal` | `NUMERIC(12,2)` | NOT NULL, >= 0 | `quantity × unit_price` |
| `created_at` | `TIMESTAMP` | NOT NULL | Creation timestamp |

### Relationship

```text
ORDER 1 ─────────── N ORDER_ITEM N ─────────── 1 PRODUCT
```

### Why snapshot title and price?

A catalogue product can later change price or title. Historical orders must remain accurate. Therefore, the order item stores the values actually purchased.

---

## 7.10 `payments`

### Purpose

Stores payment information and payment lifecycle state.

The payment wireframe explicitly describes selecting a payment option, including credit/debit card examples, completing payment, and providing confirmation. fileciteturn0file0L145-L164

### Columns

| Column | PostgreSQL Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | Payment identifier |
| `order_id` | `BIGINT` | FK, UNIQUE, NOT NULL | Related order |
| `payment_reference` | `VARCHAR(100)` | UNIQUE | Gateway/reference identifier |
| `payment_method` | `VARCHAR(30)` | NOT NULL | Payment method |
| `amount` | `NUMERIC(12,2)` | NOT NULL, >= 0 | Payment amount |
| `status` | `VARCHAR(30)` | NOT NULL | Payment state |
| `paid_at` | `TIMESTAMP` | | Successful payment timestamp |
| `created_at` | `TIMESTAMP` | NOT NULL | Creation timestamp |
| `updated_at` | `TIMESTAMP` | NOT NULL | Last update timestamp |

### Initial payment methods

- `CREDIT_CARD`
- `DEBIT_CARD`

### Payment statuses

- `INITIATED`
- `PROCESSING`
- `SUCCESS`
- `FAILED`
- `REFUNDED`

> The capstone does not identify a real payment provider. The initial implementation can therefore use a simulated payment workflow unless the project owner later requires gateway integration.

---

# 8. Supporting Entity Definitions

## 8.1 `shipments`

The architecture includes shipping-rate calculation, approximate delivery time, and return shipment. fileciteturn0file0

### Columns

| Column | PostgreSQL Type | Constraints |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `order_id` | `BIGINT` | FK, UNIQUE, NOT NULL |
| `shipping_method` | `VARCHAR(50)` | NOT NULL |
| `shipping_cost` | `NUMERIC(12,2)` | NOT NULL, >= 0 |
| `estimated_from` | `DATE` | |
| `estimated_to` | `DATE` | |
| `tracking_number` | `VARCHAR(100)` | UNIQUE |
| `status` | `VARCHAR(30)` | NOT NULL |
| `shipped_at` | `TIMESTAMP` | |
| `delivered_at` | `TIMESTAMP` | |
| `created_at` | `TIMESTAMP` | NOT NULL |
| `updated_at` | `TIMESTAMP` | NOT NULL |

### Initial statuses

- `PENDING`
- `SHIPPED`
- `IN_TRANSIT`
- `DELIVERED`
- `RETURNED`

---

## 8.2 `gift_point_accounts`

The payment/purchase flow explicitly includes redeeming gift points. fileciteturn0file0L145-L149

| Column | PostgreSQL Type | Constraints |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `user_id` | `BIGINT` | FK, UNIQUE, NOT NULL |
| `balance` | `INTEGER` | NOT NULL, >= 0 |
| `created_at` | `TIMESTAMP` | NOT NULL |
| `updated_at` | `TIMESTAMP` | NOT NULL |

Relationship:

```text
USER 1 ─────────── 1 GIFT_POINT_ACCOUNT
```

---

## 8.3 `gift_point_transactions`

Tracks changes to the gift-point balance.

| Column | PostgreSQL Type | Constraints |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `account_id` | `BIGINT` | FK, NOT NULL |
| `order_id` | `BIGINT` | FK, nullable |
| `type` | `VARCHAR(30)` | NOT NULL |
| `points` | `INTEGER` | NOT NULL |
| `description` | `VARCHAR(255)` | |
| `created_at` | `TIMESTAMP` | NOT NULL |

### Transaction types

- `EARNED`
- `REDEEMED`
- `EXPIRED`
- `ADJUSTED`

---

## 8.4 `coupons`

The architecture includes coupons within order capabilities. fileciteturn0file0

| Column | PostgreSQL Type | Constraints |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `code` | `VARCHAR(50)` | UNIQUE, NOT NULL |
| `description` | `VARCHAR(255)` | |
| `discount_type` | `VARCHAR(30)` | NOT NULL |
| `discount_value` | `NUMERIC(12,2)` | NOT NULL, >= 0 |
| `minimum_order_value` | `NUMERIC(12,2)` | >= 0 |
| `max_discount` | `NUMERIC(12,2)` | >= 0 |
| `valid_from` | `TIMESTAMP` | NOT NULL |
| `valid_until` | `TIMESTAMP` | NOT NULL |
| `usage_limit` | `INTEGER` | >= 0 |
| `active` | `BOOLEAN` | NOT NULL |

> Coupon rules are not defined in detail by the capstone. The schema is therefore provisional and can be simplified or extended during API/business-rule design.

---

## 8.5 `return_requests`

The architecture includes return orders and return shipment. fileciteturn0file0

| Column | PostgreSQL Type | Constraints |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `order_id` | `BIGINT` | FK, NOT NULL |
| `reason` | `VARCHAR(255)` | |
| `status` | `VARCHAR(30)` | NOT NULL |
| `requested_at` | `TIMESTAMP` | NOT NULL |
| `approved_at` | `TIMESTAMP` | |
| `completed_at` | `TIMESTAMP` | |

### Initial statuses

- `REQUESTED`
- `APPROVED`
- `REJECTED`
- `COMPLETED`

Return-item-level detail can be added later if the capstone requires partial returns.

---

## 8.6 `refunds`

The architecture includes refund processing. fileciteturn0file0

| Column | PostgreSQL Type | Constraints |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `payment_id` | `BIGINT` | FK, NOT NULL |
| `amount` | `NUMERIC(12,2)` | NOT NULL, >= 0 |
| `reason` | `VARCHAR(255)` | |
| `status` | `VARCHAR(30)` | NOT NULL |
| `refund_reference` | `VARCHAR(100)` | UNIQUE |
| `created_at` | `TIMESTAMP` | NOT NULL |
| `completed_at` | `TIMESTAMP` | |

---

# 9. Recommendation Design

The capstone requires related products and recommendations based on order history. The wireframes show related products and order-history-based recommendations, while the architecture also mentions upsell/cross-sell functionality. fileciteturn0file0L123-L143

The initial database design intentionally does **not** add a `recommendations` table.

### Initial approach

Recommendations can be derived from existing data:

```text
User Order History
        ↓
Previously Purchased Products
        ↓
Categories / Brands
        ↓
Related Products
```

Possible initial rules:

- Same category
- Same brand
- Products frequently associated with the user's purchased categories
- Products not already purchased, where appropriate

This is an application-level design decision. The capstone does not prescribe a recommendation algorithm or a recommendation table.

---

# 10. Related Product Design

The capstone explicitly requires related products to appear during catalogue/product browsing. fileciteturn0file0L128-L131

The initial design derives related products from catalogue attributes rather than storing an additional many-to-many relationship.

Example:

```text
Selected Product
      ↓
Same Category
      OR
Same Brand
      ↓
Related Products
```

A dedicated `product_relations` table should only be introduced if future requirements require manually curated or explicitly ranked relationships.

---

# 11. Entity Relationship Diagram

The core relational model is:

```mermaid
erDiagram

    USERS ||--o{ ADDRESSES : has
    USERS ||--|| CARTS : owns
    USERS ||--o{ ORDERS : places
    USERS ||--|| GIFT_POINT_ACCOUNTS : owns

    CATEGORIES ||--o{ PRODUCTS : contains
    BRANDS ||--o{ PRODUCTS : has

    CARTS ||--o{ CART_ITEMS : contains
    PRODUCTS ||--o{ CART_ITEMS : included_in

    ORDERS ||--o{ ORDER_ITEMS : contains
    PRODUCTS ||--o{ ORDER_ITEMS : purchased_as

    ORDERS ||--|| PAYMENTS : has
    ORDERS ||--|| SHIPMENTS : has
    ORDERS ||--o{ RETURN_REQUESTS : may_have

    PAYMENTS ||--o{ REFUNDS : may_have

    GIFT_POINT_ACCOUNTS ||--o{ GIFT_POINT_TRANSACTIONS : records
    ORDERS ||--o{ GIFT_POINT_TRANSACTIONS : may_reference

    USERS {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar role
        varchar status
    }

    ADDRESSES {
        bigint id PK
        bigint user_id FK
        varchar address_line1
        varchar city
        varchar state
        varchar postal_code
        varchar country
    }

    CATEGORIES {
        bigint id PK
        varchar name UK
        boolean active
    }

    BRANDS {
        bigint id PK
        varchar name UK
        boolean active
    }

    PRODUCTS {
        bigint id PK
        varchar title
        varchar isbn UK
        numeric price
        integer stock_quantity
        bigint category_id FK
        bigint brand_id FK
        integer delivery_days_min
        integer delivery_days_max
        boolean active
    }

    CARTS {
        bigint id PK
        bigint user_id FK
        varchar status
    }

    CART_ITEMS {
        bigint id PK
        bigint cart_id FK
        bigint product_id FK
        integer quantity
        numeric unit_price
    }

    ORDERS {
        bigint id PK
        varchar order_number UK
        bigint user_id FK
        varchar status
        numeric subtotal
        numeric shipping_amount
        numeric discount_amount
        integer gift_points_used
        numeric total_amount
        timestamp placed_at
        timestamp cancellation_deadline
    }

    ORDER_ITEMS {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        varchar product_title
        integer quantity
        numeric unit_price
        numeric subtotal
    }

    PAYMENTS {
        bigint id PK
        bigint order_id FK UK
        varchar payment_reference UK
        varchar payment_method
        numeric amount
        varchar status
        timestamp paid_at
    }

    SHIPMENTS {
        bigint id PK
        bigint order_id FK UK
        varchar shipping_method
        numeric shipping_cost
        date estimated_from
        date estimated_to
        varchar tracking_number UK
        varchar status
    }

    GIFT_POINT_ACCOUNTS {
        bigint id PK
        bigint user_id FK UK
        integer balance
    }

    GIFT_POINT_TRANSACTIONS {
        bigint id PK
        bigint account_id FK
        bigint order_id FK
        varchar type
        integer points
    }

    RETURN_REQUESTS {
        bigint id PK
        bigint order_id FK
        varchar reason
        varchar status
        timestamp requested_at
    }

    REFUNDS {
        bigint id PK
        bigint payment_id FK
        numeric amount
        varchar status
        varchar refund_reference UK
    }
```

---

# 12. Cardinality Summary

| Relationship | Cardinality | Meaning |
|---|---|---|
| User → Address | 1:N | A user can save multiple addresses |
| User → Cart | 1:1 | One active cart per user in the initial model |
| User → Order | 1:N | A user can place many orders |
| User → GiftPointAccount | 1:1 | One loyalty account per user |
| Category → Product | 1:N | A category contains many products |
| Brand → Product | 1:N | A brand contains many products |
| Cart → CartItem | 1:N | A cart contains many lines |
| Product → CartItem | 1:N | A product can occur in many carts |
| Order → OrderItem | 1:N | An order contains many purchased lines |
| Product → OrderItem | 1:N | A product can appear in many orders |
| Order → Payment | 1:1 | One primary payment record per order in MVP |
| Order → Shipment | 1:1 | One shipment record per order in MVP |
| Order → ReturnRequest | 1:N | An order may have one or more return records over time if required |
| Payment → Refund | 1:N | A payment may have one or more refund transactions |
| GiftPointAccount → GiftPointTransaction | 1:N | An account has a transaction history |

---

# 13. Database Constraints

The following constraints should be enforced at database and/or application level.

## Identity

```text
users.email UNIQUE NOT NULL
```

## Product

```text
products.title NOT NULL
products.price >= 0
products.stock_quantity >= 0
products.delivery_days_min >= 0
products.delivery_days_max >= products.delivery_days_min
```

## Cart

```text
cart_items.quantity > 0
UNIQUE(cart_id, product_id)
```

## Order

```text
order_items.quantity > 0
order_items.unit_price >= 0
order_items.subtotal >= 0
orders.total_amount >= 0
orders.gift_points_used >= 0
```

## Payment

```text
payments.amount >= 0
payments.payment_reference UNIQUE
```

---

# 14. Recommended Indexes

Initial indexes should focus on lookup paths used by the planned APIs.

| Table | Column(s) | Reason |
|---|---|---|
| `users` | `email` | Login lookup; also unique |
| `products` | `title` | Product search |
| `products` | `category_id` | Category browsing |
| `products` | `brand_id` | Brand browsing |
| `products` | `isbn` | Direct product identification; also unique |
| `orders` | `user_id` | Order history |
| `orders` | `order_number` | Direct order lookup; also unique |
| `orders` | `status` | Order-state queries |
| `cart_items` | `cart_id` | Cart retrieval |
| `order_items` | `order_id` | Order-detail retrieval |
| `payments` | `payment_reference` | Payment lookup; also unique |

Search-specific indexing can be refined after the OpenAPI query requirements are finalized.

---

# 15. Business Rules Represented by the Model

## 15.1 Order cancellation

The capstone explicitly specifies cancellation within 48 hours. fileciteturn0file0L97-L99

```text
cancellation_deadline = placed_at + 48 hours
```

The database stores the deadline for efficient validation and auditability.

## 15.2 Product availability

A product can only be purchased when it is active and sufficient stock exists.

```text
product.active = true
AND
product.stock_quantity >= requested_quantity
```

## 15.3 Cart uniqueness

The same product should not have multiple cart lines for the same cart.

```text
UNIQUE(cart_id, product_id)
```

## 15.4 Order price integrity

Order totals must be based on the price captured at purchase time, not the current catalogue price.

## 15.5 Historical address integrity

The order stores the delivery address as a snapshot so later customer address changes do not rewrite historical purchases.

---

# 16. MVP Database Scope

The first implementation should create these tables and relationships:

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

This supports:

```text
Login
  ↓
Browse catalog
  ↓
Category / Brand
  ↓
Product
  ↓
Cart
  ↓
Address
  ↓
Order
  ↓
Payment
  ↓
Confirmation
```

The capstone's core customer journey explicitly follows this general browse → select → cart → address → payment → confirmation flow. fileciteturn0file0L78-L101

---

# 17. Phase-2 Database Scope

After the core journey is stable, implement:

```text
shipments
gift_point_accounts
gift_point_transactions
coupons
return_requests
refunds
```

These correspond to additional architecture capabilities such as shipping, gift points, coupons, returns, and refund processing. fileciteturn0file0

---

# 18. Intentionally Deferred Tables

The following are intentionally **not** part of the initial database model:

```text
recommendations
product_relations
wishlists
reviews
```

Reason:

- The capstone requires related products/recommendations but does not prescribe a persistence model or algorithm.
- The current wireframes do not provide enough detail to justify additional relational complexity.
- These features can initially be implemented through application logic using catalogue and order-history data.

A dedicated table can be introduced later if an explicit requirement emerges.

---

# 19. Data Lifecycle Example

The following illustrates how the entities work together for a normal purchase.

```text
1. User logs in
       ↓
2. User browses Product
       ↓
3. Product is added to Cart
       ↓
4. CartItem stores quantity + current cart price
       ↓
5. User selects a saved Address
       ↓
6. Checkout creates Order
       ↓
7. Order copies shipping address into its snapshot
       ↓
8. OrderItem copies purchased title + price
       ↓
9. Payment is initiated
       ↓
10. Payment succeeds
       ↓
11. Order becomes CONFIRMED / PAID
       ↓
12. Purchase confirmation is returned
```

---

# 20. Data Integrity During Checkout

The checkout implementation should use a transaction boundary around critical writes.

Conceptually:

```text
BEGIN TRANSACTION

1. Validate cart
2. Validate product availability
3. Recalculate totals
4. Copy cart items into order items
5. Snapshot shipping address
6. Create order
7. Reserve/decrement stock according to implementation strategy
8. Create payment record

COMMIT
```

Payment-gateway communication, if a real provider is later introduced, may require a more nuanced transaction/outbox strategy. That is outside the currently specified capstone scope.

---

# 21. Data Modeling Decisions: Source vs Design Decision

| Decision | Classification | Reason |
|---|---|---|
| PostgreSQL | **Explicit source requirement** | Specified in capstone technology components |
| User/Product/Order entities | **Explicit source direction** | Capstone Step 1 asks these to be identified |
| Category and Brand entities | **Wireframe-derived** | Catalogue supports category and brand browsing |
| Cart and CartItem | **Wireframe-derived** | Basket/cart workflow requires line-level products |
| OrderItem | **Design decision** | Needed to model multiple products per order correctly |
| Payment entity | **Wireframe/architecture-derived** | Payment is a distinct workflow |
| Shipment entity | **Architecture-derived** | Shipping is a distinct capability |
| 48-hour cancellation deadline | **Explicit business rule** | Directly specified by capstone |
| Delivery day range fields | **Design decision** | Concrete representation of tentative delivery timing |
| Order shipping-address snapshot | **Design decision** | Preserve historical transaction integrity |
| Product title/price snapshot in OrderItem | **Design decision** | Preserve historical purchase values |
| Recommendation table | **Deferred design** | Algorithm/persistence not specified |
| Product relations table | **Deferred design** | Related-product rules not specified |
| Real payment gateway | **TBD** | Provider not specified by capstone |

---

# 22. Open Data-Model Decisions

The following items should be resolved before implementation or captured as explicit assumptions in the project README:

1. Whether the final user model needs registration APIs beyond login/logout.
2. Exact roles and permissions under the architecture's role/entitlement capability.
3. Whether one order can have multiple payments or only one primary payment.
4. Whether partial returns are required, which would justify a `return_items` table.
5. Exact coupon rules and whether an order may contain multiple coupons.
6. Whether gift points are earned automatically from orders and at what rate.
7. Exact shipping-rate calculation rules.
8. Whether payment remains simulated or integrates with an actual gateway.
9. Whether PostgreSQL full-text search is needed for product search.
10. Whether related/recommended products need manually curated relationships.

None of these should be silently assumed to be IBM-specified requirements.

---

# 23. Implementation Guidance for Spring Boot

The database model maps cleanly to a conventional Spring Boot layered structure:

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

Suggested entity names:

```text
User
Address
Category
Brand
Product
Cart
CartItem
Order
OrderItem
Payment
Shipment
GiftPointAccount
GiftPointTransaction
Coupon
ReturnRequest
Refund
```

The corresponding repositories can follow standard Spring Data JPA conventions.

Business rules such as the 48-hour cancellation rule should remain in service/domain logic rather than being hidden entirely inside repository/database queries.

---

# 24. Database Naming Conventions

The project should use consistent PostgreSQL naming:

- Tables: `snake_case`, plural nouns
- Columns: `snake_case`
- Primary keys: `id`
- Foreign keys: `<entity>_id`
- Timestamps: `created_at`, `updated_at`
- Boolean values: `is_*` where appropriate, e.g. `is_default`

Examples:

```text
order_items.order_id
products.category_id
addresses.user_id
```

---

# 25. Acceptance Criteria for Step 2

Step 2 is complete when all of the following are true:

- [ ] Every core requirement has a corresponding data entity or an explicit application-level design.
- [ ] `User`, `Product`, and `Order` are represented as requested by the capstone workflow.
- [ ] Category and brand browsing can be supported by the model.
- [ ] Cart and cart-item behavior can be represented.
- [ ] Multiple products can exist in an order.
- [ ] Historical order pricing is preserved.
- [ ] Delivery-address selection is supported.
- [ ] Payment status and reference are persisted.
- [ ] The 48-hour cancellation rule can be enforced.
- [ ] Additional architecture capabilities have either a supporting table or an explicit deferred design.
- [ ] Primary and foreign-key relationships are defined.
- [ ] Important uniqueness and validation constraints are identified.
- [ ] The ERD is consistent with the table definitions.
- [ ] The model is ready to be translated into OpenAPI request/response objects.

---

# 26. Next Step

The next artifact is:

```text
03-openapi-specification.yaml
```

The capstone instructs us to use the wireframes and identified entities/operations to generate and review an OpenAPI specification before using that specification to generate the Spring Boot application. fileciteturn0file0L192-L202

The OpenAPI design should therefore be based on this document rather than allowing the AI agent to invent a competing data model.

---

## Appendix A: Core Table Summary

| Table | PK | Main Foreign Keys |
|---|---|---|
| `users` | `id` | — |
| `addresses` | `id` | `user_id` |
| `categories` | `id` | — |
| `brands` | `id` | — |
| `products` | `id` | `category_id`, `brand_id` |
| `carts` | `id` | `user_id` |
| `cart_items` | `id` | `cart_id`, `product_id` |
| `orders` | `id` | `user_id` |
| `order_items` | `id` | `order_id`, `product_id` |
| `payments` | `id` | `order_id` |
| `shipments` | `id` | `order_id` |
| `gift_point_accounts` | `id` | `user_id` |
| `gift_point_transactions` | `id` | `account_id`, `order_id` |
| `coupons` | `id` | — |
| `return_requests` | `id` | `order_id` |
| `refunds` | `id` | `payment_id` |

---

## Appendix B: Core ER Flow

```text
USER
 │
 ├────────────── ADDRESS
 │
 ├────────────── CART
 │                 │
 │                 └──── CART_ITEM ───── PRODUCT
 │                                      /       \
 │                                     /         \
 │                              CATEGORY        BRAND
 │
 └────────────── ORDER
                    │
            ┌───────┼────────┬──────────┐
            │       │        │          │
        ORDER_ITEM PAYMENT SHIPMENT  RETURN_REQUEST
            │
            └──── PRODUCT

PAYMENT
   │
   └──── REFUND

USER
   │
   └──── GIFT_POINT_ACCOUNT
              │
              └──── GIFT_POINT_TRANSACTION
```

---

**Status:** Proposed baseline for Step 2.  
**Next artifact:** `03-openapi-specification.yaml`
