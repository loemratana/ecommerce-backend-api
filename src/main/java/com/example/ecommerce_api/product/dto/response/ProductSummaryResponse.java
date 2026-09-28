package com.example.ecommerce_api.product.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
public class ProductSummaryResponse {
    private Long id;
    private String name;
    private String slug;
    private BigDecimal price;
    private String imageUrl;
}
