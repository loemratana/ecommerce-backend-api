package com.example.ecommerce_api.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Currency and timezone are also checked against the JDK's lists by SettingsService. */
@Getter
@Setter
@NoArgsConstructor
public class LocalizationSettingsRequest {

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a 3-letter ISO code such as USD")
    private String currency;

    @NotBlank(message = "Timezone is required")
    @Size(max = 64)
    private String timezone;
}
