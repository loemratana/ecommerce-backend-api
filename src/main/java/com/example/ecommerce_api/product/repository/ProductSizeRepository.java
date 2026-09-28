package com.example.ecommerce_api.product.repository;

import com.example.ecommerce_api.product.entity.ProductSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductSizeRepository extends JpaRepository<ProductSize, Long> {

    Optional<ProductSize> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
