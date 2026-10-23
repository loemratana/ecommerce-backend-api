package com.example.ecommerce_api.order.dto;


import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ShippingAddressResponse {

    private String recipientName;
    private String phone;
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
}
