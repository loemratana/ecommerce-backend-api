package com.example.ecommerce_api.product;

import com.example.ecommerce_api.Enum.CatalogStatus;
import com.example.ecommerce_api.Enum.ProductType;
import com.example.ecommerce_api.product.entity.Product;
import com.example.ecommerce_api.product.repository.ProductRepository;
import com.example.ecommerce_api.product.sevices.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/** Runs the product type filter and related-products query against the real database. */
@SpringBootTest
@Transactional
class ProductQueryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Test
    void typeFilterRunsForEveryType() {
        for (ProductType type : ProductType.values()) {
            assertThatNoException().isThrownBy(() -> productService.getAll(
                    CatalogStatus.ACTIVE, type, null, null, null, null, null, null, null, PageRequest.of(0, 5)));
        }
    }

    @Test
    void relatedProductsExcludeTheProductAndMatchType() {
        Product product = productRepository.findAll(PageRequest.of(0, 1)).stream().findFirst().orElse(null);
        if (product == null) {
            return; // empty database, nothing to relate
        }
        product.setProductType(ProductType.MEN);

        var related = productService.getRelated(product.getId(), 8);

        assertThat(related).hasSizeLessThanOrEqualTo(8);
        assertThat(related).allSatisfy(r -> {
            assertThat(r.getId()).isNotEqualTo(product.getId());
            assertThat(r.getProductType()).isNotEqualTo(ProductType.WOMEN);
        });
    }
}
