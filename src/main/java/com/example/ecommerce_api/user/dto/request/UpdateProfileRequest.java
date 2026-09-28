package com.example.ecommerce_api.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 3, max = 50, message = "FirstName name must be between 3 and 50 characters")
    @Schema(example = "John")
    private String firstName;

    @Size(min = 3, max = 50, message = "LastName name must be between 3 and 50 characters")
    @Schema(example = "Doe")
    private String lastName;

    @Size(max = 30, message = "Phone must be at most 30 characters")
    @Schema(example = "+1 555-123-4567")
    private String phone;

    @Schema(example = "https://example.com/images/profile.jpg")
    private String profileImage;
}
