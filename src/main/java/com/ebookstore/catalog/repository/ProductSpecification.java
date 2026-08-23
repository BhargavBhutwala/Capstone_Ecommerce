package com.ebookstore.catalog.repository;

import com.ebookstore.catalog.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Static factory methods for {@link Product} query predicates.
 *
 * <p>Each method returns a composable {@link Specification} that can be
 * chained with {@code .and()} to build dynamic queries:
 * <pre>
 *   Specification&lt;Product&gt; spec = Specification.where(null);
 *   if (q != null)         spec = spec.and(ProductSpecification.hasTitleContaining(q));
 *   if (categoryId != null) spec = spec.and(ProductSpecification.hasCategory(categoryId));
 * </pre>
 */
public final class ProductSpecification {

    private ProductSpecification() {}

    /**
     * Case-insensitive substring match against {@code title} OR {@code description}.
     */
    public static Specification<Product> hasTitleContaining(String q) {
        return (root, query, cb) -> {
            String pattern = "%" + q.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    /** Exact match on {@code category.id}. */
    public static Specification<Product> hasCategory(Long categoryId) {
        return (root, query, cb) ->
                cb.equal(root.get("category").get("id"), categoryId);
    }

    /** Exact match on {@code brand.id}. */
    public static Specification<Product> hasBrand(Long brandId) {
        return (root, query, cb) ->
                cb.equal(root.get("brand").get("id"), brandId);
    }

    /** Price &ge; minPrice. */
    public static Specification<Product> hasPriceAtLeast(BigDecimal minPrice) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    /** Price &le; maxPrice. */
    public static Specification<Product> hasPriceAtMost(BigDecimal maxPrice) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    /**
     * Product is available: {@code active = true AND stockQuantity &gt; 0}.
     */
    public static Specification<Product> isAvailable() {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("active")),
                cb.greaterThan(root.get("stockQuantity"), 0)
        );
    }

    /** Product is active (visible). */
    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    /**
     * Builds a combined {@link Specification} from optional filter parameters.
     * Only non-null parameters contribute predicates.
     */
    public static Specification<Product> buildSearch(String q,
                                                     Long categoryId,
                                                     Long brandId,
                                                     BigDecimal minPrice,
                                                     BigDecimal maxPrice,
                                                     Boolean availableOnly) {
        List<Specification<Product>> specs = new ArrayList<>();

        if (q != null && !q.isBlank()) specs.add(hasTitleContaining(q));
        if (categoryId != null)        specs.add(hasCategory(categoryId));
        if (brandId != null)           specs.add(hasBrand(brandId));
        if (minPrice != null)          specs.add(hasPriceAtLeast(minPrice));
        if (maxPrice != null)          specs.add(hasPriceAtMost(maxPrice));
        if (Boolean.TRUE.equals(availableOnly)) specs.add(isAvailable());

        return specs.stream()
                .reduce(Specification.where(null), Specification::and);
    }
}
