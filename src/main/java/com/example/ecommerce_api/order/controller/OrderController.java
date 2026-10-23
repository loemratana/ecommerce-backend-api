package com.example.ecommerce_api.order.controller;

import com.example.ecommerce_api.Enum.OrderStatus;
import com.example.ecommerce_api.common.response.ApiResponse;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.order.dto.*;
import com.example.ecommerce_api.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/orders")
@Tag(name = "order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ----- customer ---------------------------------------------------------------------------

    @Operation(summary = "Place an order",
            description = "Reserves stock for every item, applies the optional coupon and calculates tax and shipping")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order placed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Not enough stock or invalid coupon"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Address or product variant not found")
    })
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(orderService.placeOrder(request), "Order placed"));
    }

    @Operation(summary = "Checkout cart",
            description = "Places an order from every item in the current user's cart, then empties the cart")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order placed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cart empty, not enough stock or invalid coupon"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Address not found")
    })
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(orderService.checkout(request), "Order placed"));
    }

    @Operation(summary = "Get my orders, newest first")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0") @Min(0)  int page, @RequestParam(defaultValue = "20") @Min(1) @Max(20) int size) {


        Pageable pagerequest = PageRequest.of(page,size);
        return ResponseEntity.ok(ApiResponse.success(orderService.getMyOrders(pagerequest)));
    }

    @Operation(summary = "Get one of my orders")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @GetMapping("/me/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> getMyOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getMyOrder(orderNumber)));
    }

    @Operation(summary = "Cancel one of my orders", description = "Only allowed while the order is PENDING or PAID")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @PatchMapping("/me/{orderNumber}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelMyOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success(orderService.cancelMyOrder(orderNumber), "Order cancelled"));
    }

    // ----- admin ------------------------------------------------------------------------------

    @Operation(summary = "Get all orders, optionally filtered by status and searched by order number, customer email or name")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> getAll(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAll(status, search, pageable)));
    }

    @Operation(summary = "Order counts per status and total revenue for the admin dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<OrderStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(orderService.getStats()));
    }

    @Operation(summary = "Get an order by order number")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> getByOrderNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getByOrderNumber(orderNumber)));
    }

    @Operation(summary = "Change an order's status",
            description = "PENDING -> PAID -> PROCESSING -> SHIPPED -> DELIVERED; CANCELLED is possible until SHIPPED")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Transition not allowed")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{orderNumber}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable String orderNumber,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.updateStatus(orderNumber, request.getStatus()), "Order status updated"));
    }
}
