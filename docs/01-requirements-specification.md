# E-Bookstore Capstone Project
## Requirements Specification

**Document:** `01-requirements-specification.md`  
**Project:** AI Specialist Capstone Project  
**Source:** IBM AI Specialist Capstone instructions and wireframes  
**Status:** Baseline requirements for data-model and API design

---

## 1. Purpose

This document defines the functional and technical requirements for the **E-Bookstore Platform** capstone project.

The capstone describes an online store where a customer can browse, select, and purchase books. The project is intended to demonstrate the use of an **agentic IDE**, such as IBM BOB or AWS KIRO, to support requirements analysis, OpenAPI specification generation, Spring Boot backend generation, testing, and local/cloud readiness.

This document is the requirements baseline that will be used for:

1. Data-model design.
2. OpenAPI specification design.
3. AI-assisted Spring Boot backend generation.
4. API testing.
5. GitHub documentation and final demonstration.

---

## 2. Scope

### 2.1 In Scope

The platform covers the customer journey represented in the capstone wireframes and architecture, including:

- Landing/home experience for available books.
- User authentication and account access.
- Product/catalog browsing.
- Category and brand browsing.
- Product selection and product details.
- Related products.
- Product search and filtering.
- Shopping basket/cart management.
- Order history and Buy Again.
- Checkout.
- Delivery-address selection.
- Payment initiation and completion.
- Purchase/payment confirmation.
- Gift-point redemption.
- Recommendations based on order history.
- Order cancellation within 48 hours.
- Shipping capabilities identified by the architecture.
- Returns/refunds identified by the architecture.
- Coupon capability identified by the architecture.

### 2.2 Out of Scope / Not Fully Specified by the Source

The capstone material does not define detailed requirements for items such as:

- A specific payment gateway/provider.
- A specific authentication protocol/token mechanism.
- Detailed registration and password-recovery workflows.
- Detailed coupon rules.
- Detailed return/refund policies.
- Shipping-provider integration.
- A machine-learning recommendation algorithm.
- Detailed administrative UI/workflows.

These items should not be invented as mandatory requirements. They must be treated as implementation decisions or future-scope items unless later clarified by the project manager/SME.

---

## 3. Source of Requirements

The requirements in this document are derived from the capstone instructions, customer journey, architecture, and wireframes.

The source explicitly instructs the learner to analyze the wireframes, identify data entities such as **Product, User, and Order**, and identify required features such as **search, filter, and add to cart** before generating the API specification.

The source also specifies PostgreSQL, manual data-model preparation, OpenAPI specification generation, AI-augmented Spring Boot backend development, AI-augmented test generation, local/cloud readiness, GitHub submission, and a video demonstration.

---

## 4. Actors

### 4.1 Guest User

A guest represents a customer who has not authenticated.

Expected capability supported by the source:

- Access the bookstore.
- Browse available books/catalog content.

### 4.2 Registered User

A registered user is an authenticated customer.

Expected capabilities include:

- Login/logout.
- Browse the catalog.
- Select products.
- Add products to the basket.
- Checkout.
- Select a delivery address.
- Initiate and complete payment.
- View order history.
- Buy Again.
- Receive recommendations based on order history.
- Redeem gift points.
- Cancel an order within the permitted 48-hour window.

### 4.3 Roles and Entitlements

The architecture identifies **Role & Entitlement** as part of the member capability. The source does not define the concrete roles, permissions, or authorization matrix.

**Status:** Requirement identified; detailed authorization rules are TBD.

---

# 5. Functional Requirements

## 5.1 Home / Landing Page

### FR-001 — Landing Page

The system shall provide a landing/home experience showing the availability of books/products.

**Source:** The e-bookstore is described as having a landing page with the availability of books.

**Priority:** High

---

## 5.2 Authentication and Member Access

### FR-002 — User Login

The system shall support login for registered users.

**Priority:** High

### FR-003 — User Logout

The system shall support logout for authenticated users.

**Priority:** High

### FR-004 — Guest Browsing

The system shall allow a guest user to browse the bookstore/catalog.

**Priority:** High

### FR-005 — Roles and Entitlements

The system shall provide a mechanism for role/entitlement-based access where required by the platform architecture.

**Note:** Concrete roles and permissions are not specified by the capstone source.

**Priority:** Medium

---

## 5.3 Catalog

### FR-006 — Browse Catalog

The system shall provide a product catalog that users can browse.

**Priority:** High

### FR-007 — Browse by Category

The system shall allow users to select a product category and access the products associated with that category.

**Priority:** High

### FR-008 — Browse by Brand

The system shall allow users to browse products by brand.

**Priority:** High

### FR-009 — Select Product

