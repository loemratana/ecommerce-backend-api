package com.example.ecommerce_api.product.sevices.impl;


import com.example.ecommerce_api.Enum.CatalogStatus;
import com.example.ecommerce_api.Enum.OrderStatus;
import com.example.ecommerce_api.brand.entity.Brand;
import com.example.ecommerce_api.brand.repository.BrandRepository;
import com.example.ecommerce_api.category.entity.SubCategory;
import com.example.ecommerce_api.category.repository.SubCategoryRepository;
import com.example.ecommerce_api.common.exception.*;
import com.example.ecommerce_api.Enum.InventoryTransactionType;
import com.example.ecommerce_api.inventory.entity.Inventory;
import com.example.ecommerce_api.inventory.entity.InventoryTransaction;
import com.example.ecommerce_api.inventory.repository.InventoryTransactionRepository;
import com.example.ecommerce_api.product.dto.request.ProductImageRequest;
import com.example.ecommerce_api.product.dto.request.ProductRequest;
import com.example.ecommerce_api.product.dto.request.ProductVariantsRequest;
import com.example.ecommerce_api.product.dto.response.ProductResponse;
import com.example.ecommerce_api.product.dto.response.ProductSalesProjection;
import com.example.ecommerce_api.product.dto.response.ProductStatsResponse;
import com.example.ecommerce_api.product.dto.response.ProductSummaryResponse;
import com.example.ecommerce_api.product.entity.Product;
import com.example.ecommerce_api.product.entity.ProductImage;
import com.example.ecommerce_api.product.entity.ProductSize;
import com.example.ecommerce_api.product.entity.ProductVariants;
import com.example.ecommerce_api.product.mapper.ProductImageMapper;
import com.example.ecommerce_api.product.mapper.ProductMapper;
import com.example.ecommerce_api.product.mapper.ProductVariantMapper;
import com.example.ecommerce_api.product.productSpecification.ProductSpecification;
import com.example.ecommerce_api.product.repository.ProductRepository;
import com.example.ecommerce_api.product.repository.ProductSizeRepository;
import com.example.ecommerce_api.product.repository.ProductVariantRepository;
import com.example.ecommerce_api.product.sevices.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.core.support.PropertiesBasedNamedQueries;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private static final int MAX_LIMIT = 50;
    private final ProductRepository productRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;
    private final ProductVariantMapper productVariantMapper;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageMapper productImageMapper;
    private final ProductSizeRepository productSizeRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {

        validateSlugForCreate(request.getSlug());

        SubCategory subCategory = resolveSubCategory(request.getSubCategoryId());
        Brand brand = resolveBrand(request.getBrandId());

        Set<String> skus = validateVariants(request.getVariants());
        ensureSkusNotTaken(skus);
        validateImageColors(request.getImages(), request.getVariants());
        Map<Long, ProductSize> sizes = resolveSizes(request.getVariants());

        Product product = productMapper.toEntity(request);
        product.setSlug(request.getSlug());
        product.setSubCategory(subCategory);
        product.setBrand(brand);
        product.setVariants(buildVariants(product, request.getVariants(), sizes));
        product.setImages(buildProductImages(product, request.getImages()));

        Product savedProduct = productRepository.save(product);
        logInitialStock(savedProduct.getVariants());

        return productMapper.toResponse(savedProduct);
    }

    @Override
    public ProductResponse getById(Long id) {
        Product product = productRepository.findByIdWithAllAssociations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return productMapper.toResponse(product);
    }

    @Override
    public Page<ProductResponse> getAll(CatalogStatus status, String title, Long brandId, Long subCategoryId, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, BigDecimal minRating, Pageable pageable) {

        Specification<Product> specification = ProductSpecification.filter(status, title, brandId, subCategoryId, categoryId, minPrice, maxPrice, minRating);

        Page<Product> product = productRepository.findAll(specification, pageable);

        return product.map(productMapper::toResponse);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {

        Product product = productRepository.findByIdWithVariants(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        SubCategory subCategory = resolveSubCategory(request.getSubCategoryId());
        Brand brand = resolveBrand(request.getBrandId());
        validateSlugForUpdate(request.getSlug(), product);

        Set<String> skus = validateVariants(request.getVariants());
        ensureSkusNotTakenByOtherProduct(skus, id);
        validateImageColors(request.getImages(), request.getVariants());
        Map<Long, ProductSize> sizes = resolveSizes(request.getVariants());

        product.setTitle(request.getTitle());
        product.setSlug(request.getSlug());
        product.setDescription(request.getDescription());
        product.setImageCover(request.getImageCover());
        product.setSubCategory(subCategory);
        product.setBrand(brand);

        List<ProductVariants> addedVariants = updateVariants(product, request.getVariants(), sizes);
        updateImages(product, request.getImages());

        Product updatedProduct = productRepository.saveAndFlush(product);
        // save() merges, which persists copies of the new variants; log against those managed copies
        Set<String> addedSkus = addedVariants.stream()
                .map(ProductVariants::getSku)
                .collect(Collectors.toSet());
        logInitialStock(updatedProduct.getVariants().stream()
                .filter(v -> addedSkus.contains(v.getSku()))
                .toList());

        // Re-fetch to load all associations for mapping
        return getById(updatedProduct.getId());
    }

    @Override
    @Transactional
    public void archive(Long id) {
        // Never hard-deleted: order items, returns and inventory history reference its variants.
        // Open orders still ship normally - their stock stays reserved.
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setStatus(CatalogStatus.ARCHIVED);
    }

    @Override
    @Transactional
    public void restore(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setStatus(CatalogStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getNewArrivals(int days, int limit) {

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Product> products = productRepository.findNewArrivals(since, PageRequest.of(0, clamp(limit)));

        return productMapper.toProductSummary(products);
    }

    @Override
    public List<ProductSummaryResponse> getBestSellers(int days, int limit) {

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<ProductSalesProjection> sales = productRepository.findBestSellers(OrderStatus.revenueStatuses(),since,PageRequest.of(0,clamp(limit)));
        if (sales.isEmpty()) {
            return List.of();
        }

        List<Long> ids  = sales.stream().map(ProductSalesProjection::getProductId).toList();

        Map<Long,Product> byId = productRepository.findAllById(ids).stream().collect(Collectors.toMap(Product::getId,Function.identity()));

        return sales.stream()
                .filter(sale -> byId.containsKey(sale.getProductId()))
                .map(sale -> {
                    ProductSummaryResponse response = productMapper.toProductSummary(byId.get(sale.getProductId()));
                    response.setTotalSold(sale.getTotalSold());
                    return response;
                })
                .toList();
    }


    private int clamp (int limit)
    {
        return Math.max(1,Math.min(limit,MAX_LIMIT));
    }
    @Override
    public ProductStatsResponse getStats() {
        long totalProducts = productRepository.countByStatus(CatalogStatus.ACTIVE);

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long newThisMonth = productRepository.countByCreatedAtGreaterThanEqualAndStatus(startOfMonth, CatalogStatus.ACTIVE);

        // Stock comes from the variants' inventory (available = on hand - reserved).
        long activeProducts = productRepository.countByAvailableStockBetween(1, Long.MAX_VALUE);
        long lowStock = productRepository.countByAvailableStockBetween(1, LOW_STOCK_THRESHOLD);
        long outOfStock = productRepository.countByAvailableStockBetween(Long.MIN_VALUE, 0);

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
                .archivedProducts(productRepository.countByStatus(CatalogStatus.ARCHIVED))
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
        Set<String> colorSizes = new HashSet<>();
        for (ProductVariantsRequest variant : variants) {
            if (!skus.add(variant.getSku())) {
                throw new ProductDuplicateException("Duplicate SKU in request: " + variant.getSku());
            }
            // the customer picks a variant by color + size, so that pair must be unique
            if (!colorSizes.add(colorKey(variant.getColor()) + "|" + variant.getSizeId())) {
                throw new BadRequestException("Duplicate variant: color '" + variant.getColor()
                        + "' with size id " + variant.getSizeId() + " appears more than once");
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

    /** Every tagged image must belong to a color that actually exists among the variants. */
    private void validateImageColors(List<ProductImageRequest> images, List<ProductVariantsRequest> variants) {
        if (images == null) {
            return;
        }
        Set<String> variantColors = variants.stream()
                .map(v -> colorKey(v.getColor()))
                .collect(Collectors.toSet());
        for (ProductImageRequest image : images) {
            String color = colorKey(image.getColor());
            if (!color.isEmpty() && !variantColors.contains(color)) {
                throw new BadRequestException("Image color '" + image.getColor().trim()
                        + "' does not match any variant color");
            }
        }
    }

    private static String colorKey(String color) {
        return color == null ? "" : color.trim().toLowerCase(Locale.ROOT);
    }

    private Map<Long, ProductSize> resolveSizes(List<ProductVariantsRequest> variants) {
        Set<Long> sizeIds = variants.stream()
                .map(ProductVariantsRequest::getSizeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, ProductSize> sizes = productSizeRepository.findAllById(sizeIds).stream()
                .collect(Collectors.toMap(ProductSize::getId, Function.identity()));
        if (sizes.size() != sizeIds.size()) {
            sizeIds.removeAll(sizes.keySet());
            throw new ResourceNotFoundException("Product size(s) not found: " + sizeIds);
        }
        return sizes;
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

    private List<ProductVariants> buildVariants(Product product, List<ProductVariantsRequest> variantRequests,
                                                Map<Long, ProductSize> sizes) {
        List<ProductVariants> variants = new ArrayList<>();
        for (ProductVariantsRequest variantRequest : variantRequests) {
            ProductVariants variant = productVariantMapper.toEntity(variantRequest);
            variant.setProduct(product);
            variant.setSize(sizes.get(variantRequest.getSizeId()));
            // Every variant gets a stock record (persisted via cascade), optionally with opening stock
            variant.setInventory(new Inventory(variant, initialStock(variantRequest)));
            variants.add(variant);
        }
        return variants;
    }

    /**
     * Reconciles the product's existing variants with the incoming request: matched
     * variants (by id) are updated in place, unmatched requests become new variants,
     * and existing variants absent from the request are removed (orphanRemoval).
     */
    /** Returns the variants this request added, so their opening stock can be logged. */
    private List<ProductVariants> updateVariants(Product product, List<ProductVariantsRequest> variantRequests,
                                                 Map<Long, ProductSize> sizes) {
        List<ProductVariants> added = new ArrayList<>();
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

        // Removed variants are archived, never deleted: order items, cart items and stock
        // history point at them. One with stock reserved by open orders can't go yet.
        for (ProductVariants variant : product.getVariants()) {
            if (variant.getId() == null || !variant.isActive() || requestIds.contains(variant.getId())) {
                continue;
            }
            Inventory inventory = variant.getInventory();
            int reserved = inventory != null ? inventory.getReservedQuantity() : 0;
            if (reserved > 0) {
                throw new BusinessException("Variant " + variant.getSku() + " has " + reserved
                        + " unit(s) reserved by open orders; remove it after they ship or are cancelled");
            }
            variant.setStatus(CatalogStatus.ARCHIVED);
        }

        // Update existing / add new
        for (ProductVariantsRequest variantRequest : variantRequests) {
            if (variantRequest.getId() != null)
            {
                ProductVariants variants = existingVariants.get(variantRequest.getId());

                if (variants == null) {
                    throw new ResourceNotFoundException(
                            "Variant not found: " + variantRequest.getId()
                    );
                }
                productVariantMapper.updateEntity(variantRequest, variants);
                variants.setSize(sizes.get(variantRequest.getSizeId()));
                variants.setStatus(CatalogStatus.ACTIVE);
            }
            else {
                // Re-adding an archived SKU brings that variant back (with its stock history)
                // instead of colliding with it on the unique SKU.
                ProductVariants archived = product.getVariants().stream()
                        .filter(v -> !v.isActive() && v.getSku().equalsIgnoreCase(variantRequest.getSku()))
                        .findFirst().orElse(null);
                if (archived != null) {
                    productVariantMapper.updateEntity(variantRequest, archived);
                    archived.setSize(sizes.get(variantRequest.getSizeId()));
                    archived.setStatus(CatalogStatus.ACTIVE);
                    continue;
                }
                ProductVariants variant =
                        productVariantMapper.toEntity(variantRequest);

                variant.setProduct(product);
                variant.setSize(sizes.get(variantRequest.getSizeId()));
                variant.setInventory(new Inventory(variant, initialStock(variantRequest)));
                product.getVariants().add(variant);
                added.add(variant);
            }
        }
        return added;
    }

    private static int initialStock(ProductVariantsRequest request) {
        return request.getInitialStock() != null ? request.getInitialStock() : 0;
    }

    /** Records the opening stock of newly created variants as a PURCHASE, so the stock history explains every unit. */
    private void logInitialStock(List<ProductVariants> newVariants) {
        for (ProductVariants variant : newVariants) {
            Inventory inventory = variant.getInventory();
            if (inventory == null || inventory.getQuantity() <= 0) {
                continue;
            }
            inventoryTransactionRepository.save(InventoryTransaction.builder()
                    .inventory(inventory)
                    .type(InventoryTransactionType.PURCHASE)
                    .quantityChange(inventory.getQuantity())
                    .note("Initial stock")
                    .build());
        }
    }

    private  List<ProductImage>  buildProductImages(Product product, List<ProductImageRequest> requests) {


        List<ProductImage> productImages = new ArrayList<>();
        if (requests == null) {
            return productImages;   // never null: the collection is orphanRemoval-managed
        }
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
