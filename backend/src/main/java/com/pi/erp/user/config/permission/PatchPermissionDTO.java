package com.pi.erp.user.config.permission;

public record PatchPermissionDTO(
    String module,
    String action,
    String permissionKey
) {}
