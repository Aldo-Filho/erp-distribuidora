package com.pi.erp.user.config;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import lombok.*;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class RolePermissionId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "permission_id", nullable = false)
    private Long permissionId;
}
