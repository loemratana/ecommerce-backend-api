package com.example.ecommerce_api.settings.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NotificationSettingsRequest {

    /** Email customers an order confirmation when they place an order. */
    @NotNull(message = "Customer order email is required")
    private Boolean customerOrderEmail;
}
