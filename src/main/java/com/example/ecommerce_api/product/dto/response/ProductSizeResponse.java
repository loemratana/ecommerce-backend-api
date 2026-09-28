package com.example.ecommerce_api.product.dto.response;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProductSizeResponse {

    private Long id;
    private String name;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;

}
