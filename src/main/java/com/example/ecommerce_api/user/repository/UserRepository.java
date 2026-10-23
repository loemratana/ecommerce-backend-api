package com.example.ecommerce_api.user.repository;

import com.example.ecommerce_api.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
        SELECT DISTINCT u
        FROM User u
        LEFT JOIN FETCH u.roles
        WHERE LOWER(u.email) = LOWER(:email)
    """)
    Optional<User> findByEmailIgnoreCase(String email);

    @Query("""
        SELECT DISTINCT u
        FROM User u
        LEFT JOIN FETCH u.roles
        WHERE LOWER(u.email) = LOWER(:email)
    """)
    Optional<User> findByEmailIgnoreCaseWithRoles(@Param("email") String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            SELECT COUNT (u) >0  FROM  User u JOIN u.roles r where r.id=:roleId
            """)
    boolean existsByRoleId(@Param("roleId") Long roleId);

    @Query(
            """

select count (u)>0 from User u where LOWER(u.email) = LOWER(:email) and u.id <> :userId
"""
    )
    boolean  existsByEmailIgnoreCaseAndIdNot(@Param("email") String email,@Param("userId") Long userId);
    Optional<User> findByEmail(String email);

    // ----- customers (users holding the CUSTOMER role) -----

    @Query(
            value = """
        SELECT DISTINCT u
        FROM User u
        JOIN u.roles r
        WHERE UPPER(r.name) = 'CUSTOMER'
          AND (:active IS NULL OR u.active = :active)
          AND (
              LOWER(u.email) LIKE :search
              OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE :search
              OR LOWER(COALESCE(u.phone, '')) LIKE :search
          )
        """,
            countQuery = """
        SELECT COUNT(DISTINCT u)
        FROM User u
        JOIN u.roles r
        WHERE UPPER(r.name) = 'CUSTOMER'
          AND (:active IS NULL OR u.active = :active)
          AND (
              LOWER(u.email) LIKE :search
              OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE :search
              OR LOWER(COALESCE(u.phone, '')) LIKE :search
          )
        """
    )
    Page<User> searchCustomers(
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE UPPER(r.name) = 'CUSTOMER'")
    long countCustomers();

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE UPPER(r.name) = 'CUSTOMER' AND u.active = :active")
    long countCustomersByActive(@Param("active") boolean active);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE UPPER(r.name) = 'CUSTOMER' AND u.createdAt >= :since")
    long countCustomersCreatedSince(@Param("since") LocalDateTime since);
}
