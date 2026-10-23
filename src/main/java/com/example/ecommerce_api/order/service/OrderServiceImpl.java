package com.example.ecommerce_api.order.service;


import com.example.ecommerce_api.Enum.OrderStatus;
import com.example.ecommerce_api.cart.entity.Cart;
import com.example.ecommerce_api.cart.repository.CartRepository;
import com.example.ecommerce_api.common.exception.BadRequestException;
import com.example.ecommerce_api.common.exception.BusinessException;
import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.coupon.entity.Coupon;
import com.example.ecommerce_api.coupon.repository.CouponRepository;
import com.example.ecommerce_api.inventory.service.InventoryService;
import com.example.ecommerce_api.order.dto.*;
import com.example.ecommerce_api.order.entity.Order;
import com.example.ecommerce_api.order.entity.OrderCoupon;
import com.example.ecommerce_api.order.entity.OrderItem;
import com.example.ecommerce_api.order.entity.ShippingAddress;
import com.example.ecommerce_api.order.mapper.OrderMapper;
import com.example.ecommerce_api.order.repository.OrderRepository;
import com.example.ecommerce_api.payment.service.PaymentService;
import com.example.ecommerce_api.product.entity.ProductVariants;
import com.example.ecommerce_api.product.repository.ProductVariantRepository;
import com.example.ecommerce_api.security.SecurityUtils;
import com.example.ecommerce_api.settings.dto.SettingsResponse;
import com.example.ecommerce_api.settings.service.SettingsService;
import com.example.ecommerce_api.user.entity.Address;
import com.example.ecommerce_api.user.entity.User;
import com.example.ecommerce_api.user.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Stock flow: placing an order reserves stock, shipping turns the reservation into a sale,
 * and cancelling (only possible before shipping) releases the reservation and gives the
 * coupon use back.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService{

    private static final String ORDER_REFERENCE = "ORDER";

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CouponRepository couponRepository;
    private final InventoryService inventoryService;
    private final AddressRepository addressRepository;

    private final ProductVariantRepository productVariantRepository;
    private final CartRepository cartRepository;

    private final SecurityUtils securityUtils;
    private final SettingsService settingsService;
    private final PaymentService paymentService;

/*
PlaceOrderRequest
       │
       ▼
Get current User
       │
       ▼
Validate Address
       │
       ▼
Create Order
       │
       ▼
Merge duplicate variants
       │
       ▼
Load ProductVariants
       │
       ▼
Reserve Inventory
       │
       ▼
Create OrderItems
       │
       ▼
Validate Coupon
       │
       ▼
Apply Coupon
       │
       ▼
Calculate Tax + Shipping
       │
       ▼
Save Order
       │
       ▼
Increment Coupon Usage
       │
       ▼
OrderResponse
* */
    @Override
    public OrderResponse placeOrder(PlaceOrderRequest request) {

        User user = securityUtils.getCurrentUser();

        Address address = addressRepository.findByIdAndUserId(request.getAddressId(),user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        Order order = new Order(user, ShippingAddress.from(user,address));

        // Merge duplicate variants; TreeMap = sorted by id, so concurrent orders
        // touch inventory rows in the same order (no deadlocks).
        Map<Long,Integer> quantities = new TreeMap<>();
        request.getItems().forEach(i ->quantities.merge(i.getProductVariantId(),i.getQuantity(),Integer::sum));

        Map<Long, ProductVariants> variants = productVariantRepository.findAllById(quantities.keySet())
                .stream().collect(Collectors.toMap(ProductVariants::getId, Function.identity()));
        if (variants.size() != quantities.size()) {
            throw new ResourceNotFoundException("One or more product variants not found");
        }
        for (ProductVariants variant : variants.values()) {
            if (!variant.isActive() || !variant.getProduct().isActive()) {
                String name = variant.getProduct().getTitle() + " (" + variant.getSku() + ")";
                throw new BusinessException(name + " is no longer available; remove it from your cart");
            }
        }

        quantities.forEach((variantId, quantity) -> {
            inventoryService.reserve(variantId, quantity);   // throws if not enough stock
            order.addItem(new OrderItem(variants.get(variantId), quantity));
        });

        Coupon coupon = null;
        if (StringUtils.hasText(request.getCouponCode())) {
            coupon = couponRepository.findValidCoupon(request.getCouponCode().trim(), LocalDateTime.now())
                    .orElseThrow(() -> new BadRequestException("Coupon is invalid or expired"));
            order.applyCoupon(coupon);                        // checks the minimum amount
        }

        BigDecimal taxable = order.getSubtotal().subtract(order.getDiscountAmount());
        // taxRate e.g. 0.10 = 10% tax on (subtotal - discount)
        SettingsResponse.Orders charges = settingsService.get().getOrders();
        order.setCharges(taxable.multiply(charges.getTaxRate()).setScale(2, RoundingMode.HALF_UP), charges.getShippingFee());

        Order saved = orderRepository.saveAndFlush(order);
        OrderResponse response = orderMapper.toResponse(saved);

        // Must be last: incrementUsedCount clears the persistence context.
        // 0 rows = someone used the last coupon in the meantime -> whole transaction rolls back.
        if (coupon != null && couponRepository.incrementUsedCount(coupon.getId()) == 0) {
            throw new BusinessException("Coupon " + coupon.getCode() + " is no longer available");
        }

        log.info("Order {} placed by user {} total={}", saved.getOrderNumber(), user.getId(), saved.getTotalOrderPrice());
        return response;
    }

    @Override
    public OrderResponse checkout(CheckoutRequest request) {

        User user = securityUtils.getCurrentUser();
        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                .filter(c -> !c.getItems().isEmpty())
                .orElseThrow(() -> new BadRequestException("Your cart is empty"));

        List<OrderItemRequest> items = cart.getItems().stream()
                .map(ci -> new OrderItemRequest(ci.getProductVariants().getId(), ci.getQuantity()))
                .toList();


        // Clear before placeOrder: placeOrder's coupon update clears the persistence context,
        // after which the cart entity would be detached.
        cart.clearItems();
        cartRepository.flush();

        return placeOrder(new PlaceOrderRequest(request.getAddressId(),items,request.getCouponCode()));
    }

    // ----- customer ---------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getMyOrders(Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        Page<OrderSummaryResponse> page = orderRepository.findByUserId(userId, pageable)
                .map(orderMapper::toSummary);
        return new PageResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(String orderNumber) {
        Long userId = securityUtils.getCurrentUserId();
        Order order = orderRepository.findByOrderNumberAndUserId(orderNumber, userId)
                .orElseThrow(() -> orderNotFound(orderNumber));
        return orderMapper.toResponse(order);
    }

    @Override
    public OrderResponse cancelMyOrder(String orderNumber) {
        Long userId = securityUtils.getCurrentUserId();
        Order order = orderRepository.lockByOrderNumber(orderNumber)
                .filter(o -> o.getUser().getId().equals(userId))
                .orElseThrow(() -> orderNotFound(orderNumber));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException("Order " + orderNumber + " is already " + order.getStatus()
                    + " and can no longer be cancelled");
        }
        cancel(order);
        return orderMapper.toResponse(order);
    }

    // ----- admin ------------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getAll(OrderStatus status, String search, Pageable pageable) {
        Page<Order> orders;
        if (StringUtils.hasText(search)) {
            orders = orderRepository.search(status, "%" + search.trim().toLowerCase() + "%", pageable);
        } else {
            orders = status == null
                    ? orderRepository.findAll(pageable)
                    : orderRepository.findByStatus(status, pageable);
        }
        return new PageResponse<>(orders.map(orderMapper::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderStatsResponse getStats() {
        Map<OrderStatus, Long> byStatus = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            byStatus.put(status, 0L);
        }
        for (Object[] row : orderRepository.countGroupedByStatus()) {
            byStatus.put((OrderStatus) row[0], (Long) row[1]);
        }

        return OrderStatsResponse.builder()
                .totalOrders(byStatus.values().stream().mapToLong(Long::longValue).sum())
                .byStatus(byStatus)
                .totalRevenue(orderRepository.sumTotalByStatusIn(OrderStatus.revenueStatuses()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> orderNotFound(orderNumber));
    }

    @Override
    public OrderResponse updateStatus(String orderNumber, OrderStatus status) {
        Order order = orderRepository.lockByOrderNumber(orderNumber)
                .orElseThrow(() -> orderNotFound(orderNumber));

        // The entity rejects illegal transitions (e.g. DELIVERED -> PAID).
        switch (status) {
            case PENDING -> throw new BadRequestException("An order cannot be moved back to PENDING");
            case PAID -> order.markPaid();
            case PROCESSING -> order.startProcessing();
            case SHIPPED -> ship(order);
            case DELIVERED -> {
                order.markDelivered();
                paymentService.onOrderDelivered(order);   // cash on delivery: money collected now
            }
            case CANCELLED -> cancel(order);
        }

        log.info("Order {} status changed to {}", orderNumber, order.getStatus());
        return orderMapper.toResponse(order);
    }

    @Override
    public boolean expireIfStillPending(String orderNumber) {
        // Re-check under the row lock: the customer may have paid since the expiry job listed it.
        Order order = orderRepository.lockByOrderNumber(orderNumber).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING) {
            return false;
        }
        cancel(order);
        log.info("Order {} expired unpaid; reserved stock released", orderNumber);
        return true;
    }

    // ----- helpers ----------------------------------------------------------------------------

    /** Stock leaves the warehouse: reserved units become sold units. */
    private void ship(Order order) {
        order.markShipped();
        for (OrderItem item : order.getOrderItems()) {
            if (item.getProductVariant() != null) {   // null = variant deleted since ordering
                inventoryService.confirmSale(item.getProductVariant().getId(), item.getQuantity(),
                        ORDER_REFERENCE, order.getId());
            }
        }
    }

    /** Only reachable before SHIPPED, so the stock is still reserved, not sold. */
    private void cancel(Order order) {
        order.cancel();
        // Customer cancel, admin cancel and the expiry job all come through here
        paymentService.onOrderCancelled(order);
        for (OrderItem item : order.getOrderItems()) {
            if (item.getProductVariant() != null) {
                inventoryService.releaseReservation(item.getProductVariant().getId(), item.getQuantity());
            }
        }
        for (OrderCoupon orderCoupon : order.getOrderCoupons()) {
            couponRepository.decrementUsedCount(orderCoupon.getCoupon().getId());
        }
        orderRepository.flush();
    }

    private static ResourceNotFoundException orderNotFound(String orderNumber) {
        return new ResourceNotFoundException("Order not found: " + orderNumber);
    }
}
