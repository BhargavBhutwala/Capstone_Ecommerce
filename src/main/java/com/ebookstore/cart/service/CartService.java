package com.ebookstore.cart.service;

import com.ebookstore.cart.dto.AddCartItemRequest;
import com.ebookstore.cart.dto.CartItemResponse;
import com.ebookstore.cart.dto.CartResponse;
import com.ebookstore.cart.dto.UpdateCartItemRequest;
import com.ebookstore.cart.entity.Cart;
import com.ebookstore.cart.entity.CartItem;
import com.ebookstore.cart.repository.CartItemRepository;
import com.ebookstore.cart.repository.CartRepository;
import com.ebookstore.catalog.CatalogMapper;
import com.ebookstore.catalog.dto.ProductSummary;
import com.ebookstore.catalog.entity.Product;
import com.ebookstore.catalog.repository.ProductRepository;
import com.ebookstore.common.exception.InsufficientStockException;
import com.ebookstore.common.exception.ResourceNotFoundException;
import com.ebookstore.order.repository.OrderItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business logic for cart operations.
 *
 * <p>The cart is created once at registration and is never deleted or re-created.
 * All operations on cart items are scoped to the authenticated user's cart.
 *
 * <p>Transaction boundaries:
 * <ul>
 *   <li>{@link #getCart} — {@code @Transactional(readOnly = true)}</li>
 *   <li>{@link #addCartItem} — {@code @Transactional}</li>
 *   <li>{@link #updateCartItem} — {@code @Transactional}</li>
 *   <li>{@link #removeCartItem} — {@code @Transactional}</li>
 * </ul>
 */
@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);
    private static final int RECOMMENDATION_LIMIT = 4;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository,
                       OrderItemRepository orderItemRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
    }

    // =========================================================================
    // operationId: getCart
    // =========================================================================

    /**
     * Returns the authenticated user's cart (always exists — created at registration).
     */
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        Cart cart = loadCartByUserId(userId);
        return buildCartResponse(cart, userId);
    }

    // =========================================================================
    // operationId: addCartItem
    // =========================================================================

    /**
     * Adds a product to the cart, or merges quantity if the product is already present.
     *
     * <p>Steps:
     * <ol>
     *   <li>Load the product; throw {@link ResourceNotFoundException} if absent.</li>
     *   <li>Reject inactive products or insufficient stock with {@link InsufficientStockException}.</li>
     *   <li>If the product already exists in the cart, merge (add) the quantity.
     *       Re-check total against current stock.</li>
     *   <li>If the product is new to the cart, create a {@link CartItem} with
     *       {@code unitPrice = product.price} (display snapshot — NOT authoritative for checkout).</li>
     *   <li>Save and return the updated {@link CartResponse}.</li>
     * </ol>
     */
    @Transactional
    public CartResponse addCartItem(Long userId, AddCartItemRequest request) {
        // 1. Load product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.getProductId()));

        // 2. Validate active + stock
        if (!product.isActive()) {
            throw new InsufficientStockException(
                    "Product '" + product.getTitle() + "' is not available.");
        }
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(
                    "Insufficient stock for product '" + product.getTitle()
                            + "': requested " + request.getQuantity()
                            + ", available " + product.getStockQuantity());
        }

        // 3. Load cart
        Cart cart = loadCartByUserId(userId);

        // 4. Merge or create
        Optional<CartItem> existing = cartItemRepository.findByCartIdAndProductId(
                cart.getId(), product.getId());

        if (existing.isPresent()) {
            CartItem item = existing.get();
            int newQty = item.getQuantity() + request.getQuantity();
            if (newQty > product.getStockQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for product '" + product.getTitle()
                                + "': cart already has " + item.getQuantity()
                                + ", requested " + request.getQuantity()
                                + ", available " + product.getStockQuantity());
            }
            item.setQuantity(newQty);
            cartItemRepository.save(item);
            log.info("Merged cart item: cartId={}, productId={}, newQty={}",
                    cart.getId(), product.getId(), newQty);
        } else {
            // 5. New item — set unit_price as display snapshot
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(request.getQuantity());
            item.setUnitPrice(product.getPrice()); // display snapshot only — not used at checkout
            cartItemRepository.save(item);
            log.info("Added cart item: cartId={}, productId={}, qty={}",
                    cart.getId(), product.getId(), request.getQuantity());
        }

        // Reload cart to reflect persisted state
        Cart freshCart = loadCartByUserId(userId);
        return buildCartResponse(freshCart, userId);
    }

    // =========================================================================
    // operationId: updateCartItem
    // =========================================================================

    /**
     * Updates the quantity of an existing cart item.
     *
     * <p>Ownership is enforced at the repository level via
     * {@link CartItemRepository#findByIdAndCartUserId(Long, Long)}.
     */
    @Transactional
    public CartResponse updateCartItem(Long userId, Long itemId, UpdateCartItemRequest request) {
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found with id: " + itemId));

        Product product = item.getProduct();
        if (request.getQuantity() > product.getStockQuantity()) {
            throw new InsufficientStockException(
                    "Insufficient stock for product '" + product.getTitle()
                            + "': requested " + request.getQuantity()
                            + ", available " + product.getStockQuantity());
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);
        log.info("Updated cart item: itemId={}, newQty={}", itemId, request.getQuantity());

        Cart cart = loadCartByUserId(userId);
        return buildCartResponse(cart, userId);
    }

    // =========================================================================
    // operationId: removeCartItem
    // =========================================================================

    /**
     * Removes a cart item.
     *
     * <p>Ownership is enforced at the repository level via
     * {@link CartItemRepository#findByIdAndCartUserId(Long, Long)}.
     */
    @Transactional
    public void removeCartItem(Long userId, Long itemId) {
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found with id: " + itemId));

        cartItemRepository.delete(item);
        log.info("Removed cart item: itemId={}, userId={}", itemId, userId);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Cart loadCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart not found for user id: " + userId));
    }

    /**
     * Builds a {@link CartResponse} from the given cart.
     *
     * <p>Calculates display-only subtotal/totalAmount from
     * {@code CartItem.unitPrice × quantity}. These values are NOT used
     * for authoritative checkout totals — Task 11 re-fetches product prices.
     *
     * <p>Populates {@code recommendedProducts} (up to 4) using the user's
     * historical purchase category IDs, excluding products already in the cart.
     * Returns an empty list when no qualifying products exist. No fallback to
     * global popularity.
     */
    private CartResponse buildCartResponse(Cart cart, Long userId) {
        List<CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        Set<Long> cartProductIds = new java.util.HashSet<>();

        for (CartItem ci : cart.getItems()) {
            BigDecimal itemSubtotal = ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
            subtotal = subtotal.add(itemSubtotal);
            cartProductIds.add(ci.getProduct().getId());
            itemResponses.add(new CartItemResponse(
                    ci.getId(),
                    CatalogMapper.toSummary(ci.getProduct()),
                    ci.getQuantity(),
                    ci.getUnitPrice(),
                    itemSubtotal));
        }

        // totalAmount == subtotal (no shipping/discount in cart display)
        BigDecimal totalAmount = subtotal;

        // Recommendations: purchase-history categories → active in-stock products
        List<ProductSummary> recommendations = buildRecommendations(userId, cartProductIds);

        return new CartResponse(
                cart.getId(),
                cart.getStatus(),
                itemResponses,
                subtotal,
                totalAmount,
                recommendations);
    }

    /**
     * Derives up to {@value #RECOMMENDATION_LIMIT} product recommendations for
     * the given user.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Collect distinct category IDs from the user's order history.</li>
     *   <li>If none, return empty list (no fallback).</li>
     *   <li>Query active, in-stock products in those categories, excluding
     *       products already in the cart.</li>
     *   <li>Cap at {@value #RECOMMENDATION_LIMIT}.</li>
     * </ol>
     *
     * <p>No ML, no Redis, no external service, no recommendation table.
     */
    private List<ProductSummary> buildRecommendations(Long userId, Set<Long> cartProductIds) {
        List<Long> categoryIds = orderItemRepository.findDistinctCategoryIdsByUserId(userId);
        if (categoryIds.isEmpty()) {
            return List.of();
        }

        // Ensure excludedIds list is non-empty to keep JPQL NOT IN valid
        List<Long> excludedIds = cartProductIds.isEmpty()
                ? List.of(-1L)
                : new ArrayList<>(cartProductIds);

        List<Product> candidates = productRepository.findRecommendations(
                categoryIds, excludedIds, RECOMMENDATION_LIMIT);

        return candidates.stream()
                .map(CatalogMapper::toSummary)
                .collect(Collectors.toList());
    }
}
