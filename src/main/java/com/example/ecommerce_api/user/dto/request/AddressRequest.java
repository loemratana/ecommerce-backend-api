package com.example.ecommerce_api.user.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
/**
 * Fields may be left out (null) on PATCH to keep their current value, but never sent blank.
 * Creating an address needs all of them; AddressServiceImpl checks that.
 */
public class AddressRequest {

    @Schema(example = "123 Main St")
    @Size(max = 150, message = "Street must be at most 150 characters")
    @Pattern(regexp = "(?s).*\\S.*", message = "Street cannot be blank")
    private String street;

    @Schema(example = "New York")
    @Size(max = 100, message = "City must be at most 100 characters")
    @Pattern(regexp = "(?s).*\\S.*", message = "City cannot be blank")
    private String city;

    @Schema(example = "NY")
    @Size(max = 100, message = "State must be at most 100 characters")
    @Pattern(regexp = "(?s).*\\S.*", message = "State cannot be blank")
    private String state;

    @Schema(example = "10001")
    @Size(max = 20, message = "Zip code must be at most 20 characters")
    @Pattern(regexp = "(?s).*\\S.*", message = "Zip code cannot be blank")
    private String zipCode;

    @Schema(example = "USA")
    @Size(max = 100, message = "Country must be at most 100 characters")
    @Pattern(regexp = "(?s).*\\S.*", message = "Country cannot be blank")
    private String country;

    @Schema(example = "true")
    private Boolean isDefault;
}
