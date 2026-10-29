package com.pi.erp.user.config;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    List<RolePermission> findAllByRoleId(
        Long roleId
    );

    boolean existsByRoleId(
        Long roleId
    );

    boolean existsByPermissionId(
        Long permissionId
    );
}
