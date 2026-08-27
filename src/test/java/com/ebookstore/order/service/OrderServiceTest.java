package com.ebookstore.order.service;

import com.ebookstore.address.entity.Address;
import com.ebookstore.address.repository.AddressRepository;
import com.ebookstore.cart.dto.CartResponse;
import com.ebookstore.cart.entity.Cart;
import com.ebookstore.cart.entity.CartItem;
import com.ebookstore.cart.repository.CartItemRepository;
import com.ebookstore.cart.repository.CartRepository;
import com.ebookstore.cart.service.CartService;
import com.ebookstore.catalog.entity.Brand;
import com.ebookstore.catalog.entity.Category;
import com.ebookstore.catalog.entity.Product;
import com.ebookstore.catalog.repository.ProductRepository;
import com.ebookstore.common.domain.CartStatus;
import com.ebookstore.common.domain.OrderStatus;
import com.ebookstore.common.domain.PaymentMethod;
import com.ebookstore.common.domain.PaymentStatus;
import com.ebookstore.common.dto.PagedResponse;
import com.ebookstore.common.exception.BusinessRuleViolationException;
import com.ebookstore.common.exception.InsufficientStockException;
import com.ebookstore.common.exception.InvalidRequestException;
import com.ebookstore.common.exception.OrderCancellationNotAllowedException;
import com.ebookstore.common.exception.ResourceNotFoundException;
import com.ebookstore.order.dto.CreateOrderRequest;
import com.ebookstore.order.dto.OrderResponse;
import com.ebookstore.order.entity.Order;
import com.ebookstore.order.entity.OrderItem;
import com.ebookstore.order.repository.OrderItemRepository;
import com.ebookstore.order.repository.OrderRepository;
import com.ebookstore.payment.entity.Payment;
import com.ebookstore.payment.repository.PaymentRepository;
import com.ebookstore.user.entity.User;
import com.ebookstore.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OrderService}.
 * No Spring context — all dependencies are mocked with Mockito.
 * Clock is always fixed to control time-dependent behavior.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private UserRepository userRepository;
    @Mock private CartService cartService;

    private static final Long USER_ID = 10L;
    private static final Long CART_ID = 100L;
    private static final Long ORDER_ID = 500L;
    private static final Long ADDRESS_ID = 200L;
    private static final Long PRODUCT_ID = 300L;

    // Fixed clock: 2024-01-15T10:00:00Z
    private static final Instant FIXED_INSTANT = Instant.parse("2024-01-15T10:00:00Z");
    private Clock clock;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        orderService = new OrderService(
                orderRepository, orderItemRepository, cartRepository, cartItemRepository,
                productRepository, addressRepository, userRepository, cartService, clock);
    }

    // =========================================================================
    // createOrder — successful checkout
    // =========================================================================

    @Test
    void createOrder_successful_createsOrderWithPendingPaymentStatus() {
        Product product = buildProduct(PRODUCT_ID, "Spring Boot", new BigDecimal("29.99"), 10);
        Cart cart = buildCartWithItem(product, 2, new BigDecimal("25.00")); // cart price ignored
        Address address = buildAddress(ADDRESS_ID);
        User user = buildUser(USER_ID);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0); o.setId(ORDER_ID); return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> {
            OrderItem oi = inv.getArgument(0); oi.setId(1L); return oi;
        });
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(ADDRESS_ID);

        OrderResponse response = orderService.createOrder(USER_ID, req);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(response.getId()).isEqualTo(ORDER_ID);
        assertThat(response.getOrderNumber()).startsWith("ORD-");
    }

    @Test
    void createOrder_usesLiveProductPrice_notCartUnitPrice() {
        // cart has unit_price=25.00, but product.price=29.99 — checkout must use 29.99
        Product product = buildProduct(PRODUCT_ID, "Spring Boot", new BigDecimal("29.99"), 10);
        Cart cart = buildCartWithItem(product, 2, new BigDecimal("25.00")); // intentionally different
        Address address = buildAddress(ADDRESS_ID);
        User user = buildUser(USER_ID);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0); o.setId(ORDER_ID); return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(ADDRESS_ID);

        OrderResponse response = orderService.createOrder(USER_ID, req);

        // subtotal = 29.99 * 2 = 59.98 (live price), NOT 25.00 * 2 = 50.00 (cart price)
        assertThat(response.getSubtotal()).isEqualByComparingTo(new BigDecimal("59.98"));
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("59.98"));
    }

    @Test
    void createOrder_orderItemUsesLivePriceSnapshot() {
        Product product = buildProduct(PRODUCT_ID, "Clean Code", new BigDecimal("49.99"), 5);
        Cart cart = buildCartWithItem(product, 1, new BigDecimal("39.99")); // old cart price
        Address address = buildAddress(ADDRESS_ID);
        User user = buildUser(USER_ID);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0); o.setId(ORDER_ID); return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(ADDRESS_ID);

        orderService.createOrder(USER_ID, req);

        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(itemCaptor.capture());
        OrderItem saved = itemCaptor.getValue();
        assertThat(saved.getUnitPrice()).isEqualByComparingTo(new BigDecimal("49.99")); // live price
        assertThat(saved.getProductTitle()).isEqualTo("Clean Code"); // title snapshot
    }

    @Test
    void createOrder_orderItemTitleIsSnapshotted() {
        Product product = buildProduct(PRODUCT_ID, "Design Patterns", new BigDecimal("44.99"), 5);
        Cart cart = buildCartWithItem(product, 1, new BigDecimal("44.99"));
        Address address = buildAddress(ADDRESS_ID);
        User user = buildUser(USER_ID);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0); o.setId(ORDER_ID); return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        orderService.createOrder(USER_ID, new CreateOrderRequest() {{ setAddressId(ADDRESS_ID); }});

        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(captor.capture());
        assertThat(captor.getValue().getProductTitle()).isEqualTo("Design Patterns");
    }

    @Test
    void createOrder_shippingAddressSnapshot_isPersistedOnOrder() {
        Product product = buildProduct(PRODUCT_ID, "Book", new BigDecimal("10.00"), 5);
        Cart cart = buildCartWithItem(product, 1, new BigDecimal("10.00"));
        Address address = buildAddress(ADDRESS_ID);
        User user = buildUser(USER_ID);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0); o.setId(ORDER_ID); return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        OrderResponse response = orderService.createOrder(USER_ID, new CreateOrderRequest() {{ setAddressId(ADDRESS_ID); }});

        assertThat(response.getShippingAddress().getName()).isEqualTo("John Doe");
        assertThat(response.getShippingAddress().getAddressLine1()).isEqualTo("123 Main St");
        assertThat(response.getShippingAddress().getCity()).isEqualTo("Springfield");
        assertThat(response.getShippingAddress().getState()).isEqualTo("IL");
        assertThat(response.getShippingAddress().getPostalCode()).isEqualTo("62701");
        assertThat(response.getShippingAddress().getCountry()).isEqualTo("US");
    }

    @Test
    void createOrder_subtotalAndTotalsCalculatedFromLivePrice() {
        Product p1 = buildProduct(1L, "Book A", new BigDecimal("10.00"), 5);
        Product p2 = buildProduct(2L, "Book B", new BigDecimal("20.00"), 5);
        Cart cart = buildCartWithTwoItems(p1, 2, p2, 3);
        Address address = buildAddress(ADDRESS_ID);
        User user = buildUser(USER_ID);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(p1, p2));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0); o.setId(ORDER_ID); return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        OrderResponse response = orderService.createOrder(USER_ID, new CreateOrderRequest() {{ setAddressId(ADDRESS_ID); }});

        // 10*2 + 20*3 = 80
        assertThat(response.getSubtotal()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(response.getShippingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void createOrder_giftPointsAndDiscountAreZero() {
        Product product = buildProduct(PRODUCT_ID, "Book", new BigDecimal("10.00"), 5);
        Cart cart = buildCartWithItem(product, 1, new BigDecimal("10.00"));
        Address address = buildAddress(ADDRESS_ID);
        User user = buildUser(USER_ID);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0); o.setId(ORDER_ID); return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        orderService.createOrder(USER_ID, new CreateOrderRequest() {{ setAddressId(ADDRESS_ID); }});

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getGiftPointsUsed()).isEqualTo(0);
    }

    @Test
    void createOrder_stockDecremented() {
        Product product = buildProduct(PRODUCT_ID, "Book", new BigDecimal("10.00"), 5);
        Cart cart = buildCartWithItem(product, 2, new BigDecimal("10.00"));
        Address address = buildAddress(ADDRESS_ID);
        User user = buildUser(USER_ID);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0); o.setId(ORDER_ID); return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        orderService.createOrder(USER_ID, new CreateOrderRequest() {{ setAddressId(ADDRESS_ID); }});

        // Product stock should be decremented from 5 to 3 (quantity=2)
        assertThat(product.getStockQuantity()).isEqualTo(3);
        verify(productRepository).save(product);
    }

    @Test
    void createOrder_cartItemsCleared_andCartStatusRemainsActive() {
        Product product = buildProduct(PRODUCT_ID, "Book", new BigDecimal("10.00"), 5);
        Cart cart = buildCartWithItem(product, 1, new BigDecimal("10.00"));
        long originalCartId = cart.getId();
        Address address = buildAddress(ADDRESS_ID);
        User user = buildUser(USER_ID);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0); o.setId(ORDER_ID); return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        orderService.createOrder(USER_ID, new CreateOrderRequest() {{ setAddressId(ADDRESS_ID); }});

        // Cart items must be deleted
        verify(cartItemRepository).deleteAll(anyList());
        // Cart status reset to ACTIVE
        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(cartCaptor.capture());
        assertThat(cartCaptor.getValue().getStatus()).isEqualTo(CartStatus.ACTIVE);
        // Same cart id — not replaced
        assertThat(cartCaptor.getValue().getId()).isEqualTo(originalCartId);
    }

    @Test
    void createOrder_noPaymentCreated() {
        Product product = buildProduct(PRODUCT_ID, "Book", new BigDecimal("10.00"), 5);
        Cart cart = buildCartWithItem(product, 1, new BigDecimal("10.00"));
        Address address = buildAddress(ADDRESS_ID);
        User user = buildUser(USER_ID);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0); o.setId(ORDER_ID); return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        orderService.createOrder(USER_ID, new CreateOrderRequest() {{ setAddressId(ADDRESS_ID); }});

        // Must NOT interact with PaymentRepository — Task 12 only
        // (PaymentRepository is not injected into OrderService; verify no payment objects saved)
        verify(orderRepository, times(1)).save(any(Order.class));
        // No payment save calls possible since we don't have PaymentRepository mock
    }

    @Test
    void createOrder_cancellationDeadlineIs48HoursAfterPlacedAt() {
        Product product = buildProduct(PRODUCT_ID, "Book", new BigDecimal("10.00"), 5);
        Cart cart = buildCartWithItem(product, 1, new BigDecimal("10.00"));
        Address address = buildAddress(ADDRESS_ID);
        User user = buildUser(USER_ID);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0); o.setId(ORDER_ID); return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        OrderResponse response = orderService.createOrder(USER_ID, new CreateOrderRequest() {{ setAddressId(ADDRESS_ID); }});

        // With fixed clock at 2024-01-15T10:00:00Z (UTC)
        OffsetDateTime expectedPlacedAt = OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);
        assertThat(response.getPlacedAt()).isEqualTo(expectedPlacedAt);
        assertThat(response.getCancellationDeadline()).isEqualTo(expectedPlacedAt.plusHours(48));
    }

    // =========================================================================
    // createOrder — validation failures
    // =========================================================================

    @Test
    void createOrder_emptyCart_throwsInvalidRequestException() {
        Cart cart = buildEmptyCart(CART_ID);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.createOrder(USER_ID,
                new CreateOrderRequest() {{ setAddressId(ADDRESS_ID); }}))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("empty cart");
    }

    @Test
    void createOrder_addressNotFound_throwsResourceNotFoundException() {
        Product product = buildProduct(PRODUCT_ID, "Book", new BigDecimal("10.00"), 5);
        Cart cart = buildCartWithItem(product, 1, new BigDecimal("10.00"));

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(USER_ID,
                new CreateOrderRequest() {{ setAddressId(ADDRESS_ID); }}))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(ADDRESS_ID));
    }

    @Test
    void createOrder_inactiveProduct_throwsBusinessRuleViolationException() {
        Product product = buildProduct(PRODUCT_ID, "Discontinued", new BigDecimal("10.00"), 5);
        product.setActive(false);
        Cart cart = buildCartWithItem(product, 1, new BigDecimal("10.00"));
        Address address = buildAddress(ADDRESS_ID);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));

        assertThatThrownBy(() -> orderService.createOrder(USER_ID,
                new CreateOrderRequest() {{ setAddressId(ADDRESS_ID); }}))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_insufficientStock_throwsInsufficientStockException() {
        Product product = buildProduct(PRODUCT_ID, "Rare Book", new BigDecimal("10.00"), 1);
        Cart cart = buildCartWithItem(product, 3, new BigDecimal("10.00")); // requesting 3, only 1
        Address address = buildAddress(ADDRESS_ID);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));

        assertThatThrownBy(() -> orderService.createOrder(USER_ID,
                new CreateOrderRequest() {{ setAddressId(ADDRESS_ID); }}))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");

        verify(orderRepository, never()).save(any());
    }

    // =========================================================================
    // listOrders
    // =========================================================================

    @Test
    void listOrders_returnsPagedResponse_sortedByPlacedAtDesc() {
        Order o1 = buildOrder(ORDER_ID, OrderStatus.PENDING_PAYMENT,
                OffsetDateTime.now(clock).minusHours(2));
        Order o2 = buildOrder(ORDER_ID + 1, OrderStatus.PAID,
                OffsetDateTime.now(clock).minusHours(1));

        PageImpl<Order> page = new PageImpl<>(List.of(o2, o1));
        when(orderRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(page);

        PagedResponse<OrderResponse> result = orderService.listOrders(USER_ID, null, 0, 20);

        assertThat(result.getContent()).hasSize(2);

        // Verify that sort is DESC on placedAt
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findByUserId(eq(USER_ID), pageableCaptor.capture());
        Sort.Order sortOrder = pageableCaptor.getValue().getSort().getOrderFor("placedAt");
        assertThat(sortOrder).isNotNull();
        assertThat(sortOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void listOrders_withStatusFilter_usesFilteredQuery() {
        PageImpl<Order> page = new PageImpl<>(List.of());
        when(orderRepository.findByUserIdAndStatus(eq(USER_ID), eq(OrderStatus.PAID), any(Pageable.class)))
                .thenReturn(page);

        orderService.listOrders(USER_ID, OrderStatus.PAID, 0, 20);

        verify(orderRepository).findByUserIdAndStatus(eq(USER_ID), eq(OrderStatus.PAID), any(Pageable.class));
        verify(orderRepository, never()).findByUserId(eq(USER_ID), any(Pageable.class));
    }

    // =========================================================================
    // getOrder — ownership
    // =========================================================================

    @Test
    void getOrder_ownOrder_returnsOrderResponse() {
        Order order = buildOrder(ORDER_ID, OrderStatus.PENDING_PAYMENT, OffsetDateTime.now(clock));
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(USER_ID, ORDER_ID);

        assertThat(response.getId()).isEqualTo(ORDER_ID);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void getOrder_anotherUsersOrder_throwsResourceNotFoundException() {
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(USER_ID, ORDER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(ORDER_ID));
    }

    // =========================================================================
    // buyAgain
    // =========================================================================

    @Test
    void buyAgain_allAvailable_addsToCartUsingCurrentPrice() {
        Product product = buildProduct(PRODUCT_ID, "Book", new BigDecimal("29.99"), 5);
        Order order = buildOrderWithItems(ORDER_ID, product, 2, new BigDecimal("19.99")); // historical price
        Cart cart = buildEmptyCart(CART_ID);
        CartResponse cartResponse = buildCartResponse();

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(CART_ID, PRODUCT_ID)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartService.getCart(USER_ID)).thenReturn(cartResponse);

        CartResponse result = orderService.buyAgain(USER_ID, ORDER_ID);

        // Verify cart item uses current product price, not historical
        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(captor.getValue().getUnitPrice()).isEqualByComparingTo(new BigDecimal("29.99"));
        assertThat(result).isEqualTo(cartResponse);
    }

    @Test
    void buyAgain_someUnavailable_skipsUnavailableAddsAvailable() {
        Product available = buildProduct(PRODUCT_ID, "Available Book", new BigDecimal("10.00"), 5);
        Product unavailable = buildProduct(PRODUCT_ID + 1, "Unavailable Book", new BigDecimal("15.00"), 0);
        Order order = buildOrderWithTwoItems(ORDER_ID, available, unavailable);
        Cart cart = buildEmptyCart(CART_ID);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(available));
        when(productRepository.findById(PRODUCT_ID + 1)).thenReturn(Optional.of(unavailable));
        when(cartItemRepository.findByCartIdAndProductId(CART_ID, PRODUCT_ID)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartService.getCart(USER_ID)).thenReturn(buildCartResponse());

        orderService.buyAgain(USER_ID, ORDER_ID);

        // Only 1 cart item saved (available product), not 2
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }

    @Test
    void buyAgain_allUnavailable_throwsBusinessRuleViolationException() {
        Product product = buildProduct(PRODUCT_ID, "Out of Stock", new BigDecimal("10.00"), 0);
        Order order = buildOrderWithItems(ORDER_ID, product, 1, new BigDecimal("10.00"));

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(buildEmptyCart(CART_ID)));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.buyAgain(USER_ID, ORDER_ID))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void buyAgain_historicalPriceNotUsed() {
        // Historical order_item.unitPrice = 19.99; current product.price = 35.00
        Product product = buildProduct(PRODUCT_ID, "Book", new BigDecimal("35.00"), 5);
        Order order = buildOrderWithItems(ORDER_ID, product, 1, new BigDecimal("19.99"));
        Cart cart = buildEmptyCart(CART_ID);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(CART_ID, PRODUCT_ID)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartService.getCart(USER_ID)).thenReturn(buildCartResponse());

        orderService.buyAgain(USER_ID, ORDER_ID);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        // Must use CURRENT price (35.00), NOT historical (19.99)
        assertThat(captor.getValue().getUnitPrice()).isEqualByComparingTo(new BigDecimal("35.00"));
    }

    // =========================================================================
    // cancelOrder
    // =========================================================================

    @Test
    void cancelOrder_pendingPayment_withinDeadline_cancels() {
        Product product = buildProduct(PRODUCT_ID, "Book", new BigDecimal("10.00"), 5);
        Order order = buildCancellableOrder(ORDER_ID, OrderStatus.PENDING_PAYMENT, product, 2);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(orderRepository.save(order)).thenReturn(order);

        OrderResponse response = orderService.cancelOrder(USER_ID, ORDER_ID);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getStockQuantity()).isEqualTo(7); // 5 + 2 restored
    }

    @Test
    void cancelOrder_paid_withinDeadline_cancelsAndRestoresStock() {
        Product product = buildProduct(PRODUCT_ID, "Book", new BigDecimal("10.00"), 5);
        Order order = buildCancellableOrder(ORDER_ID, OrderStatus.PAID, product, 1);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(orderRepository.save(order)).thenReturn(order);

        OrderResponse response = orderService.cancelOrder(USER_ID, ORDER_ID);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getStockQuantity()).isEqualTo(6); // 5 + 1 restored
    }

    @Test
    void cancelOrder_paidOrder_paymentStatusUnchanged() {
        // Payment is not touched by cancellation — PaymentStatus stays SUCCESS
        Product product = buildProduct(PRODUCT_ID, "Book", new BigDecimal("10.00"), 3);
        Order order = buildCancellableOrder(ORDER_ID, OrderStatus.PAID, product, 1);
        // Note: No PaymentRepository is injected in OrderService — cancellation doesn't touch it

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(orderRepository.save(order)).thenReturn(order);

        OrderResponse response = orderService.cancelOrder(USER_ID, ORDER_ID);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        // No payment repository interactions — payment stays at SUCCESS (correct MVP state)
    }

    @Test
    void cancelOrder_exactlyAtDeadline_isAllowed() {
        // Deadline = fixed clock time (now == deadline → allowed)
        Product product = buildProduct(PRODUCT_ID, "Book", new BigDecimal("10.00"), 5);
        OffsetDateTime deadline = OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);
        Order order = buildCancellableOrderWithDeadline(ORDER_ID, OrderStatus.PENDING_PAYMENT, product, 1, deadline);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(orderRepository.save(order)).thenReturn(order);

        // Should NOT throw — exactly at deadline is allowed (now <= deadline)
        OrderResponse response = orderService.cancelOrder(USER_ID, ORDER_ID);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_afterDeadline_throwsOrderCancellationNotAllowedException() {
        // Deadline is 1 second before now (clock is fixed at FIXED_INSTANT)
        Product product = buildProduct(PRODUCT_ID, "Book", new BigDecimal("10.00"), 5);
        OffsetDateTime deadlinePast = OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC).minusSeconds(1);
        Order order = buildCancellableOrderWithDeadline(ORDER_ID, OrderStatus.PENDING_PAYMENT, product, 1, deadlinePast);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(USER_ID, ORDER_ID))
                .isInstanceOf(OrderCancellationNotAllowedException.class)
                .hasMessageContaining("deadline");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_confirmedStatus_throwsOrderCancellationNotAllowedException() {
        OffsetDateTime futureDeadline = OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC).plusHours(47);
        Order order = buildCancellableOrderNoItems(ORDER_ID, OrderStatus.CONFIRMED, futureDeadline);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(USER_ID, ORDER_ID))
                .isInstanceOf(OrderCancellationNotAllowedException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_stockRestoredForAllItems() {
        Product p1 = buildProduct(1L, "Book A", new BigDecimal("10.00"), 3);
        Product p2 = buildProduct(2L, "Book B", new BigDecimal("20.00"), 4);
        Order order = buildCancellableOrderTwoItems(ORDER_ID, p1, 2, p2, 3);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(productRepository.findById(1L)).thenReturn(Optional.of(p1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(p2));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.cancelOrder(USER_ID, ORDER_ID);

        assertThat(p1.getStockQuantity()).isEqualTo(5); // 3 + 2
        assertThat(p2.getStockQuantity()).isEqualTo(7); // 4 + 3
    }

    // =========================================================================
    // Builders / helpers
    // =========================================================================

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        return user;
    }

    private Product buildProduct(Long id, String title, BigDecimal price, int stock) {
        Category category = new Category();
        category.setId(1L);
        category.setName("Test Category");
        Brand brand = new Brand();
        brand.setId(1L);
        brand.setName("Test Brand");
        Product p = new Product();
        p.setId(id);
        p.setTitle(title);
        p.setPrice(price);
        p.setStockQuantity(stock);
        p.setActive(true);
        p.setCategory(category);
        p.setBrand(brand);
        return p;
    }

    private Address buildAddress(Long id) {
        User user = buildUser(USER_ID);
        Address a = new Address();
        a.setId(id);
        a.setUser(user);
        a.setAddressLine1("123 Main St");
        a.setAddressLine2(null);
        a.setCity("Springfield");
        a.setState("IL");
        a.setPostalCode("62701");
        a.setCountry("US");
        a.setDefault(false);
        return a;
    }

    private Cart buildEmptyCart(Long id) {
        Cart cart = new Cart();
        cart.setId(id);
        cart.setStatus(CartStatus.ACTIVE);
        return cart;
    }

    private Cart buildCartWithItem(Product product, int qty, BigDecimal unitPrice) {
        Cart cart = buildEmptyCart(CART_ID);
        CartItem ci = new CartItem();
        ci.setId(1L);
        ci.setCart(cart);
        ci.setProduct(product);
        ci.setQuantity(qty);
        ci.setUnitPrice(unitPrice);
        cart.getItems().add(ci);
        return cart;
    }

    private Cart buildCartWithTwoItems(Product p1, int q1, Product p2, int q2) {
        Cart cart = buildEmptyCart(CART_ID);
        CartItem ci1 = new CartItem();
        ci1.setId(1L); ci1.setCart(cart); ci1.setProduct(p1); ci1.setQuantity(q1);
        ci1.setUnitPrice(p1.getPrice());
        CartItem ci2 = new CartItem();
        ci2.setId(2L); ci2.setCart(cart); ci2.setProduct(p2); ci2.setQuantity(q2);
        ci2.setUnitPrice(p2.getPrice());
        cart.getItems().add(ci1);
        cart.getItems().add(ci2);
        return cart;
    }

    private Order buildOrder(Long id, OrderStatus status, OffsetDateTime placedAt) {
        Order o = new Order();
        o.setId(id);
        o.setOrderNumber("ORD-TEST" + id);
        o.setStatus(status);
        o.setSubtotal(BigDecimal.TEN);
        o.setShippingAmount(BigDecimal.ZERO);
        o.setDiscountAmount(BigDecimal.ZERO);
        o.setGiftPointsUsed(0);
        o.setTotalAmount(BigDecimal.TEN);
        o.setPlacedAt(placedAt);
        o.setCancellationDeadline(placedAt.plusHours(48));
        o.setShippingName("Test User");
        o.setShippingLine1("123 Test St");
        o.setShippingLine2(null);
        o.setShippingCity("City");
        o.setShippingState("ST");
        o.setShippingPostalCode("12345");
        o.setShippingCountry("US");
        return o;
    }

    private Order buildOrderWithItems(Long id, Product product, int qty, BigDecimal historicalPrice) {
        Order order = buildOrder(id, OrderStatus.PAID, OffsetDateTime.now(clock).minusHours(1));
        OrderItem oi = new OrderItem();
        oi.setId(1L);
        oi.setOrder(order);
        oi.setProduct(product);
        oi.setProductTitle(product.getTitle());
        oi.setQuantity(qty);
        oi.setUnitPrice(historicalPrice);
        oi.setSubtotal(historicalPrice.multiply(BigDecimal.valueOf(qty)));
        order.setItems(new ArrayList<>(List.of(oi)));
        return order;
    }

    private Order buildOrderWithTwoItems(Long id, Product p1, Product p2) {
        Order order = buildOrder(id, OrderStatus.PAID, OffsetDateTime.now(clock).minusHours(1));
        OrderItem oi1 = new OrderItem();
        oi1.setId(1L); oi1.setOrder(order); oi1.setProduct(p1); oi1.setProductTitle(p1.getTitle());
        oi1.setQuantity(1); oi1.setUnitPrice(p1.getPrice());
        oi1.setSubtotal(p1.getPrice());
        OrderItem oi2 = new OrderItem();
        oi2.setId(2L); oi2.setOrder(order); oi2.setProduct(p2); oi2.setProductTitle(p2.getTitle());
        oi2.setQuantity(1); oi2.setUnitPrice(p2.getPrice());
        oi2.setSubtotal(p2.getPrice());
        order.setItems(new ArrayList<>(List.of(oi1, oi2)));
        return order;
    }

    private Order buildCancellableOrder(Long id, OrderStatus status, Product product, int qty) {
        OffsetDateTime deadline = OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC).plusHours(1);
        return buildCancellableOrderWithDeadline(id, status, product, qty, deadline);
    }

    private Order buildCancellableOrderWithDeadline(Long id, OrderStatus status, Product product,
                                                     int qty, OffsetDateTime deadline) {
        Order order = buildOrder(id, status, OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC).minusHours(1));
        order.setCancellationDeadline(deadline);
        OrderItem oi = new OrderItem();
        oi.setId(1L);
        oi.setOrder(order);
        oi.setProduct(product);
        oi.setProductTitle(product.getTitle());
        oi.setQuantity(qty);
        oi.setUnitPrice(product.getPrice());
        oi.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(qty)));
        order.setItems(new ArrayList<>(List.of(oi)));
        return order;
    }

    private Order buildCancellableOrderNoItems(Long id, OrderStatus status, OffsetDateTime deadline) {
        Order order = buildOrder(id, status, OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC).minusHours(1));
        order.setCancellationDeadline(deadline);
        order.setItems(new ArrayList<>());
        return order;
    }

    private Order buildCancellableOrderTwoItems(Long id, Product p1, int q1, Product p2, int q2) {
        OffsetDateTime deadline = OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC).plusHours(1);
        Order order = buildOrder(id, OrderStatus.PENDING_PAYMENT,
                OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC).minusHours(1));
        order.setCancellationDeadline(deadline);
        OrderItem oi1 = new OrderItem();
        oi1.setId(1L); oi1.setOrder(order); oi1.setProduct(p1); oi1.setProductTitle(p1.getTitle());
        oi1.setQuantity(q1); oi1.setUnitPrice(p1.getPrice());
        oi1.setSubtotal(p1.getPrice().multiply(BigDecimal.valueOf(q1)));
        OrderItem oi2 = new OrderItem();
        oi2.setId(2L); oi2.setOrder(order); oi2.setProduct(p2); oi2.setProductTitle(p2.getTitle());
        oi2.setQuantity(q2); oi2.setUnitPrice(p2.getPrice());
        oi2.setSubtotal(p2.getPrice().multiply(BigDecimal.valueOf(q2)));
        order.setItems(new ArrayList<>(List.of(oi1, oi2)));
        return order;
    }

    private CartResponse buildCartResponse() {
        return new CartResponse(CART_ID, CartStatus.ACTIVE, List.of(),
                BigDecimal.ZERO, BigDecimal.ZERO, List.of());
    }
}
