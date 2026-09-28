package com.example.ecommerce_api.product.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductImageResponse {

    private Long id;

    private String imageUrl;

    private Integer sortOrder;
}
