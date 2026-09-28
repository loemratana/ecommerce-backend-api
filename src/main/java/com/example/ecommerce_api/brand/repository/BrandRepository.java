package com.example.ecommerce_api.brand.repository;


import com.example.ecommerce_api.brand.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    boolean existsBySlugIgnoreCase(String slug);

    Optional<Brand> findBySlug(String slug);
}
