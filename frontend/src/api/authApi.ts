/**
 * Authentication API service.
 *
 * Maps frontend calls to the backend auth operationIds defined in
 * docs/03-openapi-specification.yaml.
 *
 * operationId mapping:
 *   registerUser  → POST /auth/register
 *   login         → POST /auth/login
 *   logout        → POST /auth/logout
 *   getCurrentUser → GET /users/me
 */

import { apiGet, apiPost } from './client'
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  UserResponse,
} from '../types/api'

/**
 * operationId: registerUser
 * POST /auth/register — public endpoint (security: [])
 * Returns the created UserResponse with HTTP 201.
 */
export function registerUser(data: RegisterRequest): Promise<UserResponse> {
  return apiPost<UserResponse>('/auth/register', data, /* skipAuth */ true)
}

/**
 * operationId: login
 * POST /auth/login — public endpoint (security: [])
 * Returns a LoginResponse containing the bearer token and user snapshot.
 */
export function login(data: LoginRequest): Promise<LoginResponse> {
  return apiPost<LoginResponse>('/auth/login', data, /* skipAuth */ true)
}

/**
 * operationId: logout
 * POST /auth/logout — requires Bearer token
 * Returns void (HTTP 204).
 */
export function logout(): Promise<void> {
  return apiPost<void>('/auth/logout')
}

/**
 * operationId: getCurrentUser
 * GET /users/me — requires Bearer token
 * Returns the current authenticated user.
 */
export function getCurrentUser(): Promise<UserResponse> {
  return apiGet<UserResponse>('/users/me')
}
