package com.example.ecommerce_api.permission.repository;

import com.example.ecommerce_api.permission.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Permission> findByNameIgnoreCase(String name);

    List<Permission> findAllByIdIn(Set<Long> ids);

    @Query("""
            select case when count(r) > 0 then true else false end
            from Role r join r.permissions p where p.id = :id
            """)
    boolean existsRoleWithPermission(@Param("id") Long id);
}
