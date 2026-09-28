package com.example.ecommerce_api.category.service.impl;


import com.example.ecommerce_api.category.dto.request.CategoryRequest;
import com.example.ecommerce_api.category.dto.response.CategoryDetailResponse;
import com.example.ecommerce_api.category.dto.response.CategoryResponse;
import com.example.ecommerce_api.category.entity.Category;
import com.example.ecommerce_api.category.mapper.CategoryMapper;
import com.example.ecommerce_api.category.repository.CategoryRepository;
import com.example.ecommerce_api.category.repository.SubCategoryRepository;
import com.example.ecommerce_api.category.service.CategoryService;
import com.example.ecommerce_api.common.exception.BusinessException;
import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    private final CategoryMapper categoryMapper;

    private final SubCategoryRepository subCategoryRepository;

    @Override
    public CategoryResponse createCategory(CategoryRequest categoryRequest) {

        if (categoryRepository.existsBySlugIgnoreCase(categoryRequest.getSlug())) {
            throw new BusinessException("Slug already exists");
        }

        Category category = categoryMapper.toEntity(categoryRequest);

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    public CategoryResponse updateCategory(Long id, CategoryRequest categoryRequest) {

        Category category = findCategoryById(id);

        if (categoryRequest.getSlug() != null && !categoryRequest.getSlug().equalsIgnoreCase(category.getSlug())
                && categoryRepository.existsBySlugIgnoreCase(categoryRequest.getSlug())) {
            throw new BusinessException("Slug already exists");
        }

        categoryMapper.updateEntity(category, categoryRequest);

        Category saved = categoryRepository.save(category);

        return categoryMapper.toResponse(saved);
    }

    @Override
    public void deleteCategory(Long id) {

        Category category = findCategoryById(id);

        if (subCategoryRepository.existsByCategory_Id(id)) {
            throw new BusinessException("Cannot delete category because it has sub-categories");
        }

        categoryRepository.delete(category);
    }

    @Override
    public CategoryResponse findById(Long id) {

        return categoryMapper.toResponse(findCategoryById(id));
    }

    @Override
    public CategoryDetailResponse findDetailById(Long id) {

        return categoryMapper.toDetailResponse(findCategoryById(id));
    }

    @Override
    public List<CategoryResponse> findAll() {

        List<Category> categories = categoryRepository.findAll();
        return categoryMapper.toResponseList(categories);
    }

    private Category findCategoryById(Long id) {

        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }
}
