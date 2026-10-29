package com.pi.erp.user.config.permission;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "permission_id")
    private Long id;

    @Column(name = "module", nullable = false, length = 50)
    private String module;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "permission_key", nullable = false, unique = true, length = 100)
    private String permissionKey;

    public Permission(RequestPermissionDTO data) {
        this.module = data.module();
        this.action = data.action();
        this.permissionKey = data.permissionKey();
    }
}
