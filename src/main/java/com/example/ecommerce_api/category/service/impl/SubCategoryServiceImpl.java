package com.example.ecommerce_api.category.service.impl;


import com.example.ecommerce_api.category.dto.request.SubCategoryRequest;
import com.example.ecommerce_api.category.dto.response.SubCategoryDetailResponse;
import com.example.ecommerce_api.category.dto.response.SubCategoryResponse;
import com.example.ecommerce_api.category.entity.Category;
import com.example.ecommerce_api.category.entity.SubCategory;
import com.example.ecommerce_api.category.mapper.SubCategoryMapper;
import com.example.ecommerce_api.category.repository.CategoryRepository;
import com.example.ecommerce_api.category.repository.SubCategoryRepository;
import com.example.ecommerce_api.category.service.SubCategoryService;
import com.example.ecommerce_api.common.exception.BusinessException;
import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubCategoryServiceImpl implements SubCategoryService {

    private final SubCategoryRepository subCategoryRepository;

    private final CategoryRepository categoryRepository;

    private final SubCategoryMapper subCategoryMapper;

    private final ProductRepository productRepository;

    @Override
    public SubCategoryResponse createSubCategory(SubCategoryRequest subCategoryRequest) {

        Category category = findCategoryById(subCategoryRequest.getCategoryId());

        if (subCategoryRepository.existsBySlugIgnoreCase(subCategoryRequest.getSlug())) {
            throw new BusinessException("Slug already exists");
        }

        if (subCategoryRepository.existsByCategory_IdAndNameIgnoreCase(category.getId(), subCategoryRequest.getName())) {
            throw new BusinessException("Sub-category name already exists in this category");
        }

        SubCategory subCategory = subCategoryMapper.toEntity(subCategoryRequest);
        subCategory.setCategory(category);

        SubCategory savedSubCategory = subCategoryRepository.save(subCategory);

        return subCategoryMapper.toResponse(savedSubCategory);
    }

    @Override
    public SubCategoryResponse updateSubCategory(Long id, SubCategoryRequest subCategoryRequest) {

        SubCategory subCategory = findSubCategoryById(id);

        Category category = subCategory.getCategory();
        if (subCategoryRequest.getCategoryId() != null
                && !subCategoryRequest.getCategoryId().equals(category.getId())) {
            category = findCategoryById(subCategoryRequest.getCategoryId());
        }

        if (subCategoryRequest.getSlug() != null && !subCategoryRequest.getSlug().equalsIgnoreCase(subCategory.getSlug())
                && subCategoryRepository.existsBySlugIgnoreCase(subCategoryRequest.getSlug())) {
            throw new BusinessException("Slug already exists");
        }

        if (subCategoryRequest.getName() != null
                && !(subCategoryRequest.getName().equalsIgnoreCase(subCategory.getName()) && category.getId().equals(subCategory.getCategory().getId()))
                && subCategoryRepository.existsByCategory_IdAndNameIgnoreCase(category.getId(), subCategoryRequest.getName())) {
            throw new BusinessException("Sub-category name already exists in this category");
        }

        subCategoryMapper.updateEntity(subCategory, subCategoryRequest);
        subCategory.setCategory(category);

        SubCategory saved = subCategoryRepository.save(subCategory);

        return subCategoryMapper.toResponse(saved);
    }

    @Override
    public SubCategoryDetailResponse getDetailSubCategory(Long id) {

        SubCategory subCategory = subCategoryRepository.findDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + id));

        log.info("SubCategory Details: {}", subCategory);
        return subCategoryMapper.toDetailResponse(subCategory);
    }

    @Override
    public List<SubCategoryResponse> getSubCategories() {
        return List.of();
    }

    @Override
    public void deleteSubCategory(Long id) {

        SubCategory subCategory = findSubCategoryById(id);

        if (productRepository.existsBySubCategory_Id(id)) {
            throw new BusinessException("Cannot delete sub-category because it has products");
        }

        subCategoryRepository.delete(subCategory);
    }

    @Override
    public SubCategoryResponse findById(Long id) {

        return subCategoryMapper.toResponse(findSubCategoryById(id));
    }

    @Override
    public List<SubCategoryResponse> findAll() {

        List<SubCategory> subCategories = subCategoryRepository.findAll();
        return subCategoryMapper.toResponseList(subCategories);
    }

    @Override
    public List<SubCategoryResponse> findByCategoryId(Long categoryId) {

        List<SubCategory> subCategories = subCategoryRepository.findByCategory_Id(categoryId);
        return subCategoryMapper.toResponseList(subCategories);
    }

    private SubCategory findSubCategoryById(Long id) {

        return subCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-category not found"));
    }

    private Category findCategoryById(Long id) {

        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }
}
