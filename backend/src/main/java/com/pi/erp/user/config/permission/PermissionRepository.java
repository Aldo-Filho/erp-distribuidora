package com.pi.erp.user.config.permission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PermissionRepository
    extends JpaRepository<Permission, Long>, JpaSpecificationExecutor<Permission>
{
    boolean existsByPermissionKey(
        String permissionKey
    );

    boolean existsByPermissionKeyAndIdNot(
        String permissionKey,
        Long id
    );
}
