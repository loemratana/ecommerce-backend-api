package com.example.ecommerce_api.user.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressRequest {

    @Schema(example = "123 Main St")
    private String street;

    @Schema(example = "New York")
    private String city;

    @Schema(example = "NY")
    private String state;

    @Schema(example = "10001")
    private String zipCode;

    @Schema(example = "USA")
    private String country;

    @Schema(example = "true")
    private Boolean isDefault;
}
