package com.example.ecommerce_api.product.sevices;

import com.example.ecommerce_api.Enum.CatalogStatus;
import com.example.ecommerce_api.product.dto.request.ProductRequest;
import com.example.ecommerce_api.product.dto.response.ProductResponse;
import com.example.ecommerce_api.product.dto.response.ProductStatsResponse;
import com.example.ecommerce_api.product.dto.response.ProductSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    ProductResponse create(ProductRequest request);

    ProductStatsResponse getStats();

    ProductResponse getById(Long id);
    List<ProductSummaryResponse> getNewArrivals(int days , int limit);
    List<ProductSummaryResponse> getBestSellers(int days, int limit);

    /** status null = every status. */
    Page<ProductResponse> getAll(
            CatalogStatus status,
            String title,
            Long brandId,
            Long subCategoryId,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minRating,
            Pageable pageable
    );

    ProductResponse update(
            Long id,
            ProductRequest request
    );

    /** Hides the product from the shop; orders, returns and stock history keep working. */
    void archive(Long id);

    void restore(Long id);


}
