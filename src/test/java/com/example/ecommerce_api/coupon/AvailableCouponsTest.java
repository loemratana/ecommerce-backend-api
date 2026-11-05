package com.example.ecommerce_api.coupon;

import com.example.ecommerce_api.coupon.entity.Coupon;
import com.example.ecommerce_api.coupon.repository.CouponRepository;
import com.example.ecommerce_api.coupon.services.CouponService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The storefront's coupon list runs against the real database and is open to guests. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AvailableCouponsTest {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponService couponService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsOnlyUsableCoupons() {
        LocalDateTime future = LocalDateTime.now().plusDays(7);
        couponRepository.save(coupon("TEST-OPEN", true, future, null, 0));
        Coupon hidden = coupon("TEST-HIDDEN", true, future, null, 0);
        hidden.setShowInStorefront(false);
        couponRepository.save(hidden);
        couponRepository.save(coupon("TEST-INACTIVE", false, future, null, 0));
        couponRepository.save(coupon("TEST-EXPIRED", true, LocalDateTime.now().minusDays(1), null, 0));
        couponRepository.save(coupon("TEST-USED-UP", true, future, 5, 5));

        var codes = couponService.getAvailable().stream().map(c -> c.code()).toList();

        assertThat(codes).contains("TEST-OPEN")
                .doesNotContain("TEST-HIDDEN", "TEST-INACTIVE", "TEST-EXPIRED", "TEST-USED-UP");
    }

    @Test
    void newCouponsArePrivateUnlessAskedAndUpdatesKeepTheSetting() {
        var request = new com.example.ecommerce_api.coupon.dto.CouponCreateRequest();
        request.setCode("TEST-NEW");
        request.setDiscountType(com.example.ecommerce_api.Enum.DiscountType.FIXED);
        request.setDiscountValue(new BigDecimal("5"));
        request.setExpiryDate(LocalDateTime.now().plusDays(3));
        var created = couponService.create(request);
        assertThat(created.getShowInStorefront()).isFalse();

        var update = new com.example.ecommerce_api.coupon.dto.CouponUpdateRequest(
                "TEST-NEW", com.example.ecommerce_api.Enum.DiscountType.FIXED, new BigDecimal("5"), null,
                LocalDateTime.now().plusDays(3), true, true, null);
        assertThat(couponService.update(created.getId(), update).getShowInStorefront()).isTrue();

        update.setShowInStorefront(null);   // an older client that doesn't send the field
        assertThat(couponService.update(created.getId(), update).getShowInStorefront()).isTrue();
    }

    @Test
    void guestsCanSeeTheList() throws Exception {
        mockMvc.perform(get("/api/v1/coupons/available")).andExpect(status().isOk());
    }

    @Test
    void theAdminListStaysProtected() throws Exception {
        mockMvc.perform(get("/api/v1/coupons")).andExpect(status().is4xxClientError());
    }

    private static Coupon coupon(String code, boolean active, LocalDateTime expiry, Integer limit, int used) {
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDiscountValue(new BigDecimal("10"));
        coupon.setActive(active);
        coupon.setExpiryDate(expiry);
        coupon.setUsageLimit(limit);
        coupon.setUsedCount(used);
        coupon.setShowInStorefront(true);
        return coupon;
    }
}
