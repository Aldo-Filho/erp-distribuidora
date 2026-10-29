package com.pi.erp.user.config.permission;

public record PermissionFilter(
    String module,
    String action,
    String permissionKey
) {}
