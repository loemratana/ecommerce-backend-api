package com.example.ecommerce_api.user.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_credentials"
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String alias;

    //owning side  cause it has pk
    @OneToOne(cascade = CascadeType.ALL)//points to the field that owns the relationship
    @JoinColumn(name = "user_id" , referencedColumnName = "id",nullable = false,unique = true)
    private User user;


    @Column(name = "password_change_at")
    private LocalDateTime passwordChangeAt;


    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable =false )
    private LocalDateTime updatedAt;
}
