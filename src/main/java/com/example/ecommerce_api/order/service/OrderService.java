package com.example.ecommerce_api.order.service;

import com.example.ecommerce_api.Enum.OrderStatus;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.order.dto.CheckoutRequest;
import com.example.ecommerce_api.order.dto.OrderResponse;
import com.example.ecommerce_api.order.dto.OrderStatsResponse;
import com.example.ecommerce_api.order.dto.OrderSummaryResponse;
import com.example.ecommerce_api.order.dto.PlaceOrderRequest;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    // customer
    OrderResponse placeOrder(PlaceOrderRequest request);

    PageResponse<OrderSummaryResponse> getMyOrders(Pageable pageable);

    OrderResponse getMyOrder(String orderNumber);

    /** Customers can cancel only while the order is PENDING or PAID. */
    OrderResponse cancelMyOrder(String orderNumber);

    // admin
    /** {@code search} matches order number, customer email or customer name; blank means no search. */
    PageResponse<OrderSummaryResponse> getAll(OrderStatus status, String search, Pageable pageable);

    OrderStatsResponse getStats();

    OrderResponse getByOrderNumber(String orderNumber);

    OrderResponse updateStatus(String orderNumber, OrderStatus status);

    OrderResponse checkout(CheckoutRequest request);

    /**
     * Cancels the order if it is still PENDING (unpaid), releasing its reserved stock and
     * coupon usage. Returns false when it was paid or cancelled in the meantime.
     */
    boolean expireIfStillPending(String orderNumber);
}
