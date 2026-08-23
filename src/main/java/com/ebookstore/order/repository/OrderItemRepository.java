package com.ebookstore.order.repository;

import com.ebookstore.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Returns the distinct category IDs for all products the given user
     * has ever purchased. Used by CartService to build purchase-history
     * based product recommendations.
     */
    @Query("""
            SELECT DISTINCT oi.product.category.id
            FROM OrderItem oi
            WHERE oi.order.user.id = :userId
            """)
    List<Long> findDistinctCategoryIdsByUserId(@Param("userId") Long userId);
}
