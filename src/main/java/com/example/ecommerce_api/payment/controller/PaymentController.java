package com.example.ecommerce_api.payment.controller;

import com.example.ecommerce_api.Enum.PaymentMethod;
import com.example.ecommerce_api.Enum.PaymentStatus;
import com.example.ecommerce_api.common.response.ApiResponse;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.payment.dto.InitiatePaymentRequest;
import com.example.ecommerce_api.payment.dto.PaymentResponse;
import com.example.ecommerce_api.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "payment")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Payment methods the store accepts")
    @GetMapping("/methods")
    public ResponseEntity<ApiResponse<Set<PaymentMethod>>> getMethods() {
        return ResponseEntity.ok(ApiResponse.success(paymentService.availableMethods()));
    }

    @Operation(summary = "Start paying a pending order",
            description = "Returns redirectUrl for online methods; cash on delivery has none. Calling it again with the same method returns the open attempt.")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> initiate(@Valid @RequestBody InitiatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(paymentService.initiate(request), "Payment started"));
    }

    @Operation(summary = "Payment attempts for one of my orders, newest first")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @GetMapping("/me/order/{orderNumber}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getMyOrderPayments(@PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getMyOrderPayments(orderNumber)));
    }

    @Operation(summary = "Confirm my order's payment with the gateway",
            description = "Call when the customer returns from Stripe Checkout: checks the session with Stripe and marks the order paid if it was. Safe to repeat.")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @PostMapping("/me/order/{orderNumber}/confirm")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> confirmMyOrderPayments(@PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.confirmMyOrderPayments(orderNumber)));
    }

    @Operation(summary = "All payments (admin)", description = "Filter by status and/or method")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getAll(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PaymentMethod method,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getAll(status, method, pageable)));
    }

    @Operation(summary = "Payment attempts for an order (admin), newest first")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/order/{orderNumber}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getOrderPayments(@PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getOrderPayments(orderNumber)));
    }
}
