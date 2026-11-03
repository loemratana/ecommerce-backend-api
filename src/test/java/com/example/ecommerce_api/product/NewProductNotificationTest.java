package com.example.ecommerce_api.product;

import com.example.ecommerce_api.brand.repository.BrandRepository;
import com.example.ecommerce_api.category.entity.SubCategory;
import com.example.ecommerce_api.category.repository.SubCategoryRepository;
import com.example.ecommerce_api.common.exception.ProductDuplicateException;
import com.example.ecommerce_api.notification.service.NotificationService;
import com.example.ecommerce_api.product.dto.request.ProductRequest;
import com.example.ecommerce_api.product.dto.request.ProductVariantsRequest;
import com.example.ecommerce_api.product.dto.response.ProductResponse;
import com.example.ecommerce_api.product.entity.Product;
import com.example.ecommerce_api.product.entity.ProductVariants;
import com.example.ecommerce_api.product.mapper.ProductMapper;
import com.example.ecommerce_api.product.mapper.ProductVariantMapper;
import com.example.ecommerce_api.product.repository.ProductRepository;
import com.example.ecommerce_api.product.repository.ProductSizeRepository;
import com.example.ecommerce_api.product.repository.ProductVariantRepository;
import com.example.ecommerce_api.product.sevices.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** create() asks for a NEW_PRODUCT notification only for a product that was actually created. No database. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NewProductNotificationTest {

    @Mock ProductRepository productRepository;
    @Mock SubCategoryRepository subCategoryRepository;
    @Mock BrandRepository brandRepository;
    @Mock ProductMapper productMapper;
    @Mock ProductVariantMapper productVariantMapper;
    @Mock ProductVariantRepository productVariantRepository;
    @Mock ProductSizeRepository productSizeRepository;
    @Mock NotificationService notificationService;
    @InjectMocks ProductServiceImpl productService;

    private final ProductResponse response = ProductResponse.builder().title("Basic Tee").slug("basic-tee").build();
    private ProductRequest request;

    @BeforeEach
    void setUp() {
        ProductVariantsRequest variant = new ProductVariantsRequest();
        variant.setSku("TEE-M-BLK");
        variant.setColor("Black");
        variant.setPrice(new BigDecimal("10.00"));
        variant.setInitialStock(0);

        request = new ProductRequest();
        request.setTitle("Basic Tee");
        request.setSlug("basic-tee");
        request.setSubCategoryId(2L);
        request.setVariants(List.of(variant));

        when(subCategoryRepository.findById(2L)).thenReturn(Optional.of(new SubCategory()));
        when(productMapper.toEntity(any())).thenReturn(new Product());
        when(productVariantMapper.toEntity(any())).thenAnswer(i -> new ProductVariants());
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(productMapper.toResponse(any(Product.class))).thenReturn(response);
    }

    @Test
    void createdProductTriggersTheNewProductNotification() {
        ProductResponse created = productService.create(request);

        assertThat(created).isSameAs(response);
        verify(notificationService).notifyNewProduct(response);
    }

    @Test
    void rejectedProductIsNotNotified() {
        when(productRepository.existsBySlug("basic-tee")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request)).isInstanceOf(ProductDuplicateException.class);
        verify(notificationService, never()).notifyNewProduct(any());
    }
}
