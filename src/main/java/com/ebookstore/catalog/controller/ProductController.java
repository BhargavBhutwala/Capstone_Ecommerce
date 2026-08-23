package com.ebookstore.catalog.controller;

import com.ebookstore.catalog.dto.ProductResponse;
import com.ebookstore.catalog.dto.ProductSummary;
import com.ebookstore.catalog.service.ProductService;
import com.ebookstore.common.dto.PagedResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Public catalog endpoints for products.
 * operationIds: searchProducts, getProduct, getRelatedProducts
 */
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * operationId: searchProducts
     *
     * <p>Default: {@code availableOnly=true}, sorted by {@code title,asc}.
     * All filter params are optional.
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ProductSummary>> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "true") Boolean availableOnly,
            @PageableDefault(size = 20, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(
                productService.searchProducts(q, categoryId, brandId, minPrice, maxPrice, availableOnly, pageable));
    }

    /** operationId: getProduct */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }

    /**
     * operationId: getRelatedProducts
     *
     * <p>Default size = 5 (sensible default; overridden by {@code ?size=} param).
     */
    @GetMapping("/{productId}/related")
    public ResponseEntity<List<ProductSummary>> getRelatedProducts(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(productService.getRelatedProducts(productId, size));
    }
}
