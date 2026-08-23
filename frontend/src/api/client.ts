/**
 * Base API client.
 *
 * Wraps the native fetch API with:
 * - Base URL from VITE_API_BASE_URL environment variable
 * - JSON serialisation/deserialisation
 * - Authorization: Bearer header injection from the active session token
 * - Normalised ApiError thrown for all non-2xx responses
 *
 * This module has no knowledge of React context. Token retrieval is injected
 * at call time so the client remains usable outside React trees.
 */

import type { ErrorResponse } from '../types/api'

// ─── Configuration ─────────────────────────────────────────────────────────────

const BASE_URL: string = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '/api'

// ─── ApiError ─────────────────────────────────────────────────────────────────

/**
 * Structured error thrown when the backend returns a non-2xx response.
 * Carries the full ErrorResponse payload when the server returns one.
 */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly path: string
  readonly fieldErrors?: Record<string, string>
  readonly timestamp: string

  constructor(payload: ErrorResponse) {
    super(payload.message)
    this.name = 'ApiError'
    this.status = payload.status
    this.code = payload.code
    this.path = payload.path
    this.fieldErrors = payload.fieldErrors
    this.timestamp = payload.timestamp
  }

  /** Convenience: true when this is a 401 Unauthorized */
  get isUnauthorized(): boolean {
    return this.status === 401
  }

  /** Convenience: true when this is a 404 Not Found */
  get isNotFound(): boolean {
    return this.status === 404
  }

  /** Convenience: true when this is a 409 Conflict */
  get isConflict(): boolean {
    return this.status === 409
  }
}

// ─── Token accessor ────────────────────────────────────────────────────────────

/**
 * Mutable reference to the active bearer token.
 * Set by the auth context after a successful login; cleared on logout.
 * Using a module-level variable avoids React context in this layer.
 */
let _activeToken: string | null = null

export function setActiveToken(token: string | null): void {
  _activeToken = token
}

export function getActiveToken(): string | null {
  return _activeToken
}

// ─── Core request function ─────────────────────────────────────────────────────

interface RequestOptions {
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  path: string
  body?: unknown
  /** When true, do not attach the Authorization header even if a token is set */
  skipAuth?: boolean
}

async function request<T>(options: RequestOptions): Promise<T> {
  const { method, path, body, skipAuth = false } = options

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  }

  if (!skipAuth && _activeToken) {
    headers['Authorization'] = `Bearer ${_activeToken}`
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  // 204 No Content — return void cast as T
  if (response.status === 204) {
    return undefined as T
  }

  let json: unknown
  try {
    json = await response.json()
  } catch {
    // Response body is not valid JSON; surface a synthetic error
    throw new ApiError({
      timestamp: new Date().toISOString(),
      status: response.status,
      code: 'PARSE_ERROR',
      message: `Unexpected response from server (HTTP ${response.status})`,
      path,
    })
  }

  if (!response.ok) {
    // Attempt to interpret the response body as an ErrorResponse
    const payload = json as Partial<ErrorResponse>
    throw new ApiError({
      timestamp: payload.timestamp ?? new Date().toISOString(),
      status: payload.status ?? response.status,
      code: payload.code ?? 'UNKNOWN_ERROR',
      message: payload.message ?? response.statusText,
      path: payload.path ?? path,
      fieldErrors: payload.fieldErrors,
    })
  }

  return json as T
}

// ─── Public typed helpers ─────────────────────────────────────────────────────

export function apiGet<T>(path: string, skipAuth = false): Promise<T> {
  return request<T>({ method: 'GET', path, skipAuth })
}

export function apiPost<T>(path: string, body?: unknown, skipAuth = false): Promise<T> {
  return request<T>({ method: 'POST', path, body, skipAuth })
}

export function apiPut<T>(path: string, body?: unknown): Promise<T> {
  return request<T>({ method: 'PUT', path, body })
}

export function apiDelete<T = void>(path: string): Promise<T> {
  return request<T>({ method: 'DELETE', path })
}
