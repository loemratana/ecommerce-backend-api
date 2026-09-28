package com.example.ecommerce_api.cart.entity;


import com.example.ecommerce_api.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "carts",
        uniqueConstraints = {@UniqueConstraint(name = "uk_cart_user",
                columnNames = "user_id"

        )},
        indexes = {
        @Index(name = "idx_cart_user_id" ,columnList = "user_id")
        }
)
@AllArgsConstructor
@NoArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_cart_price")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total cart price must not be negative")
    private BigDecimal totalCartPrice;

    @Column(name = "total_price_after_discount" , nullable = false, precision = 19,scale = 2)
    @DecimalMin(value = "0.0", inclusive = true, message = "Total price after discount must not be negative")
    private BigDecimal totalPriceAfterDiscount = BigDecimal.ZERO;

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    public void removeItem(CartItem item) {
       for (CartItem cartItem :items)
       {
           item.setCart(null);

       }
       items.clear();
       recalculateTotalPrice();
    }
    public void removeItem()
    {
        for (CartItem item :items)
        {
            removeItem(item);
        }
    }

    public void recalculateTotalPrice()
    {
        this.totalCartPrice = items.stream().map(
                item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
        ).reduce(BigDecimal.ZERO, BigDecimal::add);
        // If there is currently no discount logic,
        // final price = original price.

        this.totalPriceAfterDiscount = this.totalCartPrice;
    }

    public BigDecimal getTotalQuantity ()
    {
        return BigDecimal.valueOf(
                items.stream().mapToInt(CartItem::getQuantity).sum()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cart cart)) return false;
        return id != null && id.equals(cart.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
