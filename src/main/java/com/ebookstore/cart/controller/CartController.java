package com.ebookstore.cart.controller;

import com.ebookstore.cart.dto.AddCartItemRequest;
import com.ebookstore.cart.dto.CartResponse;
import com.ebookstore.cart.dto.UpdateCartItemRequest;
import com.ebookstore.cart.service.CartService;
import com.ebookstore.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cart management endpoints.
 *
 * <p>All four operations require a valid JWT. The authenticated user's database
 * id is obtained exclusively from
 * {@link AuthenticatedUser#getId()} — never from request body, query params,
 * or client-controlled headers.
 */
@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /** operationId: getCart */
    @Operation(operationId = "getCart", summary = "Get the authenticated user's cart")
    @GetMapping
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        Long userId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    /** operationId: addCartItem */
    @Operation(operationId = "addCartItem", summary = "Add an item to the cart")
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addCartItem(
            @Valid @RequestBody AddCartItemRequest request,
            Authentication authentication) {
        Long userId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
        CartResponse response = cartService.addCartItem(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** operationId: updateCartItem */
    @Operation(operationId = "updateCartItem", summary = "Update cart item quantity")
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            Authentication authentication) {
        Long userId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
        CartResponse response = cartService.updateCartItem(userId, itemId, request);
        return ResponseEntity.ok(response);
    }

    /** operationId: removeCartItem */
    @Operation(operationId = "removeCartItem", summary = "Remove an item from the cart")
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeCartItem(
            @PathVariable Long itemId,
            Authentication authentication) {
        Long userId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
        cartService.removeCartItem(userId, itemId);
        return ResponseEntity.noContent().build();
    }
}
