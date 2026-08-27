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
import com.ebookstore.common.exception.InsufficientStockException;
import com.ebookstore.order.dto.CreateOrderRequest;
import com.ebookstore.order.dto.OrderResponse;
import com.ebookstore.order.entity.Order;
import com.ebookstore.order.entity.OrderItem;
import com.ebookstore.order.repository.OrderItemRepository;
import com.ebookstore.order.repository.OrderRepository;
import com.ebookstore.payment.dto.PaymentResponse;
import com.ebookstore.payment.entity.Payment;
import com.ebookstore.payment.processor.PaymentProcessor;
import com.ebookstore.payment.repository.PaymentRepository;
import com.ebookstore.payment.service.PaymentService;
import com.ebookstore.user.entity.User;
import com.ebookstore.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
 * Focused tests for Defect 1 (timestamp / IST) and Defect 2 (stock decrement).
 *
 * Stock tests (A1-A8):
 *   A1 – single product decrement (initial 10, qty 2 → 8)
 *   A2 – quantity correctness (proves decrement uses cart quantity, not hard-coded 1)
 *   A3 – multiple products each decremented independently
 *   A4 – insufficient stock leaves stock unchanged
 *   A5 – exact-stock purchase results in stock = 0
 *   A6 – transaction safety: stock not persisted when order save throws
 *   A7 – payment does not decrement stock a second time
 *   A8 – order-item quantities and price snapshots remain correct after decrement
 *
 * Time tests (B9-B14):
 *   B9  – placedAt equals the injected fixed Clock instant (UTC, as OffsetDateTime)
 *   B10 – paidAt equals the injected fixed Clock instant
 *   B11 – cancellationDeadline = placedAt + 48 h exactly
 *   B12 – paidAt reflects payment-time clock, not placedAt
 *   B13 – DTO carries correct OffsetDateTime for placedAt and cancellationDeadline
 *   B14 – paidAt in PaymentResponse carries correct OffsetDateTime
 */
@ExtendWith(MockitoExtension.class)
class StockAndTimestampTest {

    // ── OrderService mocks ────────────────────────────────────────────────────
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private UserRepository userRepository;
    @Mock private CartService cartService;

    // ── PaymentService mocks ──────────────────────────────────────────────────
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentProcessor paymentProcessor;

    private static final Long USER_ID    = 10L;
    private static final Long CART_ID    = 100L;
    private static final Long ORDER_ID   = 500L;
    private static final Long ADDRESS_ID = 200L;

    // Two fixed instants: order time and (later) payment time
    private static final Instant ORDER_INSTANT   = Instant.parse("2024-06-15T08:00:00Z");
    private static final Instant PAYMENT_INSTANT = Instant.parse("2024-06-15T09:30:00Z");

    private Clock orderClock;
    private Clock paymentClock;
    private OrderService  orderService;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        orderClock   = Clock.fixed(ORDER_INSTANT,   ZoneOffset.UTC);
        paymentClock = Clock.fixed(PAYMENT_INSTANT, ZoneOffset.UTC);

        orderService = new OrderService(
                orderRepository, orderItemRepository, cartRepository, cartItemRepository,
                productRepository, addressRepository, userRepository, cartService, orderClock);

