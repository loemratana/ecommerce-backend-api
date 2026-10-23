package com.example.ecommerce_api.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin update of an existing user (PUT /api/v1/users/{id}).
 * Every field is optional: null leaves it unchanged. Passwords are changed via PATCH /users/{id}/password,
 * so unlike {@link UserRequest} this has no password fields.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRequest {

    @Email
    @Schema(example = "john.doe@example.com")
    private String email;

    @Size(min = 3, max = 50, message = "FirstName name must be between 3 and 50 characters")
    @Schema(example = "John")
    private String firstName;

    @Size(min = 3, max = 50, message = "LastName name must be between 3 and 50 characters")
    @Schema(example = "Doe")
    private String lastName;

    /** "" clears the picture. */
    @Size(max = 2048, message = "Profile picture must be at most 2048 characters")
    @Schema(example = "https://res.cloudinary.com/demo/image/upload/avatars/john.png")
    private String profilePicture;
}
