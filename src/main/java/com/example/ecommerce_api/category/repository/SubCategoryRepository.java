package com.example.ecommerce_api.category.repository;


import com.example.ecommerce_api.category.entity.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {

    boolean existsBySlugIgnoreCase(String slug);



    @Query("""
                    select distinct  sc from  SubCategory sc 
                    join fetch sc.category
                    left join fetch sc.products
                    where sc.id =:id 
            """)
    Optional<SubCategory> findDetailsById(@Param("id") Long id);

    boolean existsByCategory_IdAndNameIgnoreCase(Long categoryId, String name);

    boolean existsByCategory_Id(Long categoryId);

    Optional<SubCategory> findBySlug(String slug);

    List<SubCategory> findByCategory_Id(Long categoryId);
}
