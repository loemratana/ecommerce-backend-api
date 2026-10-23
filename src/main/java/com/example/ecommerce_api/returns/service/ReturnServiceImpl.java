package com.example.ecommerce_api.returns.service;

import com.example.ecommerce_api.Enum.OrderStatus;
import com.example.ecommerce_api.Enum.ReturnItemCondition;
import com.example.ecommerce_api.Enum.ReturnStatus;
import com.example.ecommerce_api.common.exception.BadRequestException;
import com.example.ecommerce_api.common.exception.BusinessException;
import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.inventory.service.InventoryService;
import com.example.ecommerce_api.order.entity.Order;
import com.example.ecommerce_api.order.entity.OrderItem;
import com.example.ecommerce_api.order.repository.OrderRepository;
import com.example.ecommerce_api.payment.service.PaymentService;
import com.example.ecommerce_api.returns.dto.*;
import com.example.ecommerce_api.returns.entity.ReturnItem;
import com.example.ecommerce_api.returns.entity.ReturnRequest;
import com.example.ecommerce_api.returns.mapper.ReturnMapper;
import com.example.ecommerce_api.returns.repository.ReturnItemRepository;
import com.example.ecommerce_api.returns.repository.ReturnRequestRepository;
import com.example.ecommerce_api.security.SecurityUtils;
import com.example.ecommerce_api.settings.service.SettingsService;
import com.example.ecommerce_api.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReturnServiceImpl implements ReturnService {

    private static final String RETURN_REFERENCE = "RETURN";

    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnItemRepository returnItemRepository;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final ReturnMapper returnMapper;
    private final SecurityUtils securityUtils;
    private final SettingsService settingsService;
    private final PaymentService paymentService;

    // ----- customer ---------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ReturnEligibilityResponse getMyEligibility(String orderNumber) {
        Order order = findMyOrder(orderNumber, false);
        Map<Long, Integer> returnable = returnableQuantities(order);
        String reason = ineligibleReason(order, returnable);

        return ReturnEligibilityResponse.builder()
                .orderNumber(order.getOrderNumber())
                .eligible(reason == null)
                .reason(reason)
                .returnableUntil(returnDeadline(order))
                .items(order.getOrderItems().stream()
                        .map(line -> ReturnEligibilityResponse.Item.builder()
                                .orderItemId(line.getId())
                                .productName(line.getProductName())
                                .color(line.getColor())
                                .sizeName(line.getSizeName())
                                .imageUrl(line.getImageUrl())
                                .unitPrice(line.getPriceAfterDiscount())
                                .purchasedQuantity(line.getQuantity())
                                .returnableQuantity(returnable.getOrDefault(line.getId(), 0))
                                .build())
                        .toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnableOrderResponse> getMyReturnableOrders() {
        Long userId = securityUtils.getCurrentUserId();
        LocalDateTime deliveredSince = LocalDateTime.now()
                .minusDays(settingsService.get().getOrders().getReturnWindowDays());
        List<Order> candidates = orderRepository.findReturnCandidates(userId, OrderStatus.DELIVERED, deliveredSince);
        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> claimed = claimedQuantities(returnItemRepository.sumQuantityByOrderItemForOrders(
                candidates.stream().map(Order::getId).toList(), ReturnStatus.open()));

        List<ReturnableOrderResponse> result = new ArrayList<>();
        for (Order order : candidates) {
            Map<Long, Integer> returnable = returnableQuantities(order, claimed);
            // The query already applied status + window; this also drops fully-returned orders.
            if (ineligibleReason(order, returnable) != null) {
                continue;
            }
            result.add(ReturnableOrderResponse.builder()
                    .orderNumber(order.getOrderNumber())
                    .orderDate(order.getOrderDate())
                    .deliveredAt(order.getDeliveredAt())
                    .returnableUntil(returnDeadline(order))
                    .totalOrderPrice(order.getTotalOrderPrice())
                    .itemCount(order.getOrderItems().stream().mapToInt(OrderItem::getQuantity).sum())
                    .returnableItemCount(returnable.values().stream().mapToInt(Integer::intValue).sum())
                    .build());
        }
        return result;
    }

    @Override
    public ReturnResponse requestReturn(CreateReturnRequest request) {
        User user = securityUtils.getCurrentUser();
        // Locked so two requests for the same order can't both claim the same units.
        Order order = findMyOrder(request.getOrderNumber(), true);

        Map<Long, Integer> returnable = returnableQuantities(order);
        String reason = ineligibleReason(order, returnable);
        if (reason != null) {
            throw new BusinessException(reason);
        }

        Map<Long, OrderItem> lines = order.getOrderItems().stream()
                .collect(Collectors.toMap(OrderItem::getId, Function.identity()));
        ReturnRequest returnRequest = new ReturnRequest(order, user, request.getReason().trim());
        Set<Long> seen = new HashSet<>();
        for (CreateReturnRequest.Item item : request.getItems()) {
            if (!seen.add(item.getOrderItemId())) {
                throw new BadRequestException("Each order item can appear only once");
            }
            OrderItem line = lines.get(item.getOrderItemId());
            if (line == null) {
                throw new ResourceNotFoundException("Item " + item.getOrderItemId() + " is not part of this order");
            }
            int left = returnable.getOrDefault(line.getId(), 0);
            if (item.getQuantity() > left) {
                throw new BusinessException("You can return at most " + left + " of " + line.getProductName());
            }
            returnRequest.addItem(line, item.getQuantity());
        }

        ReturnRequest saved = returnRequestRepository.save(returnRequest);
        log.info("Return {} requested for order {} by user {}", saved.getReturnNumber(), order.getOrderNumber(), user.getId());
        return returnMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReturnResponse> getMyReturns(Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        return new PageResponse<>(returnRequestRepository.findByUserId(userId, pageable).map(returnMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnResponse> getMyReturnsForOrder(String orderNumber) {
        Long userId = securityUtils.getCurrentUserId();
        return returnRequestRepository.findByOrder_OrderNumberAndUserIdOrderByCreatedAtDesc(orderNumber, userId)
                .stream().map(returnMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnResponse getMyReturn(String returnNumber) {
        Long userId = securityUtils.getCurrentUserId();
        return returnRequestRepository.findByReturnNumberAndUserId(returnNumber, userId)
                .map(returnMapper::toResponse)
                .orElseThrow(() -> returnNotFound(returnNumber));
    }

    @Override
    public ReturnResponse cancelMyReturn(String returnNumber) {
        Long userId = securityUtils.getCurrentUserId();
        ReturnRequest request = lock(returnNumber);
        if (!request.getUser().getId().equals(userId)) {
            throw returnNotFound(returnNumber);
        }
        if (request.getStatus() != ReturnStatus.REQUESTED) {
            throw new BusinessException("Return " + returnNumber + " is already " + request.getStatus()
                    + " and can no longer be cancelled");
        }
        request.cancel();
        return returnMapper.toResponse(request);
    }

    // ----- admin ------------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReturnResponse> getAll(ReturnStatus status, String search, Pageable pageable) {
        String term = "%" + (search == null ? "" : search.trim().toLowerCase()) + "%";
        return new PageResponse<>(returnRequestRepository.search(status, term, pageable).map(returnMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ReturnStatus, Long> countByStatus() {
        Map<ReturnStatus, Long> counts = new EnumMap<>(ReturnStatus.class);
        for (ReturnStatus status : ReturnStatus.values()) {
            counts.put(status, 0L);
        }
        for (Object[] row : returnRequestRepository.countGroupedByStatus()) {
            counts.put((ReturnStatus) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnResponse getByReturnNumber(String returnNumber) {
        return returnRequestRepository.findByReturnNumber(returnNumber)
                .map(returnMapper::toResponse)
                .orElseThrow(() -> returnNotFound(returnNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnResponse> getForOrder(String orderNumber) {
        return returnRequestRepository.findByOrder_OrderNumberOrderByCreatedAtDesc(orderNumber)
                .stream().map(returnMapper::toResponse).toList();
    }

    @Override
    public ReturnResponse approve(String returnNumber, String note) {
        ReturnRequest request = lock(returnNumber);
        request.approve(trimToNull(note));
        log.info("Return {} approved", returnNumber);
        return returnMapper.toResponse(request);
    }

    @Override
    public ReturnResponse reject(String returnNumber, String note) {
        if (!StringUtils.hasText(note)) {
            throw new BadRequestException("Tell the customer why the return is rejected");
        }
        ReturnRequest request = lock(returnNumber);
        request.reject(note.trim());
        log.info("Return {} rejected", returnNumber);
        return returnMapper.toResponse(request);
    }

    @Override
    public ReturnResponse receive(String returnNumber, ReceiveReturnRequest body) {
        ReturnRequest request = lock(returnNumber);
        if (request.getStatus() != ReturnStatus.APPROVED) {
            throw new BusinessException("Only an APPROVED return can be received; this one is " + request.getStatus());
        }

        Map<Long, ReturnItemCondition> conditions = new HashMap<>();
        for (ReceiveReturnRequest.Item item : body.getItems()) {
            conditions.put(item.getReturnItemId(), item.getCondition());
        }
        for (ReturnItem item : request.getItems()) {
            ReturnItemCondition condition = conditions.remove(item.getId());
            if (condition == null) {
                throw new BadRequestException("Missing condition for " + item.getOrderItem().getProductName());
            }
            item.setCondition(condition);
        }
        if (!conditions.isEmpty()) {
            throw new BadRequestException("Items " + conditions.keySet() + " are not part of return " + returnNumber);
        }

        // Stock: every returned unit comes off the sold count; only sellable ones go back on the shelf.
        for (ReturnItem item : request.getItems()) {
            OrderItem line = item.getOrderItem();
            if (line.getProductVariant() != null) {
                inventoryService.receiveReturn(line.getProductVariant().getId(), item.getQuantity(),
                        item.getCondition() == ReturnItemCondition.RESTOCK, RETURN_REFERENCE, request.getId());
            }
        }

        // Money: lock the order so concurrent returns can't refund more than was paid.
        Order order = orderRepository.lockByOrderNumber(request.getOrder().getOrderNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        BigDecimal refund = order.refundFor(request.getGoodsValue());
        order.recordRefund(refund);
        // Send the money back through the payment (Stripe refund; cash is logged to hand back).
        // If the gateway refuses, this throws and the whole receipt, stock included, rolls back.
        paymentService.refund(order, refund, "Return " + returnNumber);
        request.markReceived(refund, trimToNull(body.getNote()));

        log.info("Return {} received, refund {}", returnNumber, refund);
        return returnMapper.toResponse(request);
    }

    // ----- helpers ----------------------------------------------------------------------------

    private Order findMyOrder(String orderNumber, boolean lock) {
        Long userId = securityUtils.getCurrentUserId();
        Optional<Order> order = lock
                ? orderRepository.lockByOrderNumber(orderNumber)
                : orderRepository.findByOrderNumber(orderNumber);
        return order.filter(o -> o.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));
    }

    /** Per order line: purchased quantity minus what is already in open or completed returns. */
    private Map<Long, Integer> returnableQuantities(Order order) {
        return returnableQuantities(order, claimedQuantities(
                returnItemRepository.sumQuantityByOrderItem(order.getId(), ReturnStatus.open())));
    }

    /** {@code claimed}: order line id → units already in returns (lines of other orders are ignored). */
    private Map<Long, Integer> returnableQuantities(Order order, Map<Long, Integer> claimed) {
        Map<Long, Integer> returnable = new HashMap<>();
        for (OrderItem line : order.getOrderItems()) {
            returnable.put(line.getId(), Math.max(0, line.getQuantity() - claimed.getOrDefault(line.getId(), 0)));
        }
        return returnable;
    }

    /** Rows of [orderItemId, summed quantity] → map. */
    private static Map<Long, Integer> claimedQuantities(List<Object[]> rows) {
        Map<Long, Integer> claimed = new HashMap<>();
        for (Object[] row : rows) {
            claimed.put((Long) row[0], ((Long) row[1]).intValue());
        }
        return claimed;
    }

    private LocalDateTime returnDeadline(Order order) {
        LocalDateTime delivered = order.getDeliveredAt();
        // How long after delivery a customer may ask for a return
        return delivered == null ? null : delivered.plusDays(settingsService.get().getOrders().getReturnWindowDays());
    }

    /** null = the order can be returned. */
    private String ineligibleReason(Order order, Map<Long, Integer> returnable) {
        if (order.getStatus() != OrderStatus.DELIVERED) {
            return "Only delivered orders can be returned";
        }
        LocalDateTime deadline = returnDeadline(order);
        if (deadline != null && LocalDateTime.now().isAfter(deadline)) {
            return "The return period for this order ended on " + deadline.toLocalDate();
        }
        if (returnable.values().stream().allMatch(quantity -> quantity == 0)) {
            return "Every item of this order is already being returned";
        }
        return null;
    }

    private ReturnRequest lock(String returnNumber) {
        return returnRequestRepository.lockByReturnNumber(returnNumber)
                .orElseThrow(() -> returnNotFound(returnNumber));
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static ResourceNotFoundException returnNotFound(String returnNumber) {
        return new ResourceNotFoundException("Return not found: " + returnNumber);
    }
}
