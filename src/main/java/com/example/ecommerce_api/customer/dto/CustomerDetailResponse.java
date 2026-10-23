package com.example.ecommerce_api.customer.dto;

import com.example.ecommerce_api.Enum.OrderStatus;
import com.example.ecommerce_api.user.dto.response.AddressResponse;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Builder
@Getter
public class CustomerDetailResponse {

    private Long id;
    private String name;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String profileImage;
    private boolean active;
    private LocalDateTime joinedAt;
    private LocalDateTime updatedAt;

    private List<AddressResponse> addresses;

    private long orderCount;
    private BigDecimal totalSpent;
    /** totalSpent divided by the number of paid orders; 0 when there are none. */
    private BigDecimal averageOrderValue;
    private LocalDateTime lastOrderDate;
    /** Every status is present, with 0 when the customer has none. */
    private Map<OrderStatus, Long> ordersByStatus;
    private long reviewCount;
}
