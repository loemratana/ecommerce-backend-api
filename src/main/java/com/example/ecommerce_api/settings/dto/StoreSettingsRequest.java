package com.example.ecommerce_api.settings.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StoreSettingsRequest {

    @NotBlank(message = "Store name is required")
    @Size(max = 150)
    private String name;

    @Email(message = "Store email must be a valid email address")
    @Size(max = 150)
    private String email;

    @Size(max = 30)
    private String phone;

    @Size(max = 255)
    private String address;

    @Size(max = 1000)
    private String description;
}
