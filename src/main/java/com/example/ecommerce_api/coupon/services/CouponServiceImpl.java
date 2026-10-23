package com.example.ecommerce_api.coupon.services;


import com.example.ecommerce_api.common.exception.BadRequestException;
import com.example.ecommerce_api.common.exception.DuplicateResourceException;
import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.coupon.dto.CouponCreateRequest;
import com.example.ecommerce_api.coupon.dto.CouponResponse;
import com.example.ecommerce_api.coupon.dto.CouponUpdateRequest;
import com.example.ecommerce_api.coupon.entity.Coupon;
import com.example.ecommerce_api.coupon.mapper.CouponMapper;
import com.example.ecommerce_api.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CouponServiceImpl  implements  CouponService{

    private final CouponRepository couponRepository;
    private  final CouponMapper couponMapper ;

    @Override
    public CouponResponse create(CouponCreateRequest request) {

        if (couponRepository.existsByCodeIgnoreCase(request.getCode()))
        {
            throw  new DuplicateResourceException("Coupon already exists with code: {}",request.getCode());
        }

        Coupon coupon = couponMapper.toEntity(request);
        Coupon savedCoupon = couponRepository.save(coupon);

        return couponMapper.toResponse(savedCoupon);
    }

    @Override
    public CouponResponse getById(Long id) {
        return couponMapper.toResponse(findById(id));
    }

    @Override
    public CouponResponse getByCode(String code) {
        return couponRepository.findByCodeIgnoreCase(code).map(couponMapper::toResponse).orElseThrow(
                ()->new ResourceNotFoundException("Coupon not found with code: " + code)
        );
    }

    @Override
    public List<CouponResponse> getAll() {
        return couponRepository.findAll().stream().map(couponMapper::toResponse).toList();
    }

    @Override
    public CouponResponse update(Long id, CouponUpdateRequest request) {
        Coupon coupon = findById(id);
        if (request.getCode() != null
                && !request.getCode().equalsIgnoreCase(coupon.getCode())) {

            couponRepository.findByCodeIgnoreCase(request.getCode())
                    .ifPresent(existing -> {
                        throw new DuplicateResourceException(
                                "Coupon already exists with code: " + request.getCode()
                        );
                    });
        }
        couponMapper.updateEntity(request, coupon);

        Coupon updatedCoupon = couponRepository.save(coupon);

        return couponMapper.toResponse(updatedCoupon);
    }

    @Override
    public void delete(Long id) {
        Coupon coupon = findById(id);
        couponRepository.delete(coupon);

    }

    @Override
    public CouponResponse activate(Long id) {
        Coupon coupon = findById(id);
        coupon.setActive(true);

        return couponMapper.toResponse(couponRepository.save(coupon));
    }

    @Override
    public CouponResponse deactivate(Long id) {
        Coupon coupon = findById(id);
        coupon.setActive(false);
        return couponMapper.toResponse(couponRepository.save(coupon));
    }

    @Override
    public CouponResponse validate(String code, BigDecimal orderAmount) {
        Coupon coupon = findValidCoupon(code);
        CouponResponse response = couponMapper.toResponse(coupon);

        if (orderAmount != null) {
            checkMinimum(coupon, orderAmount);
            response.setDiscountAmount(coupon.calculateDiscount(orderAmount));
        }
        return response;
    }

    @Override
    @Transactional
    public CouponResponse useCoupon(String code, BigDecimal orderAmount) {
        Coupon coupon = findValidCoupon(code);
        checkMinimum(coupon, orderAmount);

        // 0 rows updated means a concurrent request took the last use first
        if (couponRepository.incrementUsedCount(coupon.getId()) == 0) {
            throw new BadRequestException("Coupon is invalid or expired");
        }

        Coupon used = findById(coupon.getId());
        CouponResponse response = couponMapper.toResponse(used);
        response.setDiscountAmount(used.calculateDiscount(orderAmount));
        return response;
    }

    private void checkMinimum(Coupon coupon, BigDecimal orderAmount) {
        if (!coupon.meetsMinimum(orderAmount)) {
            throw new BadRequestException(
                    "Minimum order amount for this coupon is " + coupon.getMinOrderAmount()
            );
        }
    }

    private Coupon  findById(Long id )
    {
        return couponRepository.findById(id).orElseThrow(
                ()-> new ResourceNotFoundException("Coupon not found with id: " + id, id)
        );

    }

    private Coupon findValidCoupon(String code)
    {
        return couponRepository.findValidCoupon(code, LocalDateTime.now()).orElseThrow(
                ()-> new BadRequestException("Coupon is invalid or expired")
        );
    }
}
