package com.pi.erp.user.config;

import com.pi.erp.user.config.permission.Permission;
import com.pi.erp.user.config.role.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_permissions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RolePermission {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private RolePermissionId id = new RolePermissionId();

    @MapsId("roleId")
    @ManyToOne(optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @MapsId("permissionId")
    @ManyToOne(optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    public RolePermission(Role role, Permission permission) {
        this.id = new RolePermissionId(role.getId(), permission.getId());
        this.role = role;
        this.permission = permission;
    }
}
