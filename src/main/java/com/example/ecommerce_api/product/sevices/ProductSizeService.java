package com.example.ecommerce_api.product.sevices;

import com.example.ecommerce_api.product.dto.request.ProductSizeRequest;
import com.example.ecommerce_api.product.dto.request.ProductSizeUpdateRequest;
import com.example.ecommerce_api.product.dto.response.ProductSizeResponse;

import java.util.List;

public interface ProductSizeService {

    ProductSizeResponse create(ProductSizeRequest request);

    ProductSizeResponse getById(Long id);

    List<ProductSizeResponse> getAll();

    ProductSizeResponse update(Long id, ProductSizeUpdateRequest request);

    void delete(Long id);

    ProductSizeResponse activate(Long id);

    ProductSizeResponse deactivate(Long id);
}
