package com.example.ecommerce_api.order.mapper;


import com.example.ecommerce_api.order.dto.OrderCouponResponse;
import com.example.ecommerce_api.order.dto.OrderCustomerResponse;
import com.example.ecommerce_api.order.dto.OrderItemResponse;
import com.example.ecommerce_api.order.dto.OrderResponse;
import com.example.ecommerce_api.order.dto.OrderSummaryResponse;
import com.example.ecommerce_api.order.dto.ShippingAddressResponse;
import com.example.ecommerce_api.order.entity.Order;
import com.example.ecommerce_api.order.entity.OrderCoupon;
import com.example.ecommerce_api.order.entity.OrderItem;
import com.example.ecommerce_api.order.entity.ShippingAddress;
import com.example.ecommerce_api.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order)
    {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .customer(toCustomerResponse(order.getUser()))
                .shippingAddress(toAddressResponse(order.getShippingAddress()))
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .taxPrice(order.getTaxPrice())
                .shippingPrice(order.getShippingPrice())
                .totalOrderPrice(order.getTotalOrderPrice())
                .refundedAmount(order.getRefundedAmount())
                .deliveredAt(order.getDeliveredAt())
                .items(order.getOrderItems().stream().map(this::toItemResponse).toList())
                .couponCodes(order.getOrderCoupons().stream().map(OrderCoupon::getCouponCode).toList())
                .coupons(order.getOrderCoupons().stream().map(this::toCouponResponse).toList())
                .orderDate(order.getOrderDate())
                .updateDate(order.getUpdateDate())
                .build();
    }


    public OrderSummaryResponse toSummary(Order order)
    {
        return OrderSummaryResponse.builder()
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .totalOrderPrice(order.getTotalOrderPrice())
                .customerName(order.getUser() != null ? fullName(order.getUser()) : null)
                .customerEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .itemCount(order.getOrderItems().stream().mapToInt(OrderItem::getQuantity).sum())
                .orderDate(order.getOrderDate())
                .build();
    }

    private OrderCustomerResponse toCustomerResponse(User user) {
        if (user == null) {
            return null;
        }
        return OrderCustomerResponse.builder()
                .id(user.getId())
                .name(fullName(user))
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();
    }

    private static String fullName(User user) {
        String name = Stream.of(user.getFirstName(), user.getLastName())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
        return name.isEmpty() ? user.getEmail() : name;
    }

    public OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productVariantId(item.getProductVariant() != null ? item.getProductVariant().getId() : null)
                .productName(item.getProductName())
                .sku(item.getSku())
                .color(item.getColor())
                .sizeName(item.getSizeName())
                .imageUrl(item.getImageUrl())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .priceAfterDiscount(item.getPriceAfterDiscount())
                .lineTotal(item.getLineTotal())
                .build();
    }

    public OrderCouponResponse toCouponResponse(OrderCoupon coupon) {
        return OrderCouponResponse.builder()
                .id(coupon.getId())
                .couponCode(coupon.getCouponCode())
                .discountAmount(coupon.getDiscountAmount())
                .build();
    }

    private ShippingAddressResponse toAddressResponse(ShippingAddress a) {
        if (a == null) {
            return null;
        }
        return ShippingAddressResponse.builder()
                .recipientName(a.getRecipientName()).phone(a.getPhone())
                .street(a.getStreet()).city(a.getCity()).state(a.getState())
                .zipCode(a.getZipCode()).country(a.getCountry())
                .build();
    }
}
