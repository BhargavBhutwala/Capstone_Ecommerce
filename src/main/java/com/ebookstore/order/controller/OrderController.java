package com.ebookstore.order.controller;

import com.ebookstore.cart.dto.CartResponse;
import com.ebookstore.common.domain.OrderStatus;
import com.ebookstore.common.dto.PagedResponse;
import com.ebookstore.order.dto.CreateOrderRequest;
import com.ebookstore.order.dto.OrderResponse;
import com.ebookstore.order.service.OrderService;
import com.ebookstore.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Order management endpoints.
 *
 * <p>All endpoints require a valid JWT. The authenticated user's database id is
 * obtained exclusively from {@link AuthenticatedUser#getId()} — never from
 * request body, path variables, or query parameters.
 *
 * <p>Order list sort is fixed at {@code placed_at DESC}; the controller does not
 * expose a sort parameter to clients.
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** operationId: listOrders */
    @Operation(operationId = "listOrders", summary = "List the authenticated user's orders")
    @GetMapping
    public ResponseEntity<PagedResponse<OrderResponse>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OrderStatus status,
            Authentication authentication) {
        Long userId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
        return ResponseEntity.ok(orderService.listOrders(userId, status, page, size));
    }

    /** operationId: createOrder */
    @Operation(operationId = "createOrder", summary = "Create an order (checkout)")
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {
        Long userId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
        OrderResponse response = orderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** operationId: getOrder */
    @Operation(operationId = "getOrder", summary = "Get an order by id")
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        Long userId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
        return ResponseEntity.ok(orderService.getOrder(userId, orderId));
    }

    /** operationId: buyAgain */
    @Operation(operationId = "buyAgain", summary = "Re-add historical order items to cart")
    @PostMapping("/{orderId}/buy-again")
    public ResponseEntity<CartResponse> buyAgain(
            @PathVariable Long orderId,
            Authentication authentication) {
        Long userId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
        return ResponseEntity.ok(orderService.buyAgain(userId, orderId));
    }

    /** operationId: cancelOrder */
    @Operation(operationId = "cancelOrder", summary = "Cancel an order within the cancellation window")
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        Long userId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
        return ResponseEntity.ok(orderService.cancelOrder(userId, orderId));
    }
}
