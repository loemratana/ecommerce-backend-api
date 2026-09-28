package com.example.ecommerce_api.user.dto.request;


import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRequest {
    @Email
    @Schema(example = "john.doe@example.com")
    private String email;


    @NotBlank
    @Size(min = 3,max = 50,message = "FirstName name must be between 3 and 50 characters")
    @Schema(example = "John")
    private String firstName;

    @NotBlank
    @Size(min = 3,max = 50,message = "LastName name must be between 3 and 50 characters")
    @Schema(example = "Doe")
    private String lastName;


    @NotBlank(message = "Password is required ")
    @Size(min = 8,message = "Password must be at least 8 characters")
    @Schema(example = "P@ssw0rd123")
    private String password;

    @NotBlank(message = "Confirm Password is required")
    @Schema(example = "P@ssw0rd123")
    private String confirmPassword;

    @Schema(example = "https://example.com/images/profile.jpg")
    private String profilePicture;

    private AddressRequest address;

    @ArraySchema(schema = @Schema(example = "1"))
    private List<Long> roles;
}
