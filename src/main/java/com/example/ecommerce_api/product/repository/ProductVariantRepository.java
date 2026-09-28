package com.example.ecommerce_api.product.repository;

import com.example.ecommerce_api.product.entity.ProductVariants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariants,Long> {

    boolean existsBySku(String sku);
    boolean existsBySkuInAndProduct_IdNot(List<String> skus, Long productId);
    boolean existsBySize_Id(Long sizeId);

}
