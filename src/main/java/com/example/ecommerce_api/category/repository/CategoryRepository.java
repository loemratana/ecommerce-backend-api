package com.example.ecommerce_api.category.repository;


import com.example.ecommerce_api.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsBySlugIgnoreCase(String slug);

    Optional<Category> findBySlug(String slug);
}
