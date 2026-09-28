package com.example.ecommerce_api.product.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(name = "ProductBrandSummary")
public class BrandResponse {

    private Long brandId;
    private String brandName;

}
