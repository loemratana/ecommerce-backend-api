package com.example.ecommerce_api.cart.entity;

import com.example.ecommerce_api.product.entity.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "cart_items",
        uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_cart_product",
                columnNames = {
                        "cart_id",
                        "product_id"
                }
        )
        },
        indexes = {
                @Index(
                        name = "idx_cart_item_cart_id",
                        columnList = "cart_id"
                ),
                @Index(
                        name = "idx_cart_item_product_id",
                        columnList = "product_id"
                )
        }
)
@AllArgsConstructor
@NoArgsConstructor
public class CartItem {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer quantity;

    /**
     * Price captured when the product is added
     * to the cart.
     *
     * Example:
     *
     * Product price = $100
     * User adds product
     * CartItem price = $100
     *
     * Later product price becomes $120.
     *
     * CartItem price remains $100 until
     * your application explicitly updates it.
     */


    @Column(nullable = false,scale = 2,precision = 19)
    @DecimalMin(value = "0.0",inclusive = true,message = "Price must be greater than 0")
    private BigDecimal price;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;



    @CreationTimestamp
    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;


    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public BigDecimal getSubtotal()
    {
        if (price == null || quantity == null) return BigDecimal.ZERO;

        return price.multiply(BigDecimal.valueOf(quantity));
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CartItem cartItem)) return false;
        return id != null && id.equals(cartItem.id);
    }




    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "CartItem{id=\" + id + \", quantity=\" + quantity + \"}";
    }
}
