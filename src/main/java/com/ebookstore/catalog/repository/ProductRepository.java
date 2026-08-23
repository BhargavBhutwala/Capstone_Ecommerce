package com.ebookstore.catalog.repository;

import com.ebookstore.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    List<Product> findByCategoryIdAndActiveTrueAndIdNot(Long categoryId, Long productId);

    List<Product> findByBrandIdAndActiveTrueAndIdNot(Long brandId, Long productId);

    /**
     * Returns at most {@code limit} active, in-stock products whose category
     * is in the provided set and whose id is NOT in the excluded set.
     *
     * <p>Used by CartService to build purchase-history-based recommendations.
     * {@code excludedIds} must contain at least one value to keep the JPQL
     * {@code NOT IN} clause valid; pass a sentinel such as {@code -1L}
     * when the cart is empty.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.category.id IN :categoryIds
              AND p.active = true
              AND p.stockQuantity > 0
              AND p.id NOT IN :excludedIds
            ORDER BY p.id ASC
            LIMIT :limit
            """)
    List<Product> findRecommendations(@Param("categoryIds") List<Long> categoryIds,
                                      @Param("excludedIds") List<Long> excludedIds,
                                      @Param("limit") int limit);
}
