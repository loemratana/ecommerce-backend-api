package com.example.ecommerce_api.product.sevices.impl;


import com.example.ecommerce_api.brand.entity.Brand;
import com.example.ecommerce_api.brand.repository.BrandRepository;
import com.example.ecommerce_api.category.entity.SubCategory;
import com.example.ecommerce_api.category.repository.SubCategoryRepository;
import com.example.ecommerce_api.common.exception.*;
import com.example.ecommerce_api.product.dto.request.ProductImageRequest;
import com.example.ecommerce_api.product.dto.request.ProductRequest;
import com.example.ecommerce_api.product.dto.request.ProductVariantsRequest;
import com.example.ecommerce_api.product.dto.response.ProductResponse;
import com.example.ecommerce_api.product.dto.response.ProductStatsResponse;
import com.example.ecommerce_api.product.entity.Product;
import com.example.ecommerce_api.product.entity.ProductImage;
import com.example.ecommerce_api.product.entity.ProductVariants;
import com.example.ecommerce_api.product.mapper.ProductImageMapper;
import com.example.ecommerce_api.product.mapper.ProductMapper;
import com.example.ecommerce_api.product.mapper.ProductVariantMapper;
import com.example.ecommerce_api.product.productSpecification.ProductSpecification;
import com.example.ecommerce_api.product.repository.ProductRepository;
import com.example.ecommerce_api.product.repository.ProductVariantRepository;
import com.example.ecommerce_api.product.sevices.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.core.support.PropertiesBasedNamedQueries;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private static final int LOW_STOCK_THRESHOLD = 10;

    private final ProductRepository productRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;
    private final ProductVariantMapper productVariantMapper;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageMapper productImageMapper;

    @Override
    public ProductResponse create(ProductRequest request) {

        validateSlugForCreate(request.getSlug());

        SubCategory subCategory = resolveSubCategory(request.getSubCategoryId());
        Brand brand = resolveBrand(request.getBrandId());

        Set<String> skus = validateVariants(request.getVariants());
        ensureSkusNotTaken(skus);

        Product product = productMapper.toEntity(request);
        product.setSlug(request.getSlug());
        product.setSubCategory(subCategory);
        product.setBrand(brand);
        product.setVariants(buildVariants(product, request.getVariants()));
        product.setImages(buildProductImages(product, request.getImages()));

        Product savedProduct = productRepository.save(product);

        return productMapper.toResponse(savedProduct);
    }

    @Override
    public ProductResponse getById(Long id) {
        Product product = productRepository.findByIdWithAllAssociations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return productMapper.toResponse(product);
    }

    @Override
    public Page<ProductResponse> getAll(String title, Long brandId, Long subCategoryId, BigDecimal minPrice, BigDecimal maxPrice, BigDecimal minRating, Pageable pageable) {

        Specification<Product> specification = ProductSpecification.filter(title, brandId, subCategoryId, minPrice, maxPrice, minRating);

        Page<Product> product = productRepository.findAll(specification, pageable);

        return product.map(productMapper::toResponse);
    }

    @Override
    public ProductResponse update(Long id, ProductRequest request) {

        Product product = productRepository.findByIdWithVariants(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        SubCategory subCategory = resolveSubCategory(request.getSubCategoryId());
        Brand brand = resolveBrand(request.getBrandId());
        validateSlugForUpdate(request.getSlug(), product);

        Set<String> skus = validateVariants(request.getVariants());
        ensureSkusNotTakenByOtherProduct(skus, id);

        product.setTitle(request.getTitle());
        product.setSlug(request.getSlug());
        product.setDescription(request.getDescription());
        product.setImageCover(request.getImageCover());
        product.setSubCategory(subCategory);
        product.setBrand(brand);

        updateVariants(product, request.getVariants());

        Product updatedProduct = productRepository.save(product);

        // Re-fetch to load all associations for mapping
        return getById(updatedProduct.getId());
    }

    @Override
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        productRepository.delete(product);
    }

    @Override
    public ProductStatsResponse getStats() {
        long totalProducts = productRepository.count();

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long newThisMonth = productRepository.countByCreatedAtGreaterThanEqual(startOfMonth);

        long activeProducts = productRepository.countByStockQuantityGreaterThan(0);
        long lowStock = productRepository.countByStockQuantityLessThanEqualAndStockQuantityGreaterThan(
                LOW_STOCK_THRESHOLD, 0
        );
        long outOfStock = productRepository.countByStockQuantityIsNullOrStockQuantityLessThanEqual(0);

        double activeProductsPercent = totalProducts == 0
                ? 0.0
                : BigDecimal.valueOf(activeProducts  * 100.0/totalProducts)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
                ;

        return ProductStatsResponse.builder()
                .totalProducts(totalProducts)
                .newThisMonth(newThisMonth)
                .activeProducts(activeProducts)
                .activeProductsPercent(activeProductsPercent)
                .lowStock(lowStock)
                .outOfStock(outOfStock)
                .build();
    }

    // ---------------------------------------------------------------------
    // Validation helpers
    // ---------------------------------------------------------------------

    private void validateSlugForCreate(String slug) {
        if (productRepository.existsBySlug(slug)) {
            throw new ProductDuplicateException("Product with slug " + slug + " already exists");
        }
    }

    private void validateSlugForUpdate(String slug, Product product) {
        if (slug != null && !slug.equals(product.getSlug()) && productRepository.existsBySlugAndIdNot(slug, product.getId())) {
            throw new DuplicateResourceException("Slug already exists: " + slug);
        }
    }

    private SubCategory resolveSubCategory(Long subCategoryId) {
        return subCategoryRepository.findById(subCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub category with id " + subCategoryId + " does not exist"));
    }

    private Brand resolveBrand(Long brandId) {
        if (brandId == null) {
            return null;
        }
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand with id " + brandId + " does not exist"));
    }

    /**
     * Validates the variant list (non-empty, no duplicate SKUs within the request,
     * discount price not greater than price) and returns the set of requested SKUs.
     */
    private Set<String> validateVariants(List<ProductVariantsRequest> variants) {
        if (variants == null || variants.isEmpty()) {
            throw new BadRequestException("Product must have at least one variant");
        }

        Set<String> skus = new HashSet<>();
        for (ProductVariantsRequest variant : variants) {
            if (!skus.add(variant.getSku())) {
                throw new ProductDuplicateException("Duplicate SKU in request: " + variant.getSku());
            }
            if (variant.getPriceAfterDiscount() != null && variant.getPrice() != null
                    && variant.getPriceAfterDiscount().compareTo(variant.getPrice()) > 0) {
                throw new BadRequestException(
                        "Discount price cannot be greater than price for SKU: " + variant.getSku()
                );
            }
        }
        return skus;
    }

    private void ensureSkusNotTaken(Set<String> skus) {
        for (String sku : skus) {
            if (productVariantRepository.existsBySku(sku)) {
                throw new ProductDuplicateException("Product with sku " + sku + " already exists");
            }
        }
    }

    private void ensureSkusNotTakenByOtherProduct(Set<String> skus, Long productId) {
        if (productVariantRepository.existsBySkuInAndProduct_IdNot(new ArrayList<>(skus), productId)) {
            throw new DuplicateResourceException("One or more SKUs are already used by another product");
        }
    }

    // ---------------------------------------------------------------------
    // Variant assembly helpers
    // ---------------------------------------------------------------------

    private List<ProductVariants> buildVariants(Product product, List<ProductVariantsRequest> variantRequests) {
        List<ProductVariants> variants = new ArrayList<>();
        for (ProductVariantsRequest variantRequest : variantRequests) {
            ProductVariants variant = productVariantMapper.toEntity(variantRequest);
            variant.setProduct(product);
            variants.add(variant);
        }
        return variants;
    }

    /**
     * Reconciles the product's existing variants with the incoming request: matched
     * variants (by id) are updated in place, unmatched requests become new variants,
     * and existing variants absent from the request are removed (orphanRemoval).
     */
    private void updateVariants(Product product, List<ProductVariantsRequest> variantRequests) {
        Map<Long,ProductVariants> existingVariants = product.getVariants()
                .stream()
                .filter(v->v.getId() !=null)
                .collect(Collectors.toMap(
                        ProductVariants::getId,
                        Function.identity()
                ));

        Set<Long> requestIds = variantRequests
                .stream()
                .map(ProductVariantsRequest::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        product.getVariants().removeIf(
                v->v.getId()!=null && !requestIds.contains(v.getId())

        );

        // Update existing / add new
        for (ProductVariantsRequest variantRequest : variantRequests) {
            if (variantRequest !=null)
            {
                ProductVariants variants = existingVariants.get(variantRequest.getId());

                if (variants == null) {
                    throw new ResourceNotFoundException(
                            "Variant not found: " + variantRequest.getId()
                    );
                }
                productVariantMapper.updateEntity(variantRequest, variants);
            }
            else {
                ProductVariants variant =
                        productVariantMapper.toEntity(variantRequest);

                variant.setProduct(product);
                product.getVariants().add(variant);
            }
        }
    }

    private  List<ProductImage>  buildProductImages(Product product, List<ProductImageRequest> requests) {


        if (requests == null || requests.isEmpty()) {
            return null;
        }
        List<ProductImage> productImages = new ArrayList<>();
        for (ProductImageRequest request : requests)
        {

            ProductImage image = productImageMapper.toEntity(request);
            image.setProduct(product);
            productImages.add(image);
        }

        return productImages;

    }

    private void  updateImages(Product product, List<ProductImageRequest> imageRequests ) {
        Map<Long, ProductImage> existingById = new HashMap<>();
        for (ProductImage existing : product.getImages()) {
            if (existing.getId() != null) {
                existingById.put(existing.getId(), existing);
            }
        }

        List<ProductImage> reconciled = new ArrayList<>();

        if (imageRequests !=null)
        {
            for (ProductImageRequest imageRequest : imageRequests) {
                ProductImage image = imageRequest.getId() !=null? existingById.remove(imageRequest.getId()) : null;
                if (image ==null)
                {
                    image =  productImageMapper.toEntity(imageRequest);
                    image.setProduct(product);
                }
                else {
                    productImageMapper.updateEntity(imageRequest,image);
                }
                reconciled.add(image);
            }
        }

        product.getImages().clear();
        product.getImages().addAll(reconciled);

    }
}
