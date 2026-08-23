package com.ebookstore.cart.service;

import com.ebookstore.cart.dto.AddCartItemRequest;
import com.ebookstore.cart.dto.CartResponse;
import com.ebookstore.cart.dto.UpdateCartItemRequest;
import com.ebookstore.cart.entity.Cart;
import com.ebookstore.cart.entity.CartItem;
import com.ebookstore.cart.repository.CartItemRepository;
import com.ebookstore.cart.repository.CartRepository;
import com.ebookstore.catalog.entity.Category;
import com.ebookstore.catalog.entity.Product;
import com.ebookstore.catalog.repository.ProductRepository;
import com.ebookstore.common.domain.CartStatus;
import com.ebookstore.common.exception.InsufficientStockException;
import com.ebookstore.common.exception.ResourceNotFoundException;
import com.ebookstore.order.repository.OrderItemRepository;
import com.ebookstore.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CartService}.
 * No Spring context — all dependencies are mocked with Mockito.
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderItemRepository orderItemRepository;

    private CartService cartService;

    private static final Long USER_ID = 10L;
    private static final Long CART_ID = 100L;
    private static final Long PRODUCT_ID = 200L;
    private static final Long ITEM_ID = 300L;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, cartItemRepository,
                productRepository, orderItemRepository);
    }

    // =========================================================================
    // getCart
    // =========================================================================

    @Test
    void getCart_returnsCartResponse_forExistingCart() {
        Cart cart = buildCart(CART_ID, CartStatus.ACTIVE);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(orderItemRepository.findDistinctCategoryIdsByUserId(USER_ID)).thenReturn(List.of());

        CartResponse response = cartService.getCart(USER_ID);

        assertThat(response.getId()).isEqualTo(CART_ID);
        assertThat(response.getStatus()).isEqualTo(CartStatus.ACTIVE);
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getCart_cartRemainsActive_notModified() {
        Cart cart = buildCart(CART_ID, CartStatus.ACTIVE);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(orderItemRepository.findDistinctCategoryIdsByUserId(USER_ID)).thenReturn(List.of());

        CartResponse response = cartService.getCart(USER_ID);

        assertThat(response.getStatus()).isEqualTo(CartStatus.ACTIVE);
        // Cart must never be saved/modified by getCart
        verify(cartRepository, never()).save(any());
    }

    // =========================================================================
    // addCartItem — new product
    // =========================================================================

    @Test
    void addCartItem_newProduct_createsCartItem() {
        Product product = buildProduct(PRODUCT_ID, "Spring Boot", new BigDecimal("29.99"), 10, true);
        Cart cart = buildCart(CART_ID, CartStatus.ACTIVE);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(CART_ID, PRODUCT_ID)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> {
            CartItem ci = inv.getArgument(0);
            ci.setId(ITEM_ID);
            cart.getItems().add(ci);
            return ci;
        });
        when(orderItemRepository.findDistinctCategoryIdsByUserId(USER_ID)).thenReturn(List.of());

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(PRODUCT_ID);
        request.setQuantity(2);

        CartResponse response = cartService.addCartItem(USER_ID, request);

        // Verify a new CartItem was saved
        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        CartItem saved = captor.getValue();
        assertThat(saved.getQuantity()).isEqualTo(2);
        assertThat(saved.getUnitPrice()).isEqualByComparingTo(new BigDecimal("29.99"));
        assertThat(saved.getCart()).isEqualTo(cart);

        assertThat(response.getId()).isEqualTo(CART_ID);
        assertThat(response.getItems()).hasSize(1);
    }

    @Test
    void addCartItem_unitPriceIsSetFromProductPrice() {
        Product product = buildProduct(PRODUCT_ID, "Clean Code", new BigDecimal("49.99"), 5, true);
        Cart cart = buildCart(CART_ID, CartStatus.ACTIVE);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(CART_ID, PRODUCT_ID)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(new CartItem());
        when(orderItemRepository.findDistinctCategoryIdsByUserId(USER_ID)).thenReturn(List.of());

        AddCartItemRequest req = addRequest(PRODUCT_ID, 1);
        cartService.addCartItem(USER_ID, req);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(captor.getValue().getUnitPrice()).isEqualByComparingTo(new BigDecimal("49.99"));
    }

    // =========================================================================
    // addCartItem — merge (duplicate product)
    // =========================================================================

    @Test
    void addCartItem_existingProduct_mergesQuantity() {
        Product product = buildProduct(PRODUCT_ID, "Java Book", new BigDecimal("39.99"), 10, true);
        Cart cart = buildCart(CART_ID, CartStatus.ACTIVE);

        CartItem existing = buildCartItem(ITEM_ID, cart, product, 3, new BigDecimal("39.99"));

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(CART_ID, PRODUCT_ID))
                .thenReturn(Optional.of(existing));
        when(cartItemRepository.save(existing)).thenReturn(existing);
        when(orderItemRepository.findDistinctCategoryIdsByUserId(USER_ID)).thenReturn(List.of());

        AddCartItemRequest request = addRequest(PRODUCT_ID, 2);
        cartService.addCartItem(USER_ID, request);

        // Must update quantity to 3+2=5, not create a new row
        assertThat(existing.getQuantity()).isEqualTo(5);
        verify(cartItemRepository).save(existing);
    }

    @Test
    void addCartItem_existingProduct_doesNotCreateDuplicateCartItemRow() {
        Product product = buildProduct(PRODUCT_ID, "Java Book", new BigDecimal("39.99"), 10, true);
        Cart cart = buildCart(CART_ID, CartStatus.ACTIVE);
        CartItem existing = buildCartItem(ITEM_ID, cart, product, 2, new BigDecimal("39.99"));

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(CART_ID, PRODUCT_ID))
                .thenReturn(Optional.of(existing));
        when(cartItemRepository.save(existing)).thenReturn(existing);
        when(orderItemRepository.findDistinctCategoryIdsByUserId(USER_ID)).thenReturn(List.of());

        cartService.addCartItem(USER_ID, addRequest(PRODUCT_ID, 1));

        // save called exactly once (update), never a second time creating a new row
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }

    // =========================================================================
    // addCartItem — validation failures
    // =========================================================================

    @Test
    void addCartItem_inactiveProduct_throwsInsufficientStockException() {
        Product product = buildProduct(PRODUCT_ID, "Old Book", new BigDecimal("9.99"), 5, false);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addCartItem(USER_ID, addRequest(PRODUCT_ID, 1)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("not available");

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addCartItem_insufficientStock_throwsInsufficientStockException() {
        Product product = buildProduct(PRODUCT_ID, "Rare Book", new BigDecimal("19.99"), 1, true);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addCartItem(USER_ID, addRequest(PRODUCT_ID, 2)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addCartItem_mergeExceedsStock_throwsInsufficientStockException() {
        Product product = buildProduct(PRODUCT_ID, "Limited Book", new BigDecimal("19.99"), 3, true);
        Cart cart = buildCart(CART_ID, CartStatus.ACTIVE);
        CartItem existing = buildCartItem(ITEM_ID, cart, product, 2, new BigDecimal("19.99"));

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(CART_ID, PRODUCT_ID))
                .thenReturn(Optional.of(existing));

        // cart has 2, wants 2 more, but only 3 in stock
        assertThatThrownBy(() -> cartService.addCartItem(USER_ID, addRequest(PRODUCT_ID, 2)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void addCartItem_productNotFound_throwsResourceNotFoundException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addCartItem(USER_ID, addRequest(999L, 1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // =========================================================================
    // updateCartItem
    // =========================================================================

    @Test
    void updateCartItem_updatesQuantity() {
        Product product = buildProduct(PRODUCT_ID, "Design Patterns", new BigDecimal("44.99"), 10, true);
        Cart cart = buildCart(CART_ID, CartStatus.ACTIVE);
        CartItem item = buildCartItem(ITEM_ID, cart, product, 1, new BigDecimal("44.99"));
        cart.getItems().add(item);

        when(cartItemRepository.findByIdAndCartUserId(ITEM_ID, USER_ID)).thenReturn(Optional.of(item));
        when(cartItemRepository.save(item)).thenReturn(item);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(orderItemRepository.findDistinctCategoryIdsByUserId(USER_ID)).thenReturn(List.of());

        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(5);

        CartResponse response = cartService.updateCartItem(USER_ID, ITEM_ID, request);

        assertThat(item.getQuantity()).isEqualTo(5);
        verify(cartItemRepository).save(item);
        assertThat(response.getItems()).hasSize(1);
    }

    @Test
    void updateCartItem_exceedsStock_throwsInsufficientStockException() {
        Product product = buildProduct(PRODUCT_ID, "Rare Edition", new BigDecimal("99.99"), 2, true);
        Cart cart = buildCart(CART_ID, CartStatus.ACTIVE);
        CartItem item = buildCartItem(ITEM_ID, cart, product, 1, new BigDecimal("99.99"));

        when(cartItemRepository.findByIdAndCartUserId(ITEM_ID, USER_ID)).thenReturn(Optional.of(item));

        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(5);

        assertThatThrownBy(() -> cartService.updateCartItem(USER_ID, ITEM_ID, request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void updateCartItem_notFound_throwsResourceNotFoundException() {
        when(cartItemRepository.findByIdAndCartUserId(999L, USER_ID)).thenReturn(Optional.empty());

        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(3);

        assertThatThrownBy(() -> cartService.updateCartItem(USER_ID, 999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // =========================================================================
    // removeCartItem
    // =========================================================================

    @Test
    void removeCartItem_deletesItem_whenOwned() {
        Product product = buildProduct(PRODUCT_ID, "Book", new BigDecimal("10.00"), 5, true);
        Cart cart = buildCart(CART_ID, CartStatus.ACTIVE);
        CartItem item = buildCartItem(ITEM_ID, cart, product, 1, new BigDecimal("10.00"));

        when(cartItemRepository.findByIdAndCartUserId(ITEM_ID, USER_ID)).thenReturn(Optional.of(item));

        cartService.removeCartItem(USER_ID, ITEM_ID);

        verify(cartItemRepository).delete(item);
    }

    @Test
    void removeCartItem_notFound_throwsResourceNotFoundException() {
        when(cartItemRepository.findByIdAndCartUserId(999L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeCartItem(USER_ID, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // =========================================================================
    // Cart totals
    // =========================================================================

    @Test
    void getCart_calculatesSubtotalAndTotalAmountFromCartItems() {
        Product p1 = buildProduct(1L, "Book A", new BigDecimal("10.00"), 5, true);
        Product p2 = buildProduct(2L, "Book B", new BigDecimal("20.00"), 5, true);
        Cart cart = buildCart(CART_ID, CartStatus.ACTIVE);
        CartItem ci1 = buildCartItem(11L, cart, p1, 2, new BigDecimal("10.00")); // 20.00
        CartItem ci2 = buildCartItem(12L, cart, p2, 3, new BigDecimal("20.00")); // 60.00
        cart.getItems().add(ci1);
        cart.getItems().add(ci2);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(orderItemRepository.findDistinctCategoryIdsByUserId(USER_ID)).thenReturn(List.of());

        CartResponse response = cartService.getCart(USER_ID);

        assertThat(response.getSubtotal()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(response.getItems()).hasSize(2);
    }

    // =========================================================================
    // Recommendations
    // =========================================================================

    @Test
    void getCart_recommendations_returnsEmptyList_whenNoPurchaseHistory() {
        Cart cart = buildCart(CART_ID, CartStatus.ACTIVE);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(orderItemRepository.findDistinctCategoryIdsByUserId(USER_ID)).thenReturn(List.of());

        CartResponse response = cartService.getCart(USER_ID);

        assertThat(response.getRecommendedProducts()).isEmpty();
        // productRepository.findRecommendations must NOT be called when no categories
        verify(productRepository, never()).findRecommendations(anyList(), anyList(), anyInt());
    }

    @Test
    void getCart_recommendations_excludesProductsAlreadyInCart() {
        Category category = buildCategory(1L);
        Product inCart = buildProductWithCategory(PRODUCT_ID, "In Cart Book", new BigDecimal("10.00"), 5, true, category);
        Product recommended = buildProductWithCategory(50L, "Recommended", new BigDecimal("15.00"), 3, true, category);

        Cart cart = buildCart(CART_ID, CartStatus.ACTIVE);
        CartItem ci = buildCartItem(ITEM_ID, cart, inCart, 1, new BigDecimal("10.00"));
        cart.getItems().add(ci);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(orderItemRepository.findDistinctCategoryIdsByUserId(USER_ID)).thenReturn(List.of(1L));
        // excludedIds contains PRODUCT_ID (the cart item's product)
        when(productRepository.findRecommendations(anyList(), anyList(), anyInt()))
                .thenReturn(List.of(recommended));

        CartResponse response = cartService.getCart(USER_ID);

        assertThat(response.getRecommendedProducts()).hasSize(1);
        assertThat(response.getRecommendedProducts().get(0).getId()).isEqualTo(50L);

        // Verify excluded ids include the cart product
        ArgumentCaptor<List> excludedCaptor = ArgumentCaptor.forClass(List.class);
        verify(productRepository).findRecommendations(anyList(), excludedCaptor.capture(), anyInt());
        assertThat(excludedCaptor.getValue()).contains(PRODUCT_ID);
    }

    @Test
    void getCart_recommendations_cappedAtFour() {
        Category category = buildCategory(1L);
        List<Product> manyProducts = new ArrayList<>();
        for (long i = 1; i <= 4; i++) {
            manyProducts.add(buildProductWithCategory(i, "Book " + i, new BigDecimal("10.00"), 5, true, category));
        }

        Cart cart = buildCart(CART_ID, CartStatus.ACTIVE);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(orderItemRepository.findDistinctCategoryIdsByUserId(USER_ID)).thenReturn(List.of(1L));
        when(productRepository.findRecommendations(anyList(), anyList(), anyInt()))
                .thenReturn(manyProducts);

        CartResponse response = cartService.getCart(USER_ID);

        assertThat(response.getRecommendedProducts()).hasSize(4);
    }

    @Test
    void getCart_recommendations_maximumSizeLimit_passedToRepository() {
        Cart cart = buildCart(CART_ID, CartStatus.ACTIVE);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(orderItemRepository.findDistinctCategoryIdsByUserId(USER_ID)).thenReturn(List.of(1L));
        when(productRepository.findRecommendations(anyList(), anyList(), anyInt()))
                .thenReturn(List.of());

        cartService.getCart(USER_ID);

        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(productRepository).findRecommendations(anyList(), anyList(), limitCaptor.capture());
        assertThat(limitCaptor.getValue()).isEqualTo(4);
    }

    // =========================================================================
    // Cart status remains ACTIVE
    // =========================================================================

    @Test
    void addCartItem_cartStatusRemainsActive() {
        Product product = buildProduct(PRODUCT_ID, "Test Book", new BigDecimal("9.99"), 10, true);
        Cart cart = buildCart(CART_ID, CartStatus.ACTIVE);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(CART_ID, PRODUCT_ID)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(new CartItem());
        when(orderItemRepository.findDistinctCategoryIdsByUserId(USER_ID)).thenReturn(List.of());

        cartService.addCartItem(USER_ID, addRequest(PRODUCT_ID, 1));

        // Cart status must not be changed
        assertThat(cart.getStatus()).isEqualTo(CartStatus.ACTIVE);
        verify(cartRepository, never()).save(any());
    }

    // =========================================================================
    // Builders / helpers
    // =========================================================================

    private Cart buildCart(Long id, CartStatus status) {
        User user = new User();
        user.setId(USER_ID);
        Cart cart = new Cart();
        cart.setId(id);
        cart.setStatus(status);
        cart.setUser(user);
        return cart;
    }

    private Product buildProduct(Long id, String title, BigDecimal price, int stock, boolean active) {
        Category category = buildCategory(1L);
        return buildProductWithCategory(id, title, price, stock, active, category);
    }

    private Product buildProductWithCategory(long id, String title, BigDecimal price,
                                              int stock, boolean active, Category category) {
        Product p = new Product();
        p.setId(id);
        p.setTitle(title);
        p.setPrice(price);
        p.setStockQuantity(stock);
        p.setActive(active);
        p.setCategory(category);
        com.ebookstore.catalog.entity.Brand brand = new com.ebookstore.catalog.entity.Brand();
        brand.setId(1L);
        brand.setName("Test Brand");
        p.setBrand(brand);
        return p;
    }

    private Category buildCategory(Long id) {
        Category c = new Category();
        c.setId(id);
        c.setName("Test Category");
        return c;
    }

    private CartItem buildCartItem(Long id, Cart cart, Product product, int qty, BigDecimal unitPrice) {
        CartItem ci = new CartItem();
        ci.setId(id);
        ci.setCart(cart);
        ci.setProduct(product);
        ci.setQuantity(qty);
        ci.setUnitPrice(unitPrice);
        return ci;
    }

    private AddCartItemRequest addRequest(Long productId, int quantity) {
        AddCartItemRequest req = new AddCartItemRequest();
        req.setProductId(productId);
        req.setQuantity(quantity);
        return req;
    }
}
