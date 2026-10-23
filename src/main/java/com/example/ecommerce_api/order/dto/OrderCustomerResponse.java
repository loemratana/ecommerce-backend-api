package com.example.ecommerce_api.order.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OrderCustomerResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
}
