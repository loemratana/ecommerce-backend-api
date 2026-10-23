package com.example.ecommerce_api.coupon.services;

import com.example.ecommerce_api.coupon.dto.CouponCreateRequest;
import com.example.ecommerce_api.coupon.dto.CouponResponse;
import com.example.ecommerce_api.coupon.dto.CouponUpdateRequest;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {


    CouponResponse create(CouponCreateRequest request);

    CouponResponse getById(Long id);

    CouponResponse getByCode(String code);

    List<CouponResponse> getAll();

    CouponResponse update(
            Long id,
            CouponUpdateRequest request
    );

    void delete(Long id);

    CouponResponse activate(Long id);

    CouponResponse deactivate(Long id);

    /** orderAmount is optional; when given, the minimum is checked and discountAmount is filled in. */
    CouponResponse validate(String code, BigDecimal orderAmount);

    /** For checkout: checks the minimum, consumes one use and returns the discount for orderAmount. */
    CouponResponse useCoupon(String code, BigDecimal orderAmount);
}
