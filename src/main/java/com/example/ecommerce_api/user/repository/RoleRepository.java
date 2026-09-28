package com.example.ecommerce_api.user.repository;


import com.example.ecommerce_api.user.entity.Role;
import org.hibernate.annotations.Parameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    boolean existsByNameIgnoreCase(String name);

    Optional<Role> findByNameIgnoreCase(String name);

    Optional<Role> findByNameAndActiveTrue(String name);


    @Query(
            """
                    
                    select case when  count (u) >0 then true  else  false  end 
                    from  User u join u.roles r where r.id =:id
                    """
    )
    boolean existsUserWithRole(@Param("id") Long id);


    List<Role> findByActiveTrue();

    List<Role> findByActiveFalse();


//    @Query(value = """
//            SELECT  * FROM Role  r WHERE  r.name =:name and active = true
//            """,nativeQuery = true)
//    List<Role> findByNameAndActive (@Param("name") String name);
//
//    @Query("""
//select r  from  Role r where r.name =?1
//""")
//    Optional<Role> findByName(String name);
//
//
//
//    @Query("""
//
//select r from  Role r where  r.name =:name and r.active =:active
//""")
//    Optional<Role> findAllByNameAndActive(@Param("name") String name, @Param("active") boolean active);
//
//
//    @Query("""
//
//select r from  Role r where  r.name =?1 and r.active =?2
//""")
//    Optional<Role> findAllByNameAndActive1(String name, boolean active);


}
