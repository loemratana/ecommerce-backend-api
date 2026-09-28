package com.example.ecommerce_api.user.entity;


import com.example.ecommerce_api.cart.entity.Cart;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users",
        indexes = {
        @Index(name = "idx_user_email",columnList = "email")
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,unique = true ,length = 100)
    private String email;

    @Column(nullable = false,length = 100)
    private String firstName;

    @Column(nullable = false,length = 100)
    private String lastName;

    @Column(nullable = false)
    private String password;


    @Column(name = "profile_image",nullable = true)
    private String profileImage;

    @Column(name = "phone", length = 30, nullable = true)
    private String phone;

    @OneToOne(mappedBy = "user")
    private UserCredential userCredential;

    //The Parent (One Side)
    // Always use helper methods to keep both sides in sync
    //"When I perform this operation on this entity, should JPA automatically perform the same operation on the related entity?"




    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    List<Address> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "user",cascade = CascadeType.ALL , orphanRemoval = true)
    private List<PasswordResetTokens>  tokens = new ArrayList<>();

    @OneToOne(mappedBy = "user")

    private Cart cart;


    @ManyToMany
    @JoinTable(
            name ="user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "roles_id")

    )
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    private boolean active =true ;


    @CreationTimestamp
    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;



    public void  addAddress(Address address){

        addresses.add(address);
        address.setUser(this);


    }

    public void  removeAddress(Address address){
        addresses.remove(address);
        address.setUser(null);
    }

    public void  updateAddress(Address address){
        addresses.remove(address);
        address.setUser(this);
    }


    public void addRole(Role role){
        roles.add(role);
        role.getUsers().add(this);
    }
    public void removeRole(Role role){
        roles.remove(role);

        role.getUsers().remove(this);
    }




}
