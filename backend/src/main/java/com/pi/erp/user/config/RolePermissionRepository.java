package com.pi.erp.user.config;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {
    Page<RolePermission> findAllByRoleId(Long roleId, Pageable pageable);

    List<RolePermission> findAllByRoleId(Long roleId);

    boolean existsByRoleId(Long roleId);

    boolean existsByPermissionId(Long permissionId);
}
