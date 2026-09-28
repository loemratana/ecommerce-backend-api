package com.example.ecommerce_api.user.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class AddressResponse {

    private Long id;
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private Boolean isDefault;
}
