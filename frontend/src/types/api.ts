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
