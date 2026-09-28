package com.example.ecommerce_api.product.sevices.impl;

import com.example.ecommerce_api.common.exception.BusinessException;
import com.example.ecommerce_api.common.exception.DuplicateResourceException;
import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.product.dto.request.ProductSizeRequest;
import com.example.ecommerce_api.product.dto.request.ProductSizeUpdateRequest;
import com.example.ecommerce_api.product.dto.response.ProductSizeResponse;
import com.example.ecommerce_api.product.entity.ProductSize;
import com.example.ecommerce_api.product.mapper.ProductSizeMapper;
import com.example.ecommerce_api.product.repository.ProductSizeRepository;
import com.example.ecommerce_api.product.repository.ProductVariantRepository;
import com.example.ecommerce_api.product.sevices.ProductSizeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class ProductSizeServiceImpl implements ProductSizeService {

    private final ProductSizeRepository productSizeRepository;

    private final ProductSizeMapper productSizeMapper;

    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional
    public ProductSizeResponse deactivate(Long id) {

        ProductSize productSize = findById(id);
        productSize.setStatus(false);

        ProductSize saved = productSizeRepository.save(productSize);
        log.info("Deactivated product size {}", id);
        return productSizeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProductSizeResponse activate(Long id) {

        ProductSize productSize = findById(id);
        productSize.setStatus(true);

        ProductSize saved = productSizeRepository.save(productSize);
        log.info("Activated product size {}", id);
        return productSizeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {

        ProductSize productSize = findById(id);

        if (productVariantRepository.existsBySize_Id(id)) {
            throw new BusinessException("Cannot delete product size because it is assigned to product variants");
        }

        productSizeRepository.delete(productSize);

        log.info("Deleted product size {}", id);
    }

    @Override
    @Transactional
    public ProductSizeResponse update(Long id, ProductSizeUpdateRequest request) {

        ProductSize productSize = productSizeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("ProductSize"));
        if (request.getName() != null && !request.getName().trim().equalsIgnoreCase(productSize.getName())
                && productSizeRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("ProductSize is already exist");

        }

        productSizeMapper.toUpdatedEntity(productSize, request);

        ProductSize savedProductSize = productSizeRepository.save(productSize);
        return productSizeMapper.toResponse(savedProductSize);
    }

    @Override
    public List<ProductSizeResponse> getAll() {
        return productSizeRepository.findAll().stream()
                .map(productSizeMapper::toResponse)
                .toList();
    }

    @Override
    public ProductSizeResponse getById(Long id) {

        ProductSize productSize = findById(id);
        return productSizeMapper.toResponse(productSize);
    }

    @Override
    @Transactional
    public ProductSizeResponse create(ProductSizeRequest request) {

        if (productSizeRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Product Size with name " + request.getName() + " already exists");
        }

        ProductSize productSize = productSizeMapper.toEntity(request);
        productSize.setStatus(true);

        ProductSize saved = productSizeRepository.save(productSize);
        return productSizeMapper.toResponse(saved);
    }

    private ProductSize findById(Long id) {
        return productSizeRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("ProductSize with id: " + id + " not found")
        );
    }
}
