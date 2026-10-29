package com.pi.erp.user.config;

import jakarta.validation.constraints.NotNull;

public record RequestRolePermissionDTO(
    @NotNull Long permissionId
) {}
