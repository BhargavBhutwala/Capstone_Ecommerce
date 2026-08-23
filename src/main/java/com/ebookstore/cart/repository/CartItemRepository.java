package com.ebookstore.cart.repository;

import com.ebookstore.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Finds a cart item by its product within a given cart.
     * Used for merge-on-duplicate logic.
     */
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    /**
     * Finds a cart item by its id and the owning cart's user id.
     * Enforces ownership at the repository level — returns empty when the
     * item does not belong to the requesting user, preventing information
     * leakage about the resource's existence.
     */
    @Query("""
            SELECT ci FROM CartItem ci
            WHERE ci.id = :itemId
              AND ci.cart.user.id = :userId
            """)
    Optional<CartItem> findByIdAndCartUserId(@Param("itemId") Long itemId,
                                             @Param("userId") Long userId);
}
