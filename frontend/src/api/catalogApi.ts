/**
 * Catalog API service.
 *
 * Maps frontend calls to the backend catalog operationIds defined in
 * docs/03-openapi-specification.yaml.
 *
 * All catalog endpoints are public (security: []).
 * The shared API client's skipAuth=true ensures no Authorization header is
 * sent, keeping catalog requests unauthenticated as required.
 *
 * operationId mapping:
 *   listCategories        → GET /categories
 *   getProductsByCategory → GET /categories/{categoryId}/products
 *   listBrands            → GET /brands
 *   getProductsByBrand    → GET /brands/{brandId}/products
 *   searchProducts        → GET /products
 *   getProduct            → GET /products/{productId}
 *   getRelatedProducts    → GET /products/{productId}/related
 */

import { apiGet } from './client'
import type {
  BrandSummary,
  CategorySummary,
  PagedResponse,
  ProductResponse,
  ProductSummary,
} from '../types/api'

// ─── Parameter types ───────────────────────────────────────────────────────────

/** Query parameters for GET /products (searchProducts) */
export interface SearchProductsParams {
  q?: string
  categoryId?: number
  brandId?: number
  minPrice?: number
  maxPrice?: number
  /** Default: true per OpenAPI spec */
  availableOnly?: boolean
  /** Zero-based page index, default 0 */
  page?: number
  /** Page size 1–100, default 20 */
  size?: number
  /** Spring-style sort expression, e.g. "title,asc". Default: "title,asc" */
  sort?: string
}

/** Query parameters for paginated category/brand product endpoints */
export interface PagedProductsParams {
  page?: number
  size?: number
  sort?: string
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Build a query string from a plain object, omitting undefined/null values */
function buildQuery(params: Record<string, string | number | boolean | undefined | null>): string {
  const parts: string[] = []
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    }
  }
  return parts.length > 0 ? `?${parts.join('&')}` : ''
}

// ─── Catalog API functions ─────────────────────────────────────────────────────

/**
 * operationId: listCategories
 * GET /categories — public endpoint
 * Returns the full list of categories (not paginated).
 */
export function listCategories(): Promise<CategorySummary[]> {
  return apiGet<CategorySummary[]>('/categories', /* skipAuth */ true)
}

/**
 * operationId: getProductsByCategory
 * GET /categories/{categoryId}/products — public endpoint
 * Returns a paginated list of products in the given category.
 */
export function getProductsByCategory(
  categoryId: number,
  params: PagedProductsParams = {},
): Promise<PagedResponse<ProductSummary>> {
  const query = buildQuery(params as Record<string, string | number | boolean | undefined>)
  return apiGet<PagedResponse<ProductSummary>>(
    `/categories/${categoryId}/products${query}`,
    /* skipAuth */ true,
  )
}

/**
 * operationId: listBrands
 * GET /brands — public endpoint
 * Returns the full list of brands (not paginated).
 */
export function listBrands(): Promise<BrandSummary[]> {
  return apiGet<BrandSummary[]>('/brands', /* skipAuth */ true)
}

/**
 * operationId: getProductsByBrand
 * GET /brands/{brandId}/products — public endpoint
 * Returns a paginated list of products by the given brand.
 */
export function getProductsByBrand(
  brandId: number,
  params: PagedProductsParams = {},
): Promise<PagedResponse<ProductSummary>> {
  const query = buildQuery(params as Record<string, string | number | boolean | undefined>)
  return apiGet<PagedResponse<ProductSummary>>(
    `/brands/${brandId}/products${query}`,
    /* skipAuth */ true,
  )
}

/**
 * operationId: searchProducts
 * GET /products — public endpoint
 * Returns a paginated, filtered, sorted list of products.
 */
export function searchProducts(
  params: SearchProductsParams = {},
): Promise<PagedResponse<ProductSummary>> {
  const query = buildQuery(params as Record<string, string | number | boolean | undefined>)
  return apiGet<PagedResponse<ProductSummary>>(`/products${query}`, /* skipAuth */ true)
}

/**
 * operationId: getProduct
 * GET /products/{productId} — public endpoint
 * Returns full product detail including category, brand, and delivery estimate.
 */
export function getProduct(productId: number): Promise<ProductResponse> {
  return apiGet<ProductResponse>(`/products/${productId}`, /* skipAuth */ true)
}

/**
 * operationId: getRelatedProducts
 * GET /products/{productId}/related — public endpoint
 * Returns an array (NOT paginated) of related ProductSummary objects.
 */
export function getRelatedProducts(
  productId: number,
  size?: number,
): Promise<ProductSummary[]> {
  const query = size !== undefined ? `?size=${size}` : ''
  return apiGet<ProductSummary[]>(`/products/${productId}/related${query}`, /* skipAuth */ true)
}
