package com.example.ecommerce_api.coupon.mapper;


import com.example.ecommerce_api.coupon.dto.CouponCreateRequest;
import com.example.ecommerce_api.coupon.dto.CouponResponse;
import com.example.ecommerce_api.coupon.dto.CouponUpdateRequest;
import com.example.ecommerce_api.coupon.entity.Coupon;
import org.springframework.stereotype.Component;

@Component
public class CouponMapper
{


    public Coupon toEntity(CouponCreateRequest request)
    {
        Coupon coupon = new Coupon();
        coupon.setCode(request.getCode());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setExpiryDate(request.getExpiryDate());
        coupon.setActive(request.getActive() !=null ? request.getActive() : true);
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setUsedCount(0);

        return coupon;
    }

    public void updateEntity (CouponUpdateRequest request, Coupon coupon)
    {
        coupon.setCode(request.getCode());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setExpiryDate(request.getExpiryDate());
        coupon.setActive(request.getActive());
        coupon.setUsageLimit(request.getUsageLimit());

    }

    public CouponResponse toResponse(Coupon coupon)
    {



        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .expiryDate(coupon.getExpiryDate())
                .active(coupon.isActive())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                // Calculate remaining usage
                //        if (coupon.getUsageLimit() == null) {
                //            response.setRemainingUses(null);
                //        } else {
                //            int remaining =
                //                    coupon.getUsageLimit() - coupon.getUsedCount();
                //
                //            response.setRemainingUses(
                //                    Math.max(remaining, 0)
                //            );
                //        }
                .remainingUses(
                        coupon.getUsageLimit() == null? null:Math.max(
                                coupon.getUsageLimit() -coupon.getUsedCount(),0
                        )
                )
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();

    }

}
