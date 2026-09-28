ecommerce-api/
│
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
│
└── src/
│
├── main/
│   │
│   ├── java/
│   │   └── com/example/ecommerce/
│   │       │
│   │       ├── EcommerceApplication.java
│   │       │
│   │       ├── config/
│   │       │   ├── JpaConfig.java
│   │       │   ├── JacksonConfig.java
│   │       │   └── OpenApiConfig.java
│   │       │
│   │       ├── security/
│   │       │   ├── SecurityConfig.java
│   │       │   ├── JwtAuthenticationFilter.java
│   │       │   ├── JwtService.java
│   │       │   └── CustomUserDetailsService.java
│   │       │
│   │       ├── common/
│   │       │   ├── exception/
│   │       │   │   ├── GlobalExceptionHandler.java
│   │       │   │   ├── ResourceNotFoundException.java
│   │       │   │   ├── BadRequestException.java
│   │       │   │   └── UnauthorizedException.java
│   │       │   │
│   │       │   ├── response/
│   │       │   │   ├── ApiResponse.java
│   │       │   │   └── PageResponse.java
│   │       │   │
│   │       │   └── enums/
│   │       │       ├── OrderStatus.java
│   │       │       ├── PaymentMethod.java
│   │       │       └── PaymentStatus.java
│   │       │
│   │       ├── auth/
│   │       │   ├── controller/
│   │       │   │   └── AuthController.java
│   │       │   ├── dto/
│   │       │   │   ├── LoginRequest.java
│   │       │   │   ├── LoginResponse.java
│   │       │   │   ├── RegisterRequest.java
│   │       │   │   └── RefreshTokenRequest.java
│   │       │   └── service/
│   │       │       └── AuthService.java
│   │       │
│   │       ├── user/
│   │       │   ├── controller/
│   │       │   │   └── 
│   │       │   ├── dto/
│   │       │   │   ├── UserRequest.java
│   │       │   │   └── UserResponse.java
│   │       │   ├── entity/
│   │       │   │   ├── User.java
│   │       │   │   └── UserAddress.java
│   │       │   ├── mapper/
│   │       │   │   └── UserMapper.java
│   │       │   ├── repository/
│   │       │   │   ├── UserRepository.java
│   │       │   │   └── UserAddressRepository.java
│   │       │   └── service/
│   │       │       ├── UserService.java
│   │       │       └── impl/
│   │       │           └── UserServiceImpl.java
│   │       │
│   │       ├── category/
│   │       │   ├── controller/
│   │       │   ├── dto/
│   │       │   ├── entity/
│   │       │   │   ├── Category.java
│   │       │   │   └── SubCategory.java
│   │       │   ├── mapper/
│   │       │   ├── repository/
│   │       │   └── service/
│   │       │
│   │       ├── brand/
│   │       │   ├── controller/
│   │       │   ├── dto/
│   │       │   ├── entity/
│   │       │   │   └── Brand.java
│   │       │   ├── mapper/
│   │       │   ├── repository/
│   │       │   └── service/
│   │       │
│   │       ├── product/
│   │       │   ├── controller/
│   │       │   │   └── ProductController.java
│   │       │   ├── dto/
│   │       │   │   ├── ProductRequest.java
│   │       │   │   ├── ProductResponse.java
│   │       │   │   ├── ProductVariantRequest.java
│   │       │   │   └── ProductVariantResponse.java
│   │       │   ├── entity/
│   │       │   │   ├── Product.java
│   │       │   │   ├── ProductVariant.java
│   │       │   │   ├── ProductImage.java
│   │       │   │   └── ProductSize.java
│   │       │   ├── mapper/
│   │       │   │   ├── ProductMapper.java
│   │       │   │   └── ProductVariantMapper.java
│   │       │   ├── repository/
│   │       │   │   ├── ProductRepository.java
│   │       │   │   ├── ProductVariantRepository.java
│   │       │   │   ├── ProductImageRepository.java
│   │       │   │   └── ProductSizeRepository.java
│   │       │   └── service/
│   │       │       ├── ProductService.java
│   │       │       └── impl/
│   │       │           └── ProductServiceImpl.java
│   │       │
│   │       ├── cart/
│   │       │   ├── controller/
│   │       │   │   └── CartController.java
│   │       │   ├── dto/
│   │       │   │   ├── AddCartItemRequest.java
│   │       │   │   ├── UpdateCartItemRequest.java
│   │       │   │   └── CartResponse.java
│   │       │   ├── entity/
│   │       │   │   ├── Cart.java
│   │       │   │   └── CartItem.java
│   │       │   ├── mapper/
│   │       │   │   └── CartMapper.java
│   │       │   ├── repository/
│   │       │   │   ├── CartRepository.java
│   │       │   │   └── CartItemRepository.java
│   │       │   └── service/
│   │       │       ├── CartService.java
│   │       │       └── impl/
│   │       │           └── CartServiceImpl.java
│   │       │
│   │       ├── order/
│   │       │   ├── controller/
│   │       │   │   └── OrderController.java
│   │       │   ├── dto/
│   │       │   │   ├── CreateOrderRequest.java
│   │       │   │   ├── OrderResponse.java
│   │       │   │   └── OrderItemResponse.java
│   │       │   ├── entity/
│   │       │   │   ├── Order.java
│   │       │   │   ├── OrderItem.java
│   │       │   │   └── OrderAddress.java
│   │       │   ├── mapper/
│   │       │   │   └── OrderMapper.java
│   │       │   ├── repository/
│   │       │   │   ├── OrderRepository.java
│   │       │   │   ├── OrderItemRepository.java
│   │       │   │   └── OrderAddressRepository.java
│   │       │   └── service/
│   │       │       ├── OrderService.java
│   │       │       └── impl/
│   │       │           └── OrderServiceImpl.java
│   │       │
│   │       ├── payment/
│   │       │   ├── controller/
│   │       │   │   └── PaymentController.java
│   │       │   ├── dto/
│   │       │   │   ├── CreatePaymentRequest.java
│   │       │   │   └── PaymentResponse.java
│   │       │   ├── entity/
│   │       │   │   └── Payment.java
│   │       │   ├── repository/
│   │       │   │   └── PaymentRepository.java
│   │       │   └── service/
│   │       │       ├── PaymentService.java
│   │       │       ├── PaymentGateway.java
│   │       │       └── impl/
│   │       │           ├── PaymentServiceImpl.java
│   │       │           └── KhqrPaymentGateway.java
│   │       │
│   │       ├── coupon/
│   │       │   ├── controller/
│   │       │   ├── dto/
│   │       │   ├── entity/
│   │       │   │   └── Coupon.java
│   │       │   ├── mapper/
│   │       │   ├── repository/
│   │       │   └── service/
│   │       │
│   │       ├── review/
│   │       │   ├── controller/
│   │       │   ├── dto/
│   │       │   ├── entity/
│   │       │   │   └── Review.java
│   │       │   ├── mapper/
│   │       │   ├── repository/
│   │       │   └── service/
│   │       │
│   │       └── wishlist/
│   │           ├── controller/
│   │           ├── dto/
│   │           ├── entity/
│   │           │   └── Wishlist.java
│   │           ├── repository/
│   │           └── service/
│   │
│   └── resources/
│       ├── application.yml
│       │
│       └── db/
│           └── migration/
│               ├── V1__create_categories.sql
│               ├── V2__create_products.sql
│               ├── V3__create_users.sql
│               ├── V4__create_cart.sql
│               ├── V5__create_orders.sql
│               ├── V6__create_coupons.sql
│               └── V7__create_payments.sql
│
└── test/
└── java/
└── com/example/ecommerce/