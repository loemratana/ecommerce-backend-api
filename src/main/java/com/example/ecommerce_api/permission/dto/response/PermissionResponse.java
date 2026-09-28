package com.example.ecommerce_api.permission.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PermissionResponse {

    private Long id;
    private String name;
    private String resource;
    private String action;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
