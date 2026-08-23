package com.ebookstore.catalog.controller;

import com.ebookstore.catalog.dto.CategorySummary;
import com.ebookstore.catalog.dto.ProductSummary;
import com.ebookstore.catalog.service.CategoryService;
import com.ebookstore.catalog.service.ProductService;
import com.ebookstore.common.dto.PagedResponse;
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
 * Public catalog endpoints for categories.
 * operationIds: listCategories, getProductsByCategory
 */
@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final ProductService productService;

    public CategoryController(CategoryService categoryService, ProductService productService) {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    /** operationId: listCategories */
    @GetMapping
    public ResponseEntity<List<CategorySummary>> listCategories() {
        return ResponseEntity.ok(categoryService.listActiveCategories());
    }

    /** operationId: getProductsByCategory */
    @GetMapping("/{categoryId}/products")
    public ResponseEntity<PagedResponse<ProductSummary>> getProductsByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(size = 20, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId, pageable));
    }
}
