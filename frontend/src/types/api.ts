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
