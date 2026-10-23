package com.example.ecommerce_api.customer.service;

import com.example.ecommerce_api.Enum.OrderStatus;
import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.customer.dto.CustomerDetailResponse;
import com.example.ecommerce_api.customer.dto.CustomerStatsResponse;
import com.example.ecommerce_api.customer.dto.CustomerSummaryResponse;
import com.example.ecommerce_api.order.dto.OrderSummaryResponse;
import com.example.ecommerce_api.order.mapper.OrderMapper;
import com.example.ecommerce_api.order.repository.OrderRepository;
import com.example.ecommerce_api.review.dto.ReviewResponse;
import com.example.ecommerce_api.review.repository.ReviewRepository;
import com.example.ecommerce_api.review.service.ReviewServices;
import com.example.ecommerce_api.user.entity.User;
import com.example.ecommerce_api.user.mapper.AddressMapper;
import com.example.ecommerce_api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private static final String CUSTOMER_ROLE = "CUSTOMER";

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewServices reviewServices;
    private final OrderMapper orderMapper;
    private final AddressMapper addressMapper;

    @Override
    public PageResponse<CustomerSummaryResponse> getAll(String search, Boolean active, Pageable pageable) {
        String pattern = "%" + (search == null ? "" : search.trim().toLowerCase()) + "%";
        Page<User> users = userRepository.searchCustomers(active, pattern, pageable);

        // Two grouped queries for the whole page instead of one per customer.
        List<Long> ids = users.getContent().stream().map(User::getId).toList();
        Map<Long, Object[]> countAndLast = new HashMap<>();
        Map<Long, BigDecimal> spent = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : orderRepository.countAndLastOrderByUserIds(ids)) {
                countAndLast.put((Long) row[0], row);
            }
            for (Object[] row : orderRepository.sumTotalByUserIds(ids, OrderStatus.revenueStatuses())) {
                spent.put((Long) row[0], (BigDecimal) row[1]);
            }
        }

        return new PageResponse<>(users.map(user -> {
            Object[] orders = countAndLast.get(user.getId());
            return CustomerSummaryResponse.builder()
                    .id(user.getId())
                    .name(fullName(user))
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .profileImage(user.getProfileImage())
                    .active(user.isActive())
                    .joinedAt(user.getCreatedAt())
                    .orderCount(orders == null ? 0 : (Long) orders[1])
                    .lastOrderDate(orders == null ? null : (LocalDateTime) orders[2])
                    .totalSpent(spent.getOrDefault(user.getId(), BigDecimal.ZERO))
                    .build();
        }));
    }

    @Override
    public CustomerStatsResponse getStats() {
        long active = userRepository.countCustomersByActive(true);
        long inactive = userRepository.countCustomersByActive(false);
        return CustomerStatsResponse.builder()
                .totalCustomers(userRepository.countCustomers())
                .activeCustomers(active)
                .inactiveCustomers(inactive)
                .newThisMonth(userRepository.countCustomersCreatedSince(LocalDate.now().withDayOfMonth(1).atStartOfDay()))
                .customersWithOrders(orderRepository.countDistinctCustomers())
                .build();
    }

    @Override
    public CustomerDetailResponse getById(Long id) {
        return toDetail(findCustomer(id));
    }

    @Override
    public PageResponse<OrderSummaryResponse> getOrders(Long id, Pageable pageable) {
        findCustomer(id);
        return new PageResponse<>(orderRepository.findByUserId(id, pageable).map(orderMapper::toSummary));
    }

    @Override
    public PageResponse<ReviewResponse> getReviews(Long id, Pageable pageable) {
        findCustomer(id);
        return reviewServices.getUserReviews(id, pageable);
    }

    @Override
    @Transactional
    public CustomerDetailResponse setActive(Long id, boolean active) {
        User user = findCustomer(id);
        user.setActive(active);
        log.info("Customer {} {}", id, active ? "activated" : "deactivated");
        return toDetail(user);
    }

    // ------------------------------------------------------------------------------------------

    private User findCustomer(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: ", id));
        boolean isCustomer = user.getRoles().stream().anyMatch(role -> CUSTOMER_ROLE.equalsIgnoreCase(role.getName()));
        if (!isCustomer) {
            // Staff accounts are managed under /users, not here.
            throw new ResourceNotFoundException("Customer not found with id: ", id);
        }
        return user;
    }

    private CustomerDetailResponse toDetail(User user) {
        Map<OrderStatus, Long> byStatus = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            byStatus.put(status, 0L);
        }
        for (Object[] row : orderRepository.countGroupedByStatusForUser(user.getId())) {
            byStatus.put((OrderStatus) row[0], (Long) row[1]);
        }

        Set<Long> ids = Set.of(user.getId());
        BigDecimal spent = orderRepository.sumTotalByUserIds(ids, OrderStatus.revenueStatuses()).stream()
                .map(row -> (BigDecimal) row[1])
                .findFirst()
                .orElse(BigDecimal.ZERO);
        LocalDateTime lastOrder = orderRepository.countAndLastOrderByUserIds(ids).stream()
                .map(row -> (LocalDateTime) row[2])
                .findFirst()
                .orElse(null);
        long paidOrders = OrderStatus.revenueStatuses().stream().mapToLong(byStatus::get).sum();

        return CustomerDetailResponse.builder()
                .id(user.getId())
                .name(fullName(user))
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profileImage(user.getProfileImage())
                .active(user.isActive())
                .joinedAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .addresses(addressMapper.toResponseList(user.getAddresses()))
                .orderCount(byStatus.values().stream().mapToLong(Long::longValue).sum())
                .totalSpent(spent)
                .averageOrderValue(paidOrders == 0 ? BigDecimal.ZERO : spent.divide(BigDecimal.valueOf(paidOrders), 2, RoundingMode.HALF_UP))
                .lastOrderDate(lastOrder)
                .ordersByStatus(byStatus)
                .reviewCount(reviewRepository.countByUserId(user.getId()))
                .build();
    }

    private static String fullName(User user) {
        String name = Stream.of(user.getFirstName(), user.getLastName())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
        return name.isEmpty() ? user.getEmail() : name;
    }
}