The system shall allow a user to select a product from the catalog and view its product information.

**Priority:** High

### FR-010 — Related Products

The system shall present related products for a selected product.

**Priority:** Medium

### FR-011 — Product Search

The system shall support product search.

The capstone explicitly lists **search** as a feature to identify during wireframe analysis.

**Note:** Search fields, ranking, pagination, and sorting behavior are not specified by the source.

**Priority:** High

### FR-012 — Product Filtering

The system shall support product filtering as part of catalog browsing.

The capstone explicitly lists **filter** as a feature to identify during wireframe analysis.

**Note:** The exact filter dimensions are not fully specified. Category and brand are clearly represented by the catalog wireframes and architecture.

**Priority:** High

### FR-013 — Product Availability

The product/catalog experience shall expose product availability information sufficient to support selection and purchase.

**Priority:** High

### FR-014 — Tentative Delivery Information

A selected product shall provide tentative/estimated delivery information.

**Priority:** Medium

---

## 5.4 Shopping Cart / Basket

### FR-015 — Add Product to Basket

The system shall allow users to add one or more products to a shopping basket/cart.

**Priority:** High

### FR-016 — View Cart

The system shall allow the user to view the current contents of the shopping cart.

**Priority:** High

### FR-017 — Update Cart

The system shall allow the shopping cart to be updated.

The wireframe identifies the shopping-cart screen as providing the cart for update.

**Derived implementation behavior:** update quantities and remove items.

**Priority:** High

### FR-018 — Cart Recommendations

The system shall provide recommendations in the shopping-cart experience based on order history.

**Priority:** Medium

---

## 5.5 Order History

### FR-019 — View Order History

An authenticated user shall be able to browse previous orders.

**Priority:** Medium

### FR-020 — Buy Again

An authenticated user shall be able to use the Buy Again capability for a previous order.

**Implementation decision to be finalized:** the recommended implementation is to re-add eligible products to the user's cart and allow the normal checkout flow to continue.

**Priority:** Medium

---

## 5.6 Checkout and Order Creation

### FR-021 — Checkout

The system shall support checkout from the shopping cart.

**Priority:** High

### FR-022 — Create / Modify Order

The system shall support creation and modification of an order during the applicable order lifecycle.

**Priority:** High

### FR-023 — Select Delivery Address

The checkout flow shall allow the user to select the address to be used for delivery.

**Priority:** High

### FR-024 — Gift Point Redemption

The checkout/payment flow shall support redemption of gift points.

**Priority:** Medium

### FR-025 — Coupons

The order capability shall support coupons.

**Note:** Coupon validity, discount calculation, usage limits, and other business rules are not defined by the source.

**Priority:** Medium

---

## 5.7 Payment

### FR-026 — Initiate Payment

The system shall allow the customer to initiate payment using an appropriate payment option.

**Priority:** High

### FR-027 — Payment Methods

The payment experience shall support the payment methods represented by the source, including:

- Credit card.
- Debit card.

The source uses these as examples of payment options.

**Note:** Additional payment methods are not mandatory unless later required.

**Priority:** High

### FR-028 — Complete Payment

The system shall support completion of a payment transaction.

**Priority:** High

### FR-029 — Payment Confirmation

The system shall provide confirmation of the payment outcome.

**Priority:** High

---

## 5.8 Purchase Confirmation

### FR-030 — Purchase Confirmation

After successful purchase completion, the system shall present a confirmation indicating that the purchase has been completed.

**Priority:** High

---

## 5.9 Shipping

The architecture identifies a Shipping capability containing shipping-rate calculation, approximate delivery time, and return shipment.

### FR-031 — Shipping Rate Calculation

The system shall support shipping-rate calculation.

**Priority:** Medium

### FR-032 — Approximate Delivery Time

The system shall provide an approximate delivery time.

**Priority:** Medium

### FR-033 — Return Shipment

The system shall support return-shipment capability.

**Note:** Detailed shipping-provider integration and workflows are not specified by the source.

**Priority:** Low / Future

---

## 5.10 Order Cancellation and Returns

### FR-034 — Cancel Order

The system shall allow order cancellation subject to the applicable business rules.

**Priority:** Medium

### FR-035 — 48-Hour Cancellation Window

The customer shall be able to cancel an order within **48 hours** of purchase/order placement, subject to the final order-state rules implemented by the backend.

This is an explicit business rule in the customer journey.

**Priority:** High

### FR-036 — Return Order

The system shall support order returns.

**Note:** The source does not specify the return window, eligibility criteria, return reasons, or approval workflow.

**Priority:** Low / Future

### FR-037 — Refund Processing

The architecture identifies refund processing as a payment capability.

