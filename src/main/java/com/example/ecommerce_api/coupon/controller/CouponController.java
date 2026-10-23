package com.example.ecommerce_api.coupon.controller;

import com.example.ecommerce_api.common.response.ApiResponse;
import com.example.ecommerce_api.coupon.dto.CouponCreateRequest;
import com.example.ecommerce_api.coupon.dto.CouponResponse;
import com.example.ecommerce_api.coupon.dto.CouponUpdateRequest;
import com.example.ecommerce_api.coupon.services.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("api/v1/coupons")
@Tag(name = "coupon")
@PreAuthorize("hasAnyRole('ADMIN')")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "Get all coupons")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(couponService.getAll()));
    }

    @Operation(summary = "Get coupon by id")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Coupon found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Coupon not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(couponService.getById(id)));
    }

    @Operation(summary = "Get coupon by code")
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<CouponResponse>> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(couponService.getByCode(code)));
    }

    @Operation(summary = "Validate a coupon code",
            description = "Checks the code is active, not expired and has remaining uses. "
                    + "With orderAmount, also checks the minimum order and returns the discountAmount")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @GetMapping("/validate/{code}")
    public ResponseEntity<ApiResponse<CouponResponse>> validate(
            @PathVariable String code,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal orderAmount) {

        return ResponseEntity.ok(ApiResponse.success(couponService.validate(code, orderAmount), "Coupon is valid"));
    }

    @Operation(summary = "Create a coupon")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Coupon created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Coupon code already exists")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CouponResponse>> create(@Valid @RequestBody CouponCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(couponService.create(request), "Coupon created"));
    }

    @Operation(summary = "Update a coupon")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Coupon updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Coupon not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Coupon code already exists")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CouponUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.success(couponService.update(id, request), "Coupon updated"));
    }

    @Operation(summary = "Activate a coupon")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<CouponResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(couponService.activate(id), "Coupon activated"));
    }

    @Operation(summary = "Deactivate a coupon")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<CouponResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(couponService.deactivate(id), "Coupon deactivated"));
    }

    @Operation(summary = "Delete a coupon")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        couponService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Coupon deleted"));
    }
}
