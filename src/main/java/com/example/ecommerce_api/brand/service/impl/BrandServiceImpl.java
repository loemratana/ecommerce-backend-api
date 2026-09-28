package com.example.ecommerce_api.brand.service.impl;


import com.example.ecommerce_api.brand.dto.request.BrandRequest;
import com.example.ecommerce_api.brand.dto.response.BrandResponse;
import com.example.ecommerce_api.brand.entity.Brand;
import com.example.ecommerce_api.brand.mapper.BrandMapper;
import com.example.ecommerce_api.brand.repository.BrandRepository;
import com.example.ecommerce_api.brand.service.BrandService;
import com.example.ecommerce_api.common.exception.BusinessException;
import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;

    private final BrandMapper brandMapper;

    private final ProductRepository productRepository;

    @Override
    public BrandResponse createBrand(BrandRequest brandRequest) {

        if (brandRepository.existsBySlugIgnoreCase(brandRequest.getSlug())) {
            throw new BusinessException("Slug already exists");
        }

        Brand brand = brandMapper.toEntity(brandRequest);

        Brand savedBrand = brandRepository.save(brand);

        return brandMapper.toResponse(savedBrand);
    }

    @Override
    public BrandResponse updateBrand(Long id, BrandRequest brandRequest) {

        Brand brand = findBrandById(id);

        if (brandRequest.getSlug() != null && !brandRequest.getSlug().equalsIgnoreCase(brand.getSlug())
                && brandRepository.existsBySlugIgnoreCase(brandRequest.getSlug())) {
            throw new BusinessException("Slug already exists");
        }

        brandMapper.updateEntity(brand, brandRequest);

        Brand saved = brandRepository.save(brand);

        return brandMapper.toResponse(saved);
    }

    @Override
    public void deleteBrand(Long id) {

        Brand brand = findBrandById(id);

        if (productRepository.existsByBrand_Id(id)) {
            throw new BusinessException("Cannot delete brand because it is assigned to products");
        }

        brandRepository.delete(brand);
    }

    @Override
    public BrandResponse findById(Long id) {

        return brandMapper.toResponse(findBrandById(id));
    }

    @Override
    public List<BrandResponse> findAll() {

        List<Brand> brands = brandRepository.findAll();
        return brandMapper.toResponseList(brands);
    }

    private Brand findBrandById(Long id) {

        return brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
    }
}
