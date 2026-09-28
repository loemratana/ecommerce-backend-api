package com.example.ecommerce_api.product.repository;


import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsByBrand_Id(Long brandId);

    long countByCreatedAtGreaterThanEqual(LocalDateTime from);

    long countByStockQuantityGreaterThan(int threshold);

    long countByStockQuantityLessThanEqualAndStockQuantityGreaterThan(int upperInclusive, int lowerExclusive);

    long countByStockQuantityIsNullOrStockQuantityLessThanEqual(int threshold);

    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    Optional<Product> findBySlug(String slug);

    boolean existsBySubCategory_Id(Long subCategoryId);



    @EntityGraph(attributePaths = {"brand", "subCategory", "images"})
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdWithAllAssociations(@Param("id") Long id);

    @EntityGraph(attributePaths = {"variants"})
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdWithVariants(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"brand", "subCategory"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);






    @Query("""
            
            select distinct  p from Product p
            left join fetch p.brand
            join  fetch p.subCategory
            join fetch p.images
            where p.id =:id
            
            """)
    Optional<Product> findByIdWithDetails(@Param("id") long id);

    @Query("""
                select p from Product  p where p.id=:id
            """)
    Optional<Product> findProductWithBasicDetails(@Param("id") Long id);
}
