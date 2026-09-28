package com.example.ecommerce_api.product.sevices;

import com.example.ecommerce_api.product.dto.request.ProductRequest;
import com.example.ecommerce_api.product.dto.response.ProductResponse;
import com.example.ecommerce_api.product.dto.response.ProductStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ProductService {

    ProductResponse create(ProductRequest request);

    ProductStatsResponse getStats();

    ProductResponse getById(Long id);

    Page<ProductResponse> getAll(
            String title,
            Long brandId,
            Long subCategoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minRating,
            Pageable pageable
    );

    ProductResponse update(
            Long id,
            ProductRequest request
    );

    void delete(Long id);


}
