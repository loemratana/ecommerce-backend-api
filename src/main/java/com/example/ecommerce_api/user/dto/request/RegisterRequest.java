package com.example.ecommerce_api.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {


    @NotBlank
    @Email(regexp = "[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,3}",message = "Invalid email format for warranty")
    @Schema(example = "john.doe@example.com")
    private String email;

    @NotBlank
    @Schema(example = "John")
    private String firstName;

    @NotBlank
    @Schema(example = "Doe")
    private String lastName;

    @NotBlank
    @Size(min = 8)
    @Schema(
            example = "P@ssw0rd123",
            minLength = 8,
            format = "password"
    )
    private String password;

    @NotBlank
    @Schema(
            example = "P@ssw0rd123",
            minLength = 8,
            format = "password"
    )
    private String confirmPassword;

}
