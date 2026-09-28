package com.example.ecommerce_api.brand.service;

import com.example.ecommerce_api.brand.dto.request.BrandRequest;
import com.example.ecommerce_api.brand.dto.response.BrandResponse;

import java.util.List;

public interface BrandService {

    BrandResponse createBrand(BrandRequest brandRequest);

    BrandResponse updateBrand(Long id, BrandRequest brandRequest);

    void deleteBrand(Long id);

    BrandResponse findById(Long id);

    List<BrandResponse> findAll();
}
