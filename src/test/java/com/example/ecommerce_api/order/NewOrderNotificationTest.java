package com.example.ecommerce_api.order;

import com.example.ecommerce_api.common.exception.BusinessException;
import com.example.ecommerce_api.common.exception.InsufficientStockException;
import com.example.ecommerce_api.coupon.entity.Coupon;
import com.example.ecommerce_api.coupon.repository.CouponRepository;
import com.example.ecommerce_api.inventory.service.InventoryService;
import com.example.ecommerce_api.notification.service.NotificationService;
import com.example.ecommerce_api.order.dto.OrderItemRequest;
import com.example.ecommerce_api.order.dto.OrderResponse;
import com.example.ecommerce_api.order.dto.PlaceOrderRequest;
import com.example.ecommerce_api.order.entity.Order;
import com.example.ecommerce_api.order.mapper.OrderMapper;
import com.example.ecommerce_api.order.repository.OrderRepository;
import com.example.ecommerce_api.order.service.OrderServiceImpl;
import com.example.ecommerce_api.product.entity.Product;
import com.example.ecommerce_api.product.entity.ProductVariants;
import com.example.ecommerce_api.product.repository.ProductVariantRepository;
import com.example.ecommerce_api.security.SecurityUtils;
import com.example.ecommerce_api.settings.dto.SettingsResponse;
import com.example.ecommerce_api.settings.service.SettingsService;
import com.example.ecommerce_api.user.entity.Address;
import com.example.ecommerce_api.user.entity.User;
import com.example.ecommerce_api.user.repository.AddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** placeOrder asks for a NEW_ORDER notification only when the order goes through. No database. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NewOrderNotificationTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderMapper orderMapper;
    @Mock CouponRepository couponRepository;
    @Mock InventoryService inventoryService;
    @Mock AddressRepository addressRepository;
    @Mock ProductVariantRepository productVariantRepository;
    @Mock SecurityUtils securityUtils;
    @Mock SettingsService settingsService;
    @Mock NotificationService notificationService;
    @InjectMocks OrderServiceImpl orderService;

    private final OrderResponse response = OrderResponse.builder().orderNumber("ORD-1").build();

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        user.setFirstName("Dara");
        user.setLastName("Sok");
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(addressRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(new Address()));

        Product product = new Product();
        product.setTitle("Basic Tee");
        ProductVariants variant = new ProductVariants();
        variant.setId(7L);
        variant.setSku("TEE-M-BLK");
        variant.setPrice(new BigDecimal("10.00"));
        variant.setProduct(product);
        when(productVariantRepository.findAllById(any())).thenReturn(List.of(variant));

        when(settingsService.get()).thenReturn(new SettingsResponse(null,
                new SettingsResponse.Orders(BigDecimal.ZERO, BigDecimal.ZERO, 30, 30),
                new SettingsResponse.Inventory(10), new SettingsResponse.Notifications(true), null, null, null, null));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(response);
    }

    @Test
    void placedOrderTriggersTheNewOrderNotification() {
        OrderResponse placed = orderService.placeOrder(new PlaceOrderRequest(5L, List.of(new OrderItemRequest(7L, 2)), null));

        assertThat(placed).isSameAs(response);
        verify(notificationService).notifyNewOrder(response);
    }

    @Test
    void orderThatFailsOnStockIsNotNotified() {
        doThrow(new InsufficientStockException("Insufficient stock")).when(inventoryService).reserve(anyLong(), anyInt());

        assertThatThrownBy(() -> orderService.placeOrder(new PlaceOrderRequest(5L, List.of(new OrderItemRequest(7L, 2)), null)))
                .isInstanceOf(InsufficientStockException.class);
        verify(notificationService, never()).notifyNewOrder(any());
    }

    @Test
    void orderThatLosesTheLastCouponUseIsNotNotified() {
        Coupon coupon = org.mockito.Mockito.mock(Coupon.class);
        when(coupon.getId()).thenReturn(3L);
        when(coupon.getCode()).thenReturn("SAVE10");
        when(coupon.meetsMinimum(any())).thenReturn(true);
        when(coupon.calculateDiscount(any())).thenReturn(new BigDecimal("2.00"));
        when(couponRepository.findValidCoupon(eq("SAVE10"), any())).thenReturn(Optional.of(coupon));
        when(couponRepository.incrementUsedCount(3L)).thenReturn(0);   // someone used it in the meantime

        assertThatThrownBy(() -> orderService.placeOrder(new PlaceOrderRequest(5L, List.of(new OrderItemRequest(7L, 2)), "SAVE10")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no longer available");
        verify(notificationService, never()).notifyNewOrder(any());
    }
}