**Note:** Detailed refund rules are not specified.

**Priority:** Low / Future

---

## 5.11 Recommendations

The source identifies multiple recommendation concepts.

### FR-038 — Order-History Recommendations

The system shall provide recommended items based on order history.

**Priority:** Medium

### FR-039 — Related Product Recommendations

The catalog experience shall provide related products for selection.

**Priority:** Medium

### FR-040 — Upsell / Cross-sell

The architecture identifies upsell and cross-sell products as a recommendation capability.

**Note:** The source does not specify the recommendation algorithm.

**Priority:** Low / Future

---

# 6. Business Rules

## BR-001 — Order Cancellation Window

An order can be cancelled within **48 hours** of order placement/purchase, subject to the final order-status implementation.

## BR-002 — Product Availability

A product must be available for purchase before it can be successfully added/ordered through the purchase flow.

The detailed inventory behavior is not specified by the source and will be finalized during API and backend design.

## BR-003 — Order History

The order-history capability is associated with an authenticated customer account.

## BR-004 — Buy Again

Buy Again is initiated from a user's order history and should reuse the normal purchasing flow.

The exact implementation behavior is a design decision.

## BR-005 — Gift Points

Gift points may be redeemed as part of the purchase flow.

The rules for earning, expiration, conversion, and maximum redemption are not specified by the source.

## BR-006 — Related Products

A selected product may expose related products for additional selection.

The exact relationship/recommendation algorithm is not defined by the source.

---

# 7. Non-Functional / Technical Requirements

## NFR-001 — Database

The backend shall use **PostgreSQL** as the database technology specified by the capstone.

## NFR-002 — Agentic IDE

The project shall use an agentic IDE such as **IBM BOB** or **AWS KIRO** for AI-assisted development.

## NFR-003 — Manual Data Model Preparation

The data model shall be prepared based on the wireframes before AI-assisted backend generation.

## NFR-004 — OpenAPI Specification

An OpenAPI specification shall be generated/reviewed before using it as the contract for Spring Boot backend generation.

## NFR-005 — Spring Boot Backend

The generated backend shall be implemented as a Spring Boot application.

## NFR-006 — Local Readiness

The application shall be runnable and testable on a local development environment.

## NFR-007 — Cloud Readiness

The project may be deployed to an available cloud environment such as AWS ROSA or IBM ROKS when such an environment is available.

Cloud deployment is presented by the source as an available option rather than a mandatory requirement.

## NFR-008 — API Testing

The APIs shall be tested using an API testing tool such as Insomnia or through an equivalent supported method, and responses shall be checked against the API specification.

## NFR-009 — Source Control

The completed work shall be checked into a personal GitHub repository and made available to the manager through a pull request.

## NFR-010 — Demonstration

A video shall be recorded describing the development steps and submitted for manager review.

## NFR-011 — AI-Augmented Testing

The capstone expects AI-augmented test-case generation, for example through an ICA Agent using a test-scenario generator, API test-case generator, or React test-case generator.

---

# 8. Core User Journeys

## UJ-001 — Browse Books as Guest

```text
Guest User
    ↓
Landing / Home
    ↓
Browse Catalog
    ↓
Select Category or Brand
    ↓
Select Product
    ↓
View Related Products
```

## UJ-002 — Purchase Book as Registered User

```text
Registered User
    ↓
Login
    ↓
Browse Catalog
    ↓
Select Product
    ↓
Add Product to Basket
    ↓
Checkout
    ↓
Select Delivery Address
    ↓
Select Payment Option
    ↓
Initiate Payment
    ↓
Complete Payment
    ↓
Payment Confirmation
    ↓
Purchase Confirmation
```

## UJ-003 — Buy Again

```text
Registered User
    ↓
Order History
    ↓
Select Previous Order
    ↓
Buy Again
    ↓
Continue through Cart / Checkout Flow
```

## UJ-004 — Cancel Order

```text
Existing Order
    ↓
Check Cancellation Eligibility
    ↓
Within 48 Hours?
   ├── Yes → Cancel Order
   └── No  → Reject Cancellation
```

---

# 9. MVP Scope

The following capabilities should form the first implementation milestone because they support the main end-to-end purchase flow:

1. User authentication.
2. Categories.
3. Brands.
4. Product catalog.
5. Product search/filtering.
6. Product details.
7. Cart.
8. Delivery address selection.
9. Order creation.
10. Payment initiation/completion.
11. Purchase confirmation.
12. Order history.
13. Order cancellation with the 48-hour rule.

The following capabilities can be implemented after the core purchase flow is stable:

- Buy Again.
- Related products.
- Order-history recommendations.
- Gift points.
- Coupons.
- Shipping-rate calculation.
- Returns.
- Refunds.
- Upsell/cross-sell.

