package com.ebookstore.catalog.service;

import com.ebookstore.catalog.CatalogMapper;
import com.ebookstore.catalog.dto.ProductResponse;
import com.ebookstore.catalog.dto.ProductSummary;
import com.ebookstore.catalog.entity.Product;
import com.ebookstore.catalog.repository.ProductRepository;
import com.ebookstore.catalog.repository.ProductSpecification;
import com.ebookstore.common.dto.PagedResponse;
import com.ebookstore.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;
import java.util.Set;

/**
 * Read-only service for product catalog operations.
 * All methods use {@code @Transactional(readOnly = true)}.
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Searches products with optional filters. Filters on non-null parameters only.
     *
     * @param availableOnly when {@code true} restricts to active, in-stock products
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductSummary> searchProducts(String q,
                                                        Long categoryId,
                                                        Long brandId,
                                                        BigDecimal minPrice,
                                                        BigDecimal maxPrice,
                                                        Boolean availableOnly,
                                                        Pageable pageable) {
        Specification<Product> spec =
                ProductSpecification.buildSearch(q, categoryId, brandId, minPrice, maxPrice, availableOnly);
        Page<Product> page = productRepository.findAll(spec, pageable);
        return PagedResponse.of(page.map(CatalogMapper::toSummary));
    }

    /**
     * Returns the full product detail.
     *
     * @throws ResourceNotFoundException if no product with {@code id} exists
     */
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return CatalogMapper.toResponse(product);
    }

    /**
     * Returns related products for the given product, up to {@code size}.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Active + in-stock products in the same category, excluding self.</li>
     *   <li>If count &lt; size, supplement from the same brand, excluding self
     *       and already-included products.</li>
     * </ol>
     *
     * @throws ResourceNotFoundException if the source product does not exist
     */
    @Transactional(readOnly = true)
    public List<ProductSummary> getRelatedProducts(Long productId, int size) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        Set<Long> seen = new LinkedHashSet<>();
        seen.add(productId);

        List<ProductSummary> results = new ArrayList<>();

        // Step 1: same category
        List<Product> sameCat = productRepository.findByCategoryIdAndActiveTrueAndIdNot(
                product.getCategory().getId(), productId);
        for (Product p : sameCat) {
            if (p.getStockQuantity() != null && p.getStockQuantity() > 0 && results.size() < size) {
                results.add(CatalogMapper.toSummary(p));
                seen.add(p.getId());
            }
        }

        // Step 2: supplement from same brand
        if (results.size() < size) {
            List<Product> sameBrand = productRepository.findByBrandIdAndActiveTrueAndIdNot(
                    product.getBrand().getId(), productId);
            for (Product p : sameBrand) {
                if (!seen.contains(p.getId())
                        && p.getStockQuantity() != null && p.getStockQuantity() > 0
                        && results.size() < size) {
                    results.add(CatalogMapper.toSummary(p));
                    seen.add(p.getId());
                }
            }
        }

        return results;
    }

    /**
     * Returns active products for a specific category, paginated.
     *
     * @throws ResourceNotFoundException if the category id is not found in any active product
     *         (the controller should verify the category exists; this method just filters)
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductSummary> getProductsByCategory(Long categoryId, Pageable pageable) {
        Specification<Product> spec = ProductSpecification.hasCategory(categoryId)
                .and(ProductSpecification.isActive());
        Page<Product> page = productRepository.findAll(spec, pageable);
        return PagedResponse.of(page.map(CatalogMapper::toSummary));
    }

    /**
     * Returns active products for a specific brand, paginated.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductSummary> getProductsByBrand(Long brandId, Pageable pageable) {
        Specification<Product> spec = ProductSpecification.hasBrand(brandId)
                .and(ProductSpecification.isActive());
        Page<Product> page = productRepository.findAll(spec, pageable);
        return PagedResponse.of(page.map(CatalogMapper::toSummary));
    }
}
