package com.example.ecommerce_api.returns.mapper;

import com.example.ecommerce_api.order.entity.Order;
import com.example.ecommerce_api.order.entity.OrderItem;
import com.example.ecommerce_api.returns.dto.ReturnItemResponse;
import com.example.ecommerce_api.returns.dto.ReturnResponse;
import com.example.ecommerce_api.returns.entity.ReturnItem;
import com.example.ecommerce_api.returns.entity.ReturnRequest;
import com.example.ecommerce_api.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ReturnMapper {

    public ReturnResponse toResponse(ReturnRequest request) {
        Order order = request.getOrder();
        User user = request.getUser();
        return ReturnResponse.builder()
                .returnNumber(request.getReturnNumber())
                .status(request.getStatus())
                .orderNumber(order.getOrderNumber())
                .customerName(user != null ? fullName(user) : null)
                .customerEmail(user != null ? user.getEmail() : null)
                .reason(request.getReason())
                .adminNote(request.getAdminNote())
                .estimatedRefund(order.refundFor(request.getGoodsValue()))
                .refundAmount(request.getRefundAmount())
                .items(request.getItems().stream().map(this::toItemResponse).toList())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .receivedAt(request.getReceivedAt())
                .build();
    }

    private ReturnItemResponse toItemResponse(ReturnItem item) {
        OrderItem line = item.getOrderItem();
        return ReturnItemResponse.builder()
                .id(item.getId())
                .orderItemId(line.getId())
                .productVariantId(line.getProductVariant() != null ? line.getProductVariant().getId() : null)
                .productName(line.getProductName())
                .sku(line.getSku())
                .color(line.getColor())
                .sizeName(line.getSizeName())
                .imageUrl(line.getImageUrl())
                .quantity(item.getQuantity())
                .unitPrice(line.getPriceAfterDiscount())
                .condition(item.getCondition())
                .build();
    }

    private static String fullName(User user) {
        String name = Stream.of(user.getFirstName(), user.getLastName())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
        return name.isEmpty() ? user.getEmail() : name;
    }
}