This priority split is an implementation strategy, not a replacement for the source requirements.

---

# 10. Open / TBD Decisions

The following items require later design decisions because the capstone material does not provide enough detail:

| Area | Open Decision |
|---|---|
| Authentication | Token/session mechanism and detailed security flow |
| Registration | Exact registration workflow and validation |
| Roles | Roles, permissions, and entitlement matrix |
| Search | Search fields, ranking, pagination, and sorting |
| Inventory | Stock reservation/decrement behavior |
| Payment | Real gateway vs simulated payment |
| Payment security | Handling of payment credentials/tokens |
| Shipping | Rate calculation method and provider |
| Returns | Eligibility and return window |
| Refunds | Refund triggers and calculation |
| Coupons | Discount model and usage rules |
| Gift points | Earning, redemption, expiry, and limits |
| Recommendations | Rule-based vs ML implementation |
| Product relationships | Exact related-product logic |
| Administration | Whether/when admin workflows are required |

These decisions should be resolved **before the corresponding backend feature is finalized**, but they should not block the initial data-model/API design where a reasonable bounded assumption is sufficient.

---

# 11. Requirements Traceability Matrix

| Requirement ID | Capability | Source / Basis | Initial Priority |
|---|---|---|---|
| FR-001 | Landing page | E-bookstore description / wireframe | High |
| FR-002 | Login | Member capability / user journey | High |
| FR-003 | Logout | Member capability | High |
| FR-004 | Guest browsing | Member/store capability | High |
| FR-007 | Category browsing | Catalog wireframe | High |
| FR-008 | Brand browsing | Catalog wireframe | High |
| FR-009 | Product selection | Catalog wireframe | High |
| FR-010 | Related products | Catalog wireframe | Medium |
| FR-011 | Search | Step 1 wireframe-analysis instruction | High |
| FR-012 | Filtering | Step 1 wireframe-analysis instruction | High |
| FR-015 | Add to basket | Customer journey / cart wireframe | High |
| FR-018 | Cart recommendations | Cart wireframe | Medium |
| FR-019 | Order history | Catalog/customer journey/architecture | Medium |
| FR-020 | Buy Again | Catalog wireframe | Medium |
| FR-021 | Checkout | Order architecture / purchase journey | High |
| FR-023 | Delivery address | Payment & Purchase wireframe | High |
| FR-024 | Gift point redemption | Payment & Purchase wireframe | Medium |
| FR-025 | Coupons | Order architecture | Medium |
| FR-026 | Payment initiation | Payment wireframe | High |
| FR-027 | Credit/debit card | Payment wireframe | High |
| FR-030 | Purchase confirmation | Confirmation wireframe | High |
| FR-031 | Shipping calculation | Shipping architecture | Medium |
| FR-032 | Approximate delivery time | Shipping architecture | Medium |
| FR-035 | 48-hour cancellation | Customer journey | High |
| FR-036 | Return order | Order architecture | Low / Future |
| FR-037 | Refund processing | Payment architecture | Low / Future |
| FR-038 | Order-history recommendations | Customer journey / cart wireframe | Medium |
| FR-040 | Upsell/cross-sell | Store architecture | Low / Future |

---

# 12. Acceptance Baseline for the Next Phase

Before moving from requirements into data-model/API design, the following should be true:

- [ ] The main customer journey is understood.
- [ ] The required actors are identified.
- [ ] Catalog capabilities are identified.
- [ ] Cart capabilities are identified.
- [ ] Checkout, payment, and confirmation capabilities are identified.
- [ ] Order-history and Buy Again capabilities are identified.
- [ ] The 48-hour cancellation rule is explicitly captured.
- [ ] Architecture-only capabilities such as shipping, returns, refunds, coupons, and recommendations are recorded without inventing unsupported detail.
- [ ] MVP priorities are documented.
- [ ] TBD decisions are clearly separated from source-derived requirements.
- [ ] The requirements baseline is ready to drive the data model and OpenAPI specification.

---

# 13. Next Step

The next project artifact is:

**`02-data-model-design.md`**

It will translate these requirements into:

- PostgreSQL entities/tables.
- Columns and data types.
- Primary and foreign keys.
- Relationships/cardinalities.
- Constraints and indexes.
- Order and payment statuses.
- Historical-data rules such as order-item price snapshots.
- The final ER diagram.

The OpenAPI specification should be designed **after this data-model baseline**, following the capstone workflow.

---

## References

This document is derived from the uploaded IBM capstone instructions, including the objective, e-commerce customer journey, architecture, wireframes, technology workflow, and prescribed backend development workflow.