        paymentService = new PaymentService(
                paymentRepository, orderRepository, paymentProcessor, paymentClock);
    }

    // =========================================================================
    // A1 – single product: initial 10, qty 2 → 8
    // =========================================================================

    @Test
    void stock_A1_singleProduct_initialTen_quantityTwo_resultEight() {
        Product product = buildProduct(1L, "Book A", new BigDecimal("100.00"), 10);
        Cart cart = buildCartWithItem(product, 2);
        setUpOrderMocks(product, cart);

        orderService.createOrder(USER_ID, request());

        assertThat(product.getStockQuantity()).isEqualTo(8);
        verify(productRepository).save(product);
    }

    // =========================================================================
    // A2 – quantity correctness: decrement uses ordered quantity, not 1
    // =========================================================================

    @Test
    void stock_A2_quantityCorrectness_decrement_usesCartQuantity_notHardCoded1() {
        Product product = buildProduct(1L, "Book A", new BigDecimal("50.00"), 20);
        Cart cart = buildCartWithItem(product, 7);  // qty=7
        setUpOrderMocks(product, cart);

        orderService.createOrder(USER_ID, request());

        // Must decrement by 7, not by 1
        assertThat(product.getStockQuantity()).isEqualTo(13);
    }

    // =========================================================================
    // A3 – multiple products each decremented independently
    // =========================================================================

    @Test
    void stock_A3_multipleProducts_eachDecrementedIndependently() {
        Product pA = buildProduct(1L, "Book A", new BigDecimal("10.00"), 10);
        Product pB = buildProduct(2L, "Book B", new BigDecimal("20.00"), 7);
        Cart cart = buildCartWithTwoItems(pA, 2, pB, 3);

        Address address = buildAddress();
        User user = buildUser();

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(pA, pB));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0); o.setId(ORDER_ID); return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        orderService.createOrder(USER_ID, request());

        // A: 10 - 2 = 8;  B: 7 - 3 = 4
        assertThat(pA.getStockQuantity()).isEqualTo(8);
        assertThat(pB.getStockQuantity()).isEqualTo(4);
        verify(productRepository).save(pA);
        verify(productRepository).save(pB);
    }

    // =========================================================================
    // A4 – insufficient stock: stock unchanged, no negative
    // =========================================================================

    @Test
    void stock_A4_insufficientStock_stockUnchanged_noNegativeStock() {
        Product product = buildProduct(1L, "Rare Book", new BigDecimal("10.00"), 3);
        Cart cart = buildCartWithItem(product, 5);  // requesting 5, only 3

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID))
                .thenReturn(Optional.of(buildAddress()));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, request()))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");

        // Stock must not change
        assertThat(product.getStockQuantity()).isEqualTo(3);
        verify(productRepository, never()).save(product);
        verify(orderRepository, never()).save(any());
    }

    // =========================================================================
    // A5 – exact-stock purchase: stock = 0 after checkout
    // =========================================================================

    @Test
    void stock_A5_exactStockPurchase_resultIsZero() {
        Product product = buildProduct(1L, "Last Copy", new BigDecimal("20.00"), 3);
        Cart cart = buildCartWithItem(product, 3);  // exactly 3
        setUpOrderMocks(product, cart);

        orderService.createOrder(USER_ID, request());

        assertThat(product.getStockQuantity()).isEqualTo(0);
        verify(productRepository).save(product);
    }

    // =========================================================================
    // A6 – transaction safety: if orderRepository.save throws, productRepository.save
    //      is never reached (stock decrement happens AFTER order creation in the flow,
    //      but within the same @Transactional — verify no partial save occurs when
    //      order save fails before decrement begins)
    // =========================================================================

    @Test
    void stock_A6_transactionSafety_orderSaveThrows_stockNotSaved() {
        Product product = buildProduct(1L, "Book", new BigDecimal("10.00"), 10);
        Cart cart = buildCartWithItem(product, 1);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID))
                .thenReturn(Optional.of(buildAddress()));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser()));
        // Simulate order save failure
        when(orderRepository.save(any(Order.class)))
                .thenThrow(new RuntimeException("DB error during order save"));

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, request()))
                .isInstanceOf(RuntimeException.class);

        // Stock update (productRepository.save) must not have been called
        verify(productRepository, never()).save(any(Product.class));
        // Stock value unchanged
        assertThat(product.getStockQuantity()).isEqualTo(10);
    }

    // =========================================================================
    // A7 – payment does not decrement stock a second time
    // =========================================================================

    @Test
    void stock_A7_paymentDoesNotDecrementStock() {
        // After order creation, product stock = 8 (started at 10, ordered 2)
        Product product = buildProduct(1L, "Book", new BigDecimal("50.00"), 8);

        // Set up a PENDING_PAYMENT order for payment initiation
        Order order = buildOrder(ORDER_ID, OrderStatus.PENDING_PAYMENT, product, 2);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatusIn(eq(ORDER_ID), anyList())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(999L);
            return p;
        });
        when(paymentProcessor.process(any(Payment.class))).thenReturn(PaymentStatus.SUCCESS);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        paymentService.initiatePayment(USER_ID,
                new com.ebookstore.payment.dto.CreatePaymentRequest(ORDER_ID, PaymentMethod.CREDIT_CARD));

        // PaymentService must never touch ProductRepository
        verify(productRepository, never()).save(any(Product.class));
        verify(productRepository, never()).findById(any());
        // Product stock remains at 8 — unchanged by payment
        assertThat(product.getStockQuantity()).isEqualTo(8);
    }

    // =========================================================================
    // A8 – order-item quantities and price snapshots correct after decrement
    // =========================================================================

    @Test
    void stock_A8_orderItemQuantityAndPriceSnapshotCorrectAfterDecrement() {
        Product product = buildProduct(1L, "Snapshot Book", new BigDecimal("49.99"), 10);
        Cart cart = buildCartWithItem(product, 3);
        setUpOrderMocks(product, cart);

        orderService.createOrder(USER_ID, request());

        // Stock decremented
        assertThat(product.getStockQuantity()).isEqualTo(7);

        // OrderItem saved with correct snapshot
        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(itemCaptor.capture());
        OrderItem oi = itemCaptor.getValue();
        assertThat(oi.getQuantity()).isEqualTo(3);
        assertThat(oi.getUnitPrice()).isEqualByComparingTo(new BigDecimal("49.99"));
        assertThat(oi.getProductTitle()).isEqualTo("Snapshot Book");
        assertThat(oi.getSubtotal()).isEqualByComparingTo(new BigDecimal("149.97")); // 49.99 * 3
    }

    // =========================================================================
    // B9 – placedAt equals the injected fixed Clock instant (UTC OffsetDateTime)
    // =========================================================================

    @Test
    void time_B9_placedAt_equalsFixedClockInstant() {
        Product product = buildProduct(1L, "Book", new BigDecimal("10.00"), 5);
        Cart cart = buildCartWithItem(product, 1);
        setUpOrderMocks(product, cart);

        OrderResponse response = orderService.createOrder(USER_ID, request());

        OffsetDateTime expected = OffsetDateTime.ofInstant(ORDER_INSTANT, ZoneOffset.UTC);
        assertThat(response.getPlacedAt()).isEqualTo(expected);
        // Verify it carries UTC offset information (not a timezone-less LocalDateTime)
        assertThat(response.getPlacedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    // =========================================================================
    // B10 – paidAt equals the injected fixed payment Clock instant
    // =========================================================================

    @Test
    void time_B10_paidAt_equalsFixedPaymentClockInstant() {
        Product product = buildProduct(1L, "Book", new BigDecimal("50.00"), 5);
        Order order = buildOrder(ORDER_ID, OrderStatus.PENDING_PAYMENT, product, 1);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatusIn(eq(ORDER_ID), anyList())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(999L);
            return p;
        });
        when(paymentProcessor.process(any(Payment.class))).thenReturn(PaymentStatus.SUCCESS);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        PaymentResponse response = paymentService.initiatePayment(USER_ID,
                new com.ebookstore.payment.dto.CreatePaymentRequest(ORDER_ID, PaymentMethod.CREDIT_CARD));

        OffsetDateTime expected = OffsetDateTime.ofInstant(PAYMENT_INSTANT, ZoneOffset.UTC);
        assertThat(response.getPaidAt()).isEqualTo(expected);
        assertThat(response.getPaidAt().getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    // =========================================================================
    // B11 – cancellationDeadline = placedAt + 48 h exactly
    // =========================================================================

    @Test
    void time_B11_cancellationDeadline_isPlacedAtPlusFourtyEightHours() {
        Product product = buildProduct(1L, "Book", new BigDecimal("10.00"), 5);
        Cart cart = buildCartWithItem(product, 1);
        setUpOrderMocks(product, cart);

        OrderResponse response = orderService.createOrder(USER_ID, request());

        OffsetDateTime expectedPlacedAt = OffsetDateTime.ofInstant(ORDER_INSTANT, ZoneOffset.UTC);
        assertThat(response.getCancellationDeadline())
                .isEqualTo(expectedPlacedAt.plusHours(48));
        // Explicitly: placedAt + 48h = cancellationDeadline
        assertThat(response.getCancellationDeadline().toInstant())
                .isEqualTo(ORDER_INSTANT.plusSeconds(48 * 3600));
    }

    // =========================================================================
    // B12 – paidAt reflects payment-time clock; different from placedAt
    // =========================================================================

    @Test
    void time_B12_paidAt_reflectsPaymentTime_differentFromPlacedAt() {
        // ORDER_INSTANT (08:00Z) ≠ PAYMENT_INSTANT (09:30Z)
        assertThat(ORDER_INSTANT).isNotEqualTo(PAYMENT_INSTANT);

        Product product = buildProduct(1L, "Book", new BigDecimal("50.00"), 5);
        Order order = buildOrder(ORDER_ID, OrderStatus.PENDING_PAYMENT, product, 1);
        // Simulate that placedAt was set at order-creation time (ORDER_INSTANT)
        order.setPlacedAt(OffsetDateTime.ofInstant(ORDER_INSTANT, ZoneOffset.UTC));

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatusIn(eq(ORDER_ID), anyList())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(999L);
            return p;
        });
        when(paymentProcessor.process(any(Payment.class))).thenReturn(PaymentStatus.SUCCESS);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        PaymentResponse payResponse = paymentService.initiatePayment(USER_ID,
                new com.ebookstore.payment.dto.CreatePaymentRequest(ORDER_ID, PaymentMethod.CREDIT_CARD));

        // paidAt = PAYMENT_INSTANT (09:30Z)
        OffsetDateTime expectedPaidAt = OffsetDateTime.ofInstant(PAYMENT_INSTANT, ZoneOffset.UTC);
        assertThat(payResponse.getPaidAt()).isEqualTo(expectedPaidAt);
        // paidAt ≠ placedAt
        assertThat(payResponse.getPaidAt()).isNotEqualTo(order.getPlacedAt());
    }

    // =========================================================================
    // B13 – DTO carries correct OffsetDateTime for placedAt and cancellationDeadline
    // =========================================================================

    @Test
    void time_B13_orderResponseDTO_carriesCorrectOffsetDateTime() {
        Product product = buildProduct(1L, "Book", new BigDecimal("10.00"), 5);
        Cart cart = buildCartWithItem(product, 1);
        setUpOrderMocks(product, cart);

        OrderResponse response = orderService.createOrder(USER_ID, request());

        // placedAt must be an OffsetDateTime at UTC
        assertThat(response.getPlacedAt()).isNotNull();
        assertThat(response.getPlacedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(response.getPlacedAt().toInstant()).isEqualTo(ORDER_INSTANT);

        // cancellationDeadline must also be OffsetDateTime at UTC
        assertThat(response.getCancellationDeadline()).isNotNull();
        assertThat(response.getCancellationDeadline().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(response.getCancellationDeadline().toInstant())
                .isEqualTo(ORDER_INSTANT.plusSeconds(48 * 3600));
    }

    // =========================================================================
    // B14 – PaymentResponse carries correct OffsetDateTime for paidAt
    // =========================================================================

    @Test
    void time_B14_paymentResponseDTO_paidAt_carriesCorrectOffsetDateTime() {
        Product product = buildProduct(1L, "Book", new BigDecimal("50.00"), 5);
        Order order = buildOrder(ORDER_ID, OrderStatus.PENDING_PAYMENT, product, 1);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatusIn(eq(ORDER_ID), anyList())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(999L);
            return p;
        });
        when(paymentProcessor.process(any(Payment.class))).thenReturn(PaymentStatus.SUCCESS);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        PaymentResponse response = paymentService.initiatePayment(USER_ID,
                new com.ebookstore.payment.dto.CreatePaymentRequest(ORDER_ID, PaymentMethod.CREDIT_CARD));

        assertThat(response.getPaidAt()).isNotNull();
        // Must carry UTC offset — frontend new Date("...Z") parses as UTC instant
        assertThat(response.getPaidAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(response.getPaidAt().toInstant()).isEqualTo(PAYMENT_INSTANT);
    }

    // =========================================================================
    // Builders / helpers
    // =========================================================================

    private CreateOrderRequest request() {
        CreateOrderRequest r = new CreateOrderRequest();
        r.setAddressId(ADDRESS_ID);
        return r;
    }

    /** Wires standard mocks for a single-product, single-item checkout. */
    private void setUpOrderMocks(Product product, Cart cart) {
        Address address = buildAddress();
        User user = buildUser();

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(ORDER_ID);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
    }

    private Product buildProduct(Long id, String title, BigDecimal price, int stock) {
        Category cat = new Category(); cat.setId(1L); cat.setName("Cat");
        Brand brand = new Brand(); brand.setId(1L); brand.setName("Brand");
        Product p = new Product();
        p.setId(id);
        p.setTitle(title);
        p.setPrice(price);
        p.setStockQuantity(stock);
        p.setActive(true);
        p.setCategory(cat);
        p.setBrand(brand);
        return p;
    }

    private Cart buildCartWithItem(Product product, int qty) {
        Cart cart = new Cart();
        cart.setId(CART_ID);
        cart.setStatus(CartStatus.ACTIVE);
        CartItem ci = new CartItem();
        ci.setId(1L);
        ci.setCart(cart);
        ci.setProduct(product);
        ci.setQuantity(qty);
        ci.setUnitPrice(product.getPrice());
        cart.getItems().add(ci);
        return cart;
    }

    private Cart buildCartWithTwoItems(Product p1, int q1, Product p2, int q2) {
        Cart cart = new Cart();
        cart.setId(CART_ID);
        cart.setStatus(CartStatus.ACTIVE);
        CartItem ci1 = new CartItem();
        ci1.setId(1L); ci1.setCart(cart); ci1.setProduct(p1); ci1.setQuantity(q1); ci1.setUnitPrice(p1.getPrice());
        CartItem ci2 = new CartItem();
        ci2.setId(2L); ci2.setCart(cart); ci2.setProduct(p2); ci2.setQuantity(q2); ci2.setUnitPrice(p2.getPrice());
        cart.getItems().add(ci1);
        cart.getItems().add(ci2);
        return cart;
    }

    private Address buildAddress() {
        Address a = new Address();
        a.setId(ADDRESS_ID);
        a.setUser(buildUser());
        a.setAddressLine1("1 Test St");
        a.setCity("City");
        a.setState("ST");
        a.setPostalCode("12345");
        a.setCountry("IN");
        a.setDefault(false);
        return a;
    }

    private User buildUser() {
        User u = new User();
        u.setId(USER_ID);
        u.setFirstName("Test");
        u.setLastName("User");
        u.setEmail("test@example.com");
        return u;
    }

    /**
     * Build a minimal Order with one OrderItem, suitable for PaymentService tests.
     */
    private Order buildOrder(Long id, OrderStatus status, Product product, int qty) {
        User user = buildUser();
        Order o = new Order();
        o.setId(id);
        o.setOrderNumber("ORD-TEST" + id);
        o.setStatus(status);
        o.setUser(user);
        o.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(qty)));
        o.setShippingAmount(BigDecimal.ZERO);
        o.setDiscountAmount(BigDecimal.ZERO);
        o.setGiftPointsUsed(0);
        o.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(qty)));
        o.setShippingName("Test User");
        o.setShippingLine1("1 Test St");
        o.setShippingCity("City");
        o.setShippingState("ST");
        o.setShippingPostalCode("12345");
        o.setShippingCountry("IN");
        OffsetDateTime placedAt = OffsetDateTime.ofInstant(ORDER_INSTANT, ZoneOffset.UTC);
        o.setPlacedAt(placedAt);
        o.setCancellationDeadline(placedAt.plusHours(48));
        OrderItem oi = new OrderItem();
        oi.setId(1L);
        oi.setOrder(o);
        oi.setProduct(product);
        oi.setProductTitle(product.getTitle());
        oi.setQuantity(qty);
        oi.setUnitPrice(product.getPrice());
        oi.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(qty)));
        o.setItems(new ArrayList<>(List.of(oi)));
        return o;
    }
}
