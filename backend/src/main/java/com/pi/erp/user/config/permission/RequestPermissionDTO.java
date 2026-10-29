package com.pi.erp.user.config.permission;

import jakarta.validation.constraints.NotBlank;

public record RequestPermissionDTO(
    @NotBlank String module,
    @NotBlank String action,
    @NotBlank String permissionKey
) {}
