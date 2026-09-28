package com.example.ecommerce_api.user.controller;


import com.example.ecommerce_api.common.response.ApiResponse;
import com.example.ecommerce_api.user.dto.request.UpdatePasswordRequest;
import com.example.ecommerce_api.user.dto.request.UserRequest;
import com.example.ecommerce_api.user.dto.response.UserResponse;
import com.example.ecommerce_api.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(description = "")
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserRequest request) {

        UserResponse response = userService.createUser(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User created successfully"));
    }

    @Operation(description = "")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable Long id,
                                                                  @Valid @RequestBody UserRequest request) {

        UserResponse response = userService.updateUser(id, request);

        return ResponseEntity.ok(ApiResponse.success(response, "User updated successfully"));
    }


    @Operation(description = "")
    @PatchMapping("/{id}/password")
    public ResponseEntity<ApiResponse<UserResponse>> updatePassword(@PathVariable Long id,
                                                                      @Valid @RequestBody UpdatePasswordRequest request) {

        UserResponse response = userService.updatePassword(id, request);

        return ResponseEntity.ok(ApiResponse.success(response, "Password updated successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {

        UserResponse response = userService.findUserById(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(@PathVariable String email) {

        UserResponse response = userService.findUserByEmail(email);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {

        userService.deleteUser(id);

        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }
}
