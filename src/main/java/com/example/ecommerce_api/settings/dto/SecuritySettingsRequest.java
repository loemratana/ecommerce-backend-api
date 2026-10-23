package com.example.ecommerce_api.settings.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SecuritySettingsRequest {

    @NotNull(message = "Session length is required")
    @Min(value = 5, message = "Session length must be at least 5 minutes")
    @Max(value = 1440, message = "Session length cannot be more than 24 hours")
    private Integer accessTokenTtlMinutes;
}
