package com.pi.erp.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository
    extends JpaRepository<User, Long>, JpaSpecificationExecutor<User>
{
    @Query("SELECT u FROM User u WHERE lower(trim(u.email)) = lower(trim(:email))")
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);

    @Query(
        value = "SELECT * FROM users WHERE user_id = :id FOR UPDATE",
        nativeQuery = true
    )
    Optional<User> findByIdForUpdate(
        @Param("id") Long id
    );

    @Query("SELECT (count(u) > 0) FROM User u WHERE lower(trim(u.email)) = lower(trim(:email))")
    boolean existsByEmail(
        @Param("email") String email
    );

    @Query("SELECT (count(u) > 0) FROM User u WHERE lower(trim(u.email)) = lower(trim(:email)) AND u.id <> :id")
    boolean existsByEmailAndIdNot(
        @Param("email") String email,
        @Param("id") Long id
    );

    boolean existsByRoleId(
        Long roleId
    );
}
