package com.example.ecommerce_api.returns.controller;

import com.example.ecommerce_api.Enum.ReturnStatus;
import com.example.ecommerce_api.common.response.ApiResponse;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.returns.dto.*;
import com.example.ecommerce_api.returns.service.ReturnService;
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
import java.util.Map;

@RestController
@RequestMapping("api/v1/returns")
@Tag(name = "return")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    // ----- customer ---------------------------------------------------------------------------

    @Operation(summary = "What I can still return from one of my orders")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @GetMapping("/me/eligibility/{orderNumber}")
    public ResponseEntity<ApiResponse<ReturnEligibilityResponse>> getMyEligibility(@PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success(returnService.getMyEligibility(orderNumber)));
    }

    @Operation(summary = "My orders I can still request a return for",
            description = "Delivered, inside the return period, with at least one unit not already being returned; most recently delivered first")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @GetMapping("/me/returnable-orders")
    public ResponseEntity<ApiResponse<List<ReturnableOrderResponse>>> getMyReturnableOrders() {
        return ResponseEntity.ok(ApiResponse.success(returnService.getMyReturnableOrders()));
    }

    @Operation(summary = "Request a return", description = "Only for delivered orders, within the return period")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @PostMapping("/me")
    public ResponseEntity<ApiResponse<ReturnResponse>> requestReturn(@Valid @RequestBody CreateReturnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(returnService.requestReturn(request), "Return requested"));
    }

    @Operation(summary = "My returns, newest first")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<ReturnResponse>>> getMyReturns(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(returnService.getMyReturns(pageable)));
    }

    @Operation(summary = "My returns for one order")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @GetMapping("/me/order/{orderNumber}")
    public ResponseEntity<ApiResponse<List<ReturnResponse>>> getMyReturnsForOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success(returnService.getMyReturnsForOrder(orderNumber)));
    }

    @Operation(summary = "One of my returns")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @GetMapping("/me/{returnNumber}")
    public ResponseEntity<ApiResponse<ReturnResponse>> getMyReturn(@PathVariable String returnNumber) {
        return ResponseEntity.ok(ApiResponse.success(returnService.getMyReturn(returnNumber)));
    }

    @Operation(summary = "Cancel one of my returns", description = "Only while it is still REQUESTED")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @PatchMapping("/me/{returnNumber}/cancel")
    public ResponseEntity<ApiResponse<ReturnResponse>> cancelMyReturn(@PathVariable String returnNumber) {
        return ResponseEntity.ok(ApiResponse.success(returnService.cancelMyReturn(returnNumber), "Return cancelled"));
    }

    // ----- admin ------------------------------------------------------------------------------

    @Operation(summary = "All returns, filtered by status and searched by return/order number or customer")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReturnResponse>>> getAll(
            @RequestParam(required = false) ReturnStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(returnService.getAll(status, search, pageable)));
    }

    @Operation(summary = "Number of returns per status")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<ReturnStatus, Long>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(returnService.countByStatus()));
    }

    @Operation(summary = "Returns of one order")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/order/{orderNumber}")
    public ResponseEntity<ApiResponse<List<ReturnResponse>>> getForOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success(returnService.getForOrder(orderNumber)));
    }

    @Operation(summary = "One return")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{returnNumber}")
    public ResponseEntity<ApiResponse<ReturnResponse>> getByReturnNumber(@PathVariable String returnNumber) {
        return ResponseEntity.ok(ApiResponse.success(returnService.getByReturnNumber(returnNumber)));
    }

    @Operation(summary = "Approve a return", description = "The customer can now send the items back")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{returnNumber}/approve")
    public ResponseEntity<ApiResponse<ReturnResponse>> approve(@PathVariable String returnNumber,
                                                               @Valid @RequestBody(required = false) ReturnDecisionRequest body) {
        return ResponseEntity.ok(ApiResponse.success(
                returnService.approve(returnNumber, body != null ? body.getNote() : null), "Return approved"));
    }

    @Operation(summary = "Reject a return", description = "A note for the customer is required")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{returnNumber}/reject")
    public ResponseEntity<ApiResponse<ReturnResponse>> reject(@PathVariable String returnNumber,
                                                              @Valid @RequestBody ReturnDecisionRequest body) {
        return ResponseEntity.ok(ApiResponse.success(returnService.reject(returnNumber, body.getNote()), "Return rejected"));
    }

    @Operation(summary = "Receive returned items",
            description = "RESTOCK items go back into stock, DAMAGED ones are written off; the refund is recorded on the order")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{returnNumber}/receive")
    public ResponseEntity<ApiResponse<ReturnResponse>> receive(@PathVariable String returnNumber,
                                                               @Valid @RequestBody ReceiveReturnRequest body) {
        return ResponseEntity.ok(ApiResponse.success(returnService.receive(returnNumber, body), "Return received"));
    }
}
