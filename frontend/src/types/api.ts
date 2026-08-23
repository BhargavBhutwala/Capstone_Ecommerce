/**
 * OpenAPI-aligned shared DTO types used across multiple frontend features.
 *
 * Field names mirror the JSON camelCase property names in the OpenAPI contract.
 * These types are derived from docs/03-openapi-specification.yaml and must stay
 * aligned with the backend response shapes — do not rename or add fields here.
 */

// ─── Enumerations ──────────────────────────────────────────────────────────────

export type UserRole = 'CUSTOMER' | 'ADMIN'
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'LOCKED'

// ─── Auth ──────────────────────────────────────────────────────────────────────

/** POST /auth/register request body — operationId: registerUser */
export interface RegisterRequest {
  firstName: string
  lastName: string
  email: string
  password: string
}

/** POST /auth/login request body — operationId: login */
export interface LoginRequest {
  email: string
  password: string
}

/** POST /auth/login response body — operationId: login */
export interface LoginResponse {
  accessToken: string
  tokenType: string
  /** Token lifetime in seconds (optional) */
  expiresIn?: number
  user: UserResponse
}

// ─── Users ─────────────────────────────────────────────────────────────────────

/** GET /users/me response body — operationId: getCurrentUser */
export interface UserResponse {
  id: number
  firstName: string
  lastName: string
  email: string
  role: UserRole
  status: UserStatus
  createdAt?: string
}

// ─── Catalog — Categories ──────────────────────────────────────────────────────

/** Item in GET /categories array response — operationId: listCategories */
export interface CategorySummary {
  id: number
  name: string
  description?: string
  active?: boolean
}

// ─── Catalog — Brands ──────────────────────────────────────────────────────────

/** Item in GET /brands array response — operationId: listBrands */
export interface BrandSummary {
  id: number
  name: string
  description?: string
  active?: boolean
}

// ─── Catalog — Products ────────────────────────────────────────────────────────

/** Delivery estimate embedded in ProductResponse */
export interface DeliveryEstimate {
  minDays: number
  maxDays: number
}

/**
 * Product summary — used in paginated product lists and related-products arrays.
 * operationId: searchProducts / getProductsByCategory / getProductsByBrand / getRelatedProducts
 */
export interface ProductSummary {
  id: number
  title: string
  /** Monetary value — treat as BigDecimal-sourced, render with 2 decimal places */
  price: number
  available: boolean
  isbn?: string
  stockQuantity?: number
}

/**
 * Full product detail — returned by GET /products/{productId}.
 * Extends ProductSummary via OpenAPI allOf composition.
 * operationId: getProduct
 */
export interface ProductResponse extends ProductSummary {
  description?: string
  category?: CategorySummary
  brand?: BrandSummary
  deliveryEstimate?: DeliveryEstimate
}

// ─── Errors ────────────────────────────────────────────────────────────────────

/**
 * Normalised backend error shape returned by all non-2xx responses.
 * Defined in docs/03-openapi-specification.yaml#/components/schemas/ErrorResponse
 */
export interface ErrorResponse {
  timestamp: string
  status: number
  code: string
  message: string
  path: string
  /** Field-level validation errors keyed by field name */
  fieldErrors?: Record<string, string>
}

// ─── Pagination ────────────────────────────────────────────────────────────────

/** Standard pagination metadata returned in all paginated responses */
export interface PageMeta {
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/** Generic paginated envelope matching { content, page } backend contract */
export interface PagedResponse<T> {
  content: T[]
  page: PageMeta
}

// ─── Cart ──────────────────────────────────────────────────────────────────────

export type CartStatus = 'ACTIVE' | 'CHECKED_OUT' | 'ABANDONED'

/** POST /cart/items request body — operationId: addCartItem */
export interface AddCartItemRequest {
  productId: number
  /** minimum: 1, maximum: 999 */
  quantity: number
}

/** PUT /cart/items/{itemId} request body — operationId: updateCartItem */
export interface UpdateCartItemRequest {
  /** minimum: 1, maximum: 999 */
  quantity: number
}

/** Single item in a CartResponse — operationId: getCart / addCartItem / updateCartItem */
export interface CartItemResponse {
  id: number
  product: ProductSummary
  quantity: number
  /** Server-computed unit price — BigDecimal-sourced, render with 2 dp */
  unitPrice: number
  /** Server-computed line subtotal */
  subtotal: number
}

// ─── Addresses ────────────────────────────────────────────────────────────────

/**
 * POST /addresses / PUT /addresses/{addressId} request body.
 * operationId: createAddress / updateAddress
 * Field constraints mirror AddressRequest in the OpenAPI contract.
 */
export interface AddressRequest {
  /** Optional label e.g. "Home", "Work". maxLength: 50 */
  label?: string
  /** Required. minLength:1, maxLength:255 */
  addressLine1: string
  /** Optional. maxLength:255 */
  addressLine2?: string
  /** Required. minLength:1, maxLength:100 */
  city: string
  /** Required. minLength:1, maxLength:100 */
  state: string
  /** Required. minLength:3, maxLength:20 */
  postalCode: string
  /** Required. minLength:2, maxLength:100 */
  country: string
  /** Default: false */
  isDefault?: boolean
}

/** GET /addresses[] / POST /addresses response — operationId: listAddresses / createAddress */
export interface AddressResponse extends AddressRequest {
  id: number
}

// ─── Orders ───────────────────────────────────────────────────────────────────

export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'CONFIRMED'
  | 'CANCELLED'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'RETURN_REQUESTED'
  | 'RETURNED'
  | 'REFUNDED'

/** POST /orders request body — operationId: createOrder */
export interface CreateOrderRequest {
  /** ID of the saved address to use as shipping address */
  addressId: number
}

/** Single line item inside an OrderResponse */
export interface OrderItemResponse {
  id: number
  productId: number
  productTitle: string
  quantity: number
  /** Purchase-price snapshot — BigDecimal-sourced */
  unitPrice: number
  subtotal: number
}

/** Shipping address snapshot embedded in an order (immutable after creation) */
export interface ShippingAddressSnapshot {
  name: string
  addressLine1: string
  addressLine2?: string
  city: string
  state: string
  postalCode: string
  country: string
}

/**
 * Full order payload — operationId: getOrder / createOrder
 */
export interface OrderResponse {
  id: number
  orderNumber: string
  status: OrderStatus
  items: OrderItemResponse[]
  shippingAddress?: ShippingAddressSnapshot
  subtotal: number
  shippingAmount?: number
  discountAmount?: number
  totalAmount: number
  placedAt?: string
  cancellationDeadline?: string | null
}

/**
 * Full cart payload returned by getCart, addCartItem, updateCartItem.
 * operationId: getCart
 */
export interface CartResponse {
  id: number
  status: CartStatus
  items: CartItemResponse[]
  /** Server-computed cart subtotal */
  subtotal: number
  /** Server-computed order total */
  totalAmount: number
  /** Optional server-supplied recommendations */
  recommendedProducts?: ProductSummary[]
}
