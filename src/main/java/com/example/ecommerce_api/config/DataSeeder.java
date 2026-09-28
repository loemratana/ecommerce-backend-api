package com.example.ecommerce_api.config;

import com.example.ecommerce_api.brand.entity.Brand;
import com.example.ecommerce_api.brand.repository.BrandRepository;
import com.example.ecommerce_api.category.entity.Category;
import com.example.ecommerce_api.category.entity.SubCategory;
import com.example.ecommerce_api.category.repository.CategoryRepository;
import com.example.ecommerce_api.category.repository.SubCategoryRepository;
import com.example.ecommerce_api.coupon.entity.Coupon;
import com.example.ecommerce_api.coupon.repository.CouponRepository;
import com.example.ecommerce_api.permission.entity.Permission;
import com.example.ecommerce_api.permission.repository.PermissionRepository;
import com.example.ecommerce_api.product.entity.Product;
import com.example.ecommerce_api.product.entity.ProductImage;
import com.example.ecommerce_api.product.entity.ProductSize;
import com.example.ecommerce_api.product.entity.ProductVariants;
import com.example.ecommerce_api.product.repository.ProductRepository;
import com.example.ecommerce_api.product.repository.ProductSizeRepository;
import com.example.ecommerce_api.review.entity.Review;
import com.example.ecommerce_api.review.repository.ReviewRepository;
import com.example.ecommerce_api.user.entity.Address;
import com.example.ecommerce_api.user.entity.Role;
import com.example.ecommerce_api.user.entity.User;
import com.example.ecommerce_api.user.repository.RoleRepository;
import com.example.ecommerce_api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Adds up to 5 sample rows per module to whatever database is already there.
 * Disabled by default - enable with app.seed.enabled=true (or APP_SEED_ENABLED=true).
 * Every row is looked up by its natural key (name/slug/email/code) first, so re-running
 * this against a database that already has some of these rows reuses them instead of
 * erroring on a unique-constraint violation or creating duplicates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final String SEED_PASSWORD = "Password123!";

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final BrandRepository brandRepository;
    private final ProductSizeRepository productSizeRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {

        log.info("Seeding sample data (up to 5 rows per module, reusing anything that already exists)...");

        List<Permission> permissions = seedPermissions();
        List<Role> roles = seedRoles(permissions);
        List<User> users = seedUsers(roles);
        List<Category> categories = seedCategories();
        List<SubCategory> subCategories = seedSubCategories(categories);
        List<Brand> brands = seedBrands();
        List<ProductSize> sizes = seedProductSizes();
        List<Product> products = seedProducts(subCategories, brands, sizes);
        seedCoupons();
        seedReviews(users, products);

        log.info("Seeding complete.");
    }

    private List<Permission> seedPermissions() {
        return List.of(
                findOrCreatePermission("PRODUCT_CREATE", "PRODUCT", "CREATE", "Create products"),
                findOrCreatePermission("PRODUCT_READ", "PRODUCT", "READ", "View products"),
                findOrCreatePermission("ORDER_MANAGE", "ORDER", "MANAGE", "Manage orders"),
                findOrCreatePermission("USER_MANAGE", "USER", "MANAGE", "Manage users"),
                findOrCreatePermission("ROLE_MANAGE", "ROLE", "MANAGE", "Manage roles and permissions")
        );
    }

    private Permission findOrCreatePermission(String name, String resource, String action, String description) {
        return permissionRepository.findByNameIgnoreCase(name).orElseGet(() ->
                permissionRepository.save(Permission.builder()
                        .name(name)
                        .resource(resource)
                        .action(action)
                        .description(description)
                        .build()));
    }

    private List<Role> seedRoles(List<Permission> permissions) {
        Role admin = findOrCreateRole("ADMIN", "Full access to the system");
        Role customer = findOrCreateRole("CUSTOMER", "Regular shopper");
        Role manager = findOrCreateRole("MANAGER", "Manages catalog and orders");
        Role support = findOrCreateRole("SUPPORT", "Customer support agent");
        Role vendor = findOrCreateRole("VENDOR", "Third-party product vendor");

        permissions.forEach(admin::addPermission);
        permissions.stream()
                .filter(p -> p.getResource().equals("PRODUCT") || p.getResource().equals("ORDER"))
                .forEach(manager::addPermission);

        return roleRepository.saveAll(List.of(admin, customer, manager, support, vendor));
    }

    private Role findOrCreateRole(String name, String description) {
        return roleRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            role.setActive(true);
            return role;
        });
    }

    private List<User> seedUsers(List<Role> roles) {
        Role admin = findRole(roles, "ADMIN");
        Role customer = findRole(roles, "CUSTOMER");
        Role support = findRole(roles, "SUPPORT");
        Role vendor = findRole(roles, "VENDOR");

        User u1 = findOrCreateUser("admin.seed@ecommerce.test", "Admin", "User", "+1 555-000-0001", admin,
                "100 Admin Way", "Metropolis", "NY", "10001", "USA");
        User u2 = findOrCreateUser("john.doe.seed@ecommerce.test", "John", "Doe", "+1 555-000-0002", customer,
                "221B Baker Street", "London", "LDN", "NW1", "UK");
        User u3 = findOrCreateUser("jane.smith.seed@ecommerce.test", "Jane", "Smith", "+1 555-000-0003", customer,
                "742 Evergreen Terrace", "Springfield", "IL", "62704", "USA");
        User u4 = findOrCreateUser("mike.brown.seed@ecommerce.test", "Mike", "Brown", "+1 555-000-0004", vendor,
                "1 Infinite Loop", "Cupertino", "CA", "95014", "USA");
        User u5 = findOrCreateUser("sara.lee.seed@ecommerce.test", "Sara", "Lee", "+1 555-000-0005", support,
                "10 Downing Street", "London", "LDN", "SW1A", "UK");

        return userRepository.saveAll(List.of(u1, u2, u3, u4, u5));
    }

    private User findOrCreateUser(String email, String firstName, String lastName, String phone, Role role,
                                   String street, String city, String state, String zipCode, String country) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setFirstName(firstName);
            u.setLastName(lastName);
            u.setPhone(phone);
            u.setPassword(passwordEncoder.encode(SEED_PASSWORD));
            u.setActive(true);
            return u;
        });

        if (user.getRoles().isEmpty()) {
            user.addRole(role);
        }
        if (user.getAddresses().isEmpty()) {
            user.addAddress(newAddress(street, city, state, zipCode, country));
        }
        return user;
    }

    private Address newAddress(String street, String city, String state, String zipCode, String country) {
        Address address = new Address();
        address.setStreet(street);
        address.setCity(city);
        address.setState(state);
        address.setZipCode(zipCode);
        address.setCountry(country);
        address.setDefaultAddress(true);
        return address;
    }

    private Role findRole(List<Role> roles, String name) {
        return roles.stream().filter(r -> r.getName().equalsIgnoreCase(name)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Seed role not found: " + name));
    }

    private List<Category> seedCategories() {
        return List.of(
                findOrCreateCategory("Electronics", "electronics"),
                findOrCreateCategory("Fashion", "fashion"),
                findOrCreateCategory("Home & Kitchen", "home-kitchen"),
                findOrCreateCategory("Sports & Outdoors", "sports-outdoors"),
                findOrCreateCategory("Beauty & Health", "beauty-health")
        );
    }

    private Category findOrCreateCategory(String name, String slug) {
        return categoryRepository.findBySlug(slug).orElseGet(() -> {
            Category category = new Category();
            category.setName(name);
            category.setSlug(slug);
            return categoryRepository.save(category);
        });
    }

    private List<SubCategory> seedSubCategories(List<Category> categories) {
        return List.of(
                findOrCreateSubCategory("Smartphones", "smartphones", categories.get(0)),
                findOrCreateSubCategory("Men's Clothing", "mens-clothing", categories.get(1)),
                findOrCreateSubCategory("Cookware", "cookware", categories.get(2)),
                findOrCreateSubCategory("Fitness Equipment", "fitness-equipment", categories.get(3)),
                findOrCreateSubCategory("Skincare", "skincare", categories.get(4))
        );
    }

    private SubCategory findOrCreateSubCategory(String name, String slug, Category category) {
        return subCategoryRepository.findBySlug(slug).orElseGet(() -> {
            SubCategory subCategory = new SubCategory();
            subCategory.setName(name);
            subCategory.setSlug(slug);
            subCategory.setCategory(category);
            return subCategoryRepository.save(subCategory);
        });
    }

    private List<Brand> seedBrands() {
        return List.of(
                findOrCreateBrand("Apple", "apple"),
                findOrCreateBrand("Nike", "nike"),
                findOrCreateBrand("Samsung", "samsung"),
                findOrCreateBrand("IKEA", "ikea"),
                findOrCreateBrand("L'Oreal", "loreal")
        );
    }

    private Brand findOrCreateBrand(String name, String slug) {
        return brandRepository.findBySlug(slug).orElseGet(() -> {
            Brand brand = new Brand();
            brand.setName(name);
            brand.setSlug(slug);
            return brandRepository.save(brand);
        });
    }

    private List<ProductSize> seedProductSizes() {
        return List.of(
                findOrCreateSize("S"), findOrCreateSize("M"), findOrCreateSize("L"),
                findOrCreateSize("XL"), findOrCreateSize("One Size")
        );
    }

    private ProductSize findOrCreateSize(String name) {
        return productSizeRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            ProductSize size = new ProductSize();
            size.setName(name);
            return productSizeRepository.save(size);
        });
    }

    private List<Product> seedProducts(List<SubCategory> subCategories, List<Brand> brands, List<ProductSize> sizes) {

        Product p1 = findOrCreateProduct("iPhone 15", "iphone-15",
                "Apple's latest smartphone with A16 chip.",
                "https://images.example.com/products/iphone-15.jpg",
                subCategories.get(0), brands.get(0),
                "Black", "IPH15-BLK-128", new BigDecimal("999.99"), sizes.get(4),
                "https://images.example.com/products/iphone-15-1.jpg");

        Product p2 = findOrCreateProduct("Nike Air Max 90", "nike-air-max-90",
                "Classic Nike sneakers with visible air cushioning.",
                "https://images.example.com/products/nike-air-max-90.jpg",
                subCategories.get(1), brands.get(1),
                "White", "NIKE-AM90-WHT-42", new BigDecimal("129.99"), sizes.get(1),
                "https://images.example.com/products/nike-air-max-90-1.jpg");

        Product p3 = findOrCreateProduct("Samsung Galaxy S24", "samsung-galaxy-s24",
                "Flagship Samsung smartphone with AI features.",
                "https://images.example.com/products/galaxy-s24.jpg",
                subCategories.get(0), brands.get(2),
                "Silver", "SGS24-SLV-256", new BigDecimal("899.99"), sizes.get(4),
                "https://images.example.com/products/galaxy-s24-1.jpg");

        Product p4 = findOrCreateProduct("IKEA Non-Stick Pan Set", "ikea-non-stick-pan-set",
                "3-piece non-stick cookware set.",
                "https://images.example.com/products/ikea-pan-set.jpg",
                subCategories.get(2), brands.get(3),
                "Black", "IKEA-PAN-BLK-L", new BigDecimal("49.99"), sizes.get(2),
                "https://images.example.com/products/ikea-pan-set-1.jpg");

        Product p5 = findOrCreateProduct("L'Oreal Revitalift Serum", "loreal-revitalift-serum",
                "Anti-aging facial serum with hyaluronic acid.",
                "https://images.example.com/products/loreal-serum.jpg",
                subCategories.get(4), brands.get(4),
                "Standard", "LOREAL-SERUM-30ML", new BigDecimal("24.99"), sizes.get(0),
                "https://images.example.com/products/loreal-serum-1.jpg");

        return List.of(p1, p2, p3, p4, p5);
    }

    private Product findOrCreateProduct(String title, String slug, String description, String imageCover,
                                         SubCategory subCategory, Brand brand,
                                         String variantColor, String sku, BigDecimal price, ProductSize size,
                                         String imageUrl) {
        return productRepository.findBySlug(slug).orElseGet(() -> {
            Product product = new Product();
            product.setTitle(title);
            product.setSlug(slug);
            product.setDescription(description);
            product.setImageCover(imageCover);
            product.setRatingsAverage(BigDecimal.ZERO);
            product.setRatingsQuantity(0);
            product.setSubCategory(subCategory);
            product.setBrand(brand);

            ProductVariants variant = new ProductVariants();
            variant.setColor(variantColor);
            variant.setSku(sku);
            variant.setPrice(price);
            variant.setPriceAfterDiscount(price);
            variant.setSoldQuantity(0);
            variant.setSize(size);
            variant.setProduct(product);
            product.getVariants().add(variant);

            ProductImage image = new ProductImage();
            image.setImageUrl(imageUrl);
            image.setSortOrder(0);
            image.setProduct(product);
            product.getImages().add(image);

            return productRepository.save(product);
        });
    }

    private void seedCoupons() {
        List<Coupon> coupons = List.of(
                findOrBuildCoupon("WELCOME10", new BigDecimal("10.00")),
                findOrBuildCoupon("SAVE15", new BigDecimal("15.00")),
                findOrBuildCoupon("SUMMER20", new BigDecimal("20.00")),
                findOrBuildCoupon("BLACKFRIDAY30", new BigDecimal("30.00")),
                findOrBuildCoupon("NEWYEAR25", new BigDecimal("25.00"))
        );
        couponRepository.saveAll(coupons);
    }

    private Coupon findOrBuildCoupon(String code, BigDecimal discountPercent) {
        return couponRepository.findByCodeIgnoreCase(code).orElseGet(() -> {
            Coupon coupon = new Coupon();
            coupon.setCode(code);
            coupon.setDiscountPercent(discountPercent);
            coupon.setExpiryDate(LocalDateTime.now().plusDays(90));
            coupon.setActive(true);
            return coupon;
        });
    }

    private void seedReviews(List<User> users, List<Product> products) {
        addReviewIfMissing("Great phone", new BigDecimal("4.5"), "Battery life is excellent.", users.get(1), products.get(0));
        addReviewIfMissing("Comfortable", new BigDecimal("5.0"), "Best sneakers I've owned.", users.get(2), products.get(1));
        addReviewIfMissing("Solid upgrade", new BigDecimal("4.0"), "Camera is a big step up.", users.get(3), products.get(2));
        addReviewIfMissing("Good value", new BigDecimal("3.5"), "Non-stick coating wears over time.", users.get(4), products.get(3));
        addReviewIfMissing("Noticeable results", new BigDecimal("4.8"), "Skin feels smoother after two weeks.", users.get(0), products.get(4));
    }

    private void addReviewIfMissing(String title, BigDecimal ratings, String comment, User user, Product product) {
        if (reviewRepository.existsByUserIdAndProductId(user.getId(), product.getId())) {
            return;
        }
        Review review = new Review();
        review.setTitle(title);
        review.setRatings(ratings);
        review.setComment(comment);
        review.setUser(user);
        review.setProduct(product);
        reviewRepository.save(review);
    }
}
