package com.ebookstore.catalog.controller;

import com.ebookstore.catalog.dto.BrandSummary;
import com.ebookstore.catalog.dto.ProductSummary;
import com.ebookstore.catalog.service.BrandService;
import com.ebookstore.catalog.service.ProductService;
import com.ebookstore.common.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public catalog endpoints for brands.
 * operationIds: listBrands, getProductsByBrand
 */
@RestController
@RequestMapping("/brands")
public class BrandController {

    private final BrandService brandService;
    private final ProductService productService;

    public BrandController(BrandService brandService, ProductService productService) {
        this.brandService = brandService;
        this.productService = productService;
    }

    /** operationId: listBrands — public (no JWT required) */
    @Operation(operationId = "listBrands", summary = "List all active brands")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<List<BrandSummary>> listBrands() {
        return ResponseEntity.ok(brandService.listActiveBrands());
    }

    /** operationId: getProductsByBrand — public (no JWT required) */
    @Operation(operationId = "getProductsByBrand", summary = "Get products by brand")
    @SecurityRequirements
    @GetMapping("/{brandId}/products")
    public ResponseEntity<PagedResponse<ProductSummary>> getProductsByBrand(
            @PathVariable Long brandId,
            @PageableDefault(size = 20, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.getProductsByBrand(brandId, pageable));
    }
}
