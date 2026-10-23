package com.example.ecommerce_api.product.repository;


import com.example.ecommerce_api.Enum.CatalogStatus;
import com.example.ecommerce_api.Enum.OrderStatus;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.product.dto.response.ProductSalesProjection;
import com.example.ecommerce_api.product.dto.response.ProductSummaryResponse;
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsByBrand_Id(Long brandId);

    long countByCreatedAtGreaterThanEqualAndStatus(LocalDateTime from, CatalogStatus status);

    long countByStatus(CatalogStatus status);

    /**
     * Products whose total available stock (sum over variants of quantity - reserved) is in
     * [min, max]. A product without inventory counts as 0.
     */
    @Query("""
            select count(p) from Product p
            where p.status = com.example.ecommerce_api.Enum.CatalogStatus.ACTIVE
              and (select coalesce(sum(i.quantity - i.reservedQuantity), 0)
                   from Inventory i where i.productVariant.product = p
                     and i.productVariant.status = com.example.ecommerce_api.Enum.CatalogStatus.ACTIVE)
                  between :min and :max
            """)
    long countByAvailableStockBetween(@Param("min") long min, @Param("max") long max);

    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    Optional<Product> findBySlug(String slug);

    boolean existsBySubCategory_Id(Long subCategoryId);


    @Query("""
        select  p from Product p where p.createdAt >=:since
        and p.status = com.example.ecommerce_api.Enum.CatalogStatus.ACTIVE
        and exists (select 1 from Inventory i
                    where i.productVariant.product = p and i.quantity - i.reservedQuantity > 0
                      and i.productVariant.status = com.example.ecommerce_api.Enum.CatalogStatus.ACTIVE)
        order by p.createdAt desc
""")
    List<Product> findNewArrivals(@Param("since") LocalDateTime since , Pageable pageable);






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





    @Query(
            """
select  p.id as productId, sum(oi.quantity) as totalSold
from OrderItem  oi 
join oi.order o
join oi.productVariant v 
join v.product p
where o.status in:statuses
and o.orderDate>:since
and p.status = CatalogStatus.ACTIVE
group by p.id
order by sum(oi.quantity) desc 
"""
    )
    List<ProductSalesProjection>findBestSellers(@Param("statuses")Collection<OrderStatus> statuses,@Param("since") LocalDateTime since,
                                                Pageable pageable);
}
