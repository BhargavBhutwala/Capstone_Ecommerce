package com.ebookstore.order.service;

import com.ebookstore.address.entity.Address;
import com.ebookstore.address.repository.AddressRepository;
import com.ebookstore.cart.dto.CartResponse;
import com.ebookstore.cart.entity.Cart;
import com.ebookstore.cart.entity.CartItem;
import com.ebookstore.cart.repository.CartItemRepository;
import com.ebookstore.cart.repository.CartRepository;
import com.ebookstore.cart.service.CartService;
import com.ebookstore.catalog.CatalogMapper;
import com.ebookstore.catalog.entity.Product;
import com.ebookstore.catalog.repository.ProductRepository;
import com.ebookstore.common.domain.CartStatus;
import com.ebookstore.common.domain.OrderStatus;
import com.ebookstore.common.dto.PagedResponse;
import com.ebookstore.common.exception.BusinessRuleViolationException;
import com.ebookstore.common.exception.InsufficientStockException;
import com.ebookstore.common.exception.InvalidRequestException;
import com.ebookstore.common.exception.OrderCancellationNotAllowedException;
import com.ebookstore.common.exception.ResourceNotFoundException;
import com.ebookstore.order.dto.CreateOrderRequest;
import com.ebookstore.order.dto.OrderItemResponse;
import com.ebookstore.order.dto.OrderResponse;
import com.ebookstore.order.dto.ShippingAddressSnapshot;
import com.ebookstore.order.entity.Order;
import com.ebookstore.order.entity.OrderItem;
import com.ebookstore.order.repository.OrderItemRepository;
import com.ebookstore.order.repository.OrderRepository;
import com.ebookstore.user.entity.User;
import com.ebookstore.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Business logic for order creation, order history, buy-again, and cancellation.
 *
 * <p>Transaction boundaries:
 * <ul>
 *   <li>{@link #createOrder} — single {@code @Transactional} wrapping the entire checkout flow</li>
 *   <li>{@link #listOrders} — {@code @Transactional(readOnly = true)}</li>
 *   <li>{@link #getOrder} — {@code @Transactional(readOnly = true)}</li>
 *   <li>{@link #buyAgain} — {@code @Transactional}</li>
 *   <li>{@link #cancelOrder} — {@code @Transactional}</li>
 * </ul>
 *
 * <p>{@link Clock} is injected so tests can substitute a fixed clock without Thread.sleep.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final Clock clock;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        ProductRepository productRepository,
                        AddressRepository addressRepository,
                        UserRepository userRepository,
                        CartService cartService,
                        Clock clock) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.cartService = cartService;
        this.clock = clock;
    }

    // =========================================================================
    // operationId: createOrder
    // =========================================================================

    /**
     * Full checkout flow — single atomic transaction.
     *
     * <p>Steps (all-or-nothing):
     * <ol>
     *   <li>Load user's cart; reject if empty.</li>
     *   <li>Load and verify address ownership.</li>
     *   <li>Re-fetch all products by their IDs (authoritative prices).</li>
     *   <li>Validate active flag and stock for each product.</li>
     *   <li>Calculate authoritative subtotal from live {@code products.price}.</li>
     *   <li>Generate unique order number.</li>
     *   <li>Snapshot shipping address (7 flat columns) into {@code Order}.</li>
     *   <li>Create {@code Order} with status {@code PENDING_PAYMENT}.</li>
     *   <li>Create {@code OrderItem}s with title + price snapshots.</li>
     *   <li>Decrement stock.</li>
     *   <li>Clear cart items; reset cart status to {@code ACTIVE}.</li>
     *   <li>Do NOT create a Payment — that is Task 12.</li>
     * </ol>
     *
     * <p>{@code cart_items.unit_price} is NEVER used for order price calculations.
     */
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {

        // 1. Load cart — must have items
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id: " + userId));
        if (cart.getItems().isEmpty()) {
            throw new InvalidRequestException("Cannot checkout with an empty cart.");
        }

        // 2. Load and verify address ownership
        Address address = addressRepository.findByIdAndUserId(request.getAddressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found with id: " + request.getAddressId()));

        // 3. Re-fetch all products by their IDs (authoritative state)
        List<CartItem> cartItems = List.copyOf(cart.getItems());
        List<Long> productIds = cartItems.stream()
                .map(ci -> ci.getProduct().getId())
                .toList();
        List<Product> products = productRepository.findAllById(productIds);

        // Index products by id for O(1) lookup
        java.util.Map<Long, Product> productMap = new java.util.HashMap<>();
        for (Product p : products) {
            productMap.put(p.getId(), p);
        }

        // 4 & 5. Validate + calculate authoritative subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProduct().getId());
            if (product == null) {
                throw new ResourceNotFoundException(
                        "Product not found with id: " + cartItem.getProduct().getId());
            }
            if (!product.isActive()) {
                throw new BusinessRuleViolationException(
                        "Product '" + product.getTitle() + "' is no longer available.");
            }
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for product '" + product.getTitle()
                                + "': requested " + cartItem.getQuantity()
                                + ", available " + product.getStockQuantity());
            }
            // Use live product.price — NOT cart_items.unit_price
            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        // 6. MVP fixed amounts
        BigDecimal shippingAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        int giftPointsUsed = 0;
        BigDecimal totalAmount = subtotal;

        // 7. Generate unique order number
        String orderNumber = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        // 8. Snapshot shipping address (7 flat fields — no FK to addresses)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime cancellationDeadline = now.plusHours(48);

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setSubtotal(subtotal);
        order.setShippingAmount(shippingAmount);
        order.setDiscountAmount(discountAmount);
        order.setGiftPointsUsed(giftPointsUsed);
        order.setTotalAmount(totalAmount);
        order.setPlacedAt(now);
        order.setCancellationDeadline(cancellationDeadline);

        // Snapshot seven address fields — use user already loaded above for shipping_name
        String shippingName = user.getFirstName() + " " + user.getLastName();
        order.setShippingName(shippingName);
        order.setShippingLine1(address.getAddressLine1());
        order.setShippingLine2(address.getAddressLine2());
        order.setShippingCity(address.getCity());
        order.setShippingState(address.getState());
        order.setShippingPostalCode(address.getPostalCode());
        order.setShippingCountry(address.getCountry());

        order = orderRepository.save(order);

        // 9. Create OrderItems (snapshots from live product data, NOT cart_items.unit_price)
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProduct().getId());
            BigDecimal itemSubtotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductTitle(product.getTitle());   // title snapshot
            orderItem.setUnitPrice(product.getPrice());      // live price snapshot — NOT cart_items.unit_price
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotal(itemSubtotal);
            orderItems.add(orderItemRepository.save(orderItem));
        }
        order.setItems(orderItems);

        // 10. Decrement stock
        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProduct().getId());
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }

        // 11. Clear cart items + reset cart status to ACTIVE
        cartItemRepository.deleteAll(cartItems);
        cart.getItems().clear();
        cart.setStatus(CartStatus.ACTIVE);
        cartRepository.save(cart);

        // 12. No Payment created — Task 12

        log.info("Order created: orderId={}, orderNumber={}, userId={}, total={}",
                order.getId(), orderNumber, userId, totalAmount);

        return toResponse(order, orderItems);
    }

    // =========================================================================
    // operationId: listOrders
    // =========================================================================

    /**
     * Returns paginated order history for the authenticated user, always sorted
     * {@code placed_at DESC}. Client-supplied sort parameters are ignored.
     */
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> listOrders(Long userId, OrderStatus status,
                                                    int page, int size) {
        // Fixed sort: placed_at DESC — client cannot override
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "placedAt"));

        Page<Order> orderPage;
        if (status != null) {
            orderPage = orderRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            orderPage = orderRepository.findByUserId(userId, pageable);
        }

        return PagedResponse.of(orderPage.map(o -> toResponse(o, o.getItems())));
    }

    // =========================================================================
    // operationId: getOrder
    // =========================================================================

    /**
     * Returns a single order; ownership enforced via {@code findByIdAndUserId}.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + orderId));
        return toResponse(order, order.getItems());
    }

    // =========================================================================
    // operationId: buyAgain
    // =========================================================================

    /**
     * Re-adds historical order products to the user's active cart using current prices.
     *
     * <p>Rules:
     * <ul>
     *   <li>Re-fetches current product state — uses current {@code product.price},
     *       never historical {@code order_items.unit_price}.</li>
     *   <li>Skips individual products that are inactive or out of stock.</li>
     *   <li>If ALL products are unavailable, throws {@link BusinessRuleViolationException}.</li>
     *   <li>Merges quantity into the existing cart item if the product is already present.</li>
     *   <li>Reuses the existing Cart — never creates a second Cart.</li>
     * </ul>
     */
    @Transactional
    public CartResponse buyAgain(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + orderId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id: " + userId));

        int addedCount = 0;

        for (OrderItem orderItem : order.getItems()) {
            Long productId = orderItem.getProduct().getId();
            Optional<Product> productOpt = productRepository.findById(productId);

            if (productOpt.isEmpty()) {
                log.info("buyAgain: product {} no longer exists — skipping", productId);
                continue;
            }

            Product product = productOpt.get();

            if (!product.isActive() || product.getStockQuantity() <= 0) {
                log.info("buyAgain: product {} unavailable (active={}, stock={}) — skipping",
                        productId, product.isActive(), product.getStockQuantity());
                continue;
            }

            // Add/merge into cart using current product.price (NOT historical order_items.unit_price)
            int requestedQty = orderItem.getQuantity();
            int availableStock = product.getStockQuantity();

            Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(
                    cart.getId(), productId);

            if (existingItem.isPresent()) {
                CartItem ci = existingItem.get();
                int newQty = ci.getQuantity() + requestedQty;
                // Cap at current stock if combined exceeds it
                newQty = Math.min(newQty, availableStock);
                ci.setQuantity(newQty);
                cartItemRepository.save(ci);
            } else {
                // Cap quantity at available stock
                int qty = Math.min(requestedQty, availableStock);
                CartItem ci = new CartItem();
                ci.setCart(cart);
                ci.setProduct(product);
                ci.setQuantity(qty);
                ci.setUnitPrice(product.getPrice()); // current price — NOT historical
                cartItemRepository.save(ci);
                cart.getItems().add(ci);
            }
            addedCount++;
        }

        if (addedCount == 0) {
            throw new BusinessRuleViolationException(
                    "None of the products from order " + order.getOrderNumber()
                            + " are currently available.");
        }

        log.info("buyAgain: added {} products to cart for userId={}, orderId={}",
                addedCount, userId, orderId);

        // Return updated cart response via CartService
        return cartService.getCart(userId);
    }

    // =========================================================================
    // operationId: cancelOrder
    // =========================================================================

    /**
     * Cancels an eligible order within the 48-hour cancellation window.
     *
     * <p>Allowed statuses: {@code PENDING_PAYMENT}, {@code PAID}.
     * Deadline check: {@code now <= cancellationDeadline} (inclusive of exact deadline moment).
     *
     * <p>For PAID orders: restores stock and sets status to {@code CANCELLED};
     * the existing Payment record is left with {@code status = SUCCESS} (refund is Phase 2).
     */
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + orderId));

        // Status check: only PENDING_PAYMENT and PAID can be cancelled
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT
                && order.getStatus() != OrderStatus.PAID) {
            throw new OrderCancellationNotAllowedException(
                    "Order " + order.getOrderNumber() + " cannot be cancelled in status: "
                            + order.getStatus());
        }

        // Deadline check: now <= cancellationDeadline (inclusive)
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (now.isAfter(order.getCancellationDeadline())) {
            throw new OrderCancellationNotAllowedException(
                    "Cancellation deadline has passed for order " + order.getOrderNumber());
        }

        // Cancel the order
        order.setStatus(OrderStatus.CANCELLED);

        // Restore stock for each order item
        for (OrderItem orderItem : order.getItems()) {
            Product product = productRepository.findById(orderItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + orderItem.getProduct().getId()));
            product.setStockQuantity(product.getStockQuantity() + orderItem.getQuantity());
            productRepository.save(product);
        }

        // For PAID orders: leave payment.status = SUCCESS; no refund in MVP
        // OrderStatus = CANCELLED / PaymentStatus = SUCCESS is the correct MVP state

        orderRepository.save(order);
        log.info("Order cancelled: orderId={}, orderNumber={}, userId={}",
                orderId, order.getOrderNumber(), userId);

        return toResponse(order, order.getItems());
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Maps an {@link Order} and its items to an {@link OrderResponse}.
     * Uses historical snapshots from the order/order-items — not current product data.
     */
    static OrderResponse toResponse(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(OrderService::toItemResponse)
                .toList();

        ShippingAddressSnapshot addressSnapshot = new ShippingAddressSnapshot(
                order.getShippingName(),
                order.getShippingLine1(),
                order.getShippingLine2(),
                order.getShippingCity(),
                order.getShippingState(),
                order.getShippingPostalCode(),
                order.getShippingCountry()
        );

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                itemResponses,
                addressSnapshot,
                order.getSubtotal(),
                order.getShippingAmount(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getPlacedAt(),
                order.getCancellationDeadline()
        );
    }

    private static OrderItemResponse toItemResponse(OrderItem oi) {
        return new OrderItemResponse(
                oi.getId(),
                oi.getProduct().getId(),
                oi.getProductTitle(),      // historical snapshot
                oi.getQuantity(),
                oi.getUnitPrice(),         // historical snapshot
                oi.getSubtotal()
        );
    }
}
