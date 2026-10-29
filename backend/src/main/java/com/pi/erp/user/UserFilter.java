package com.pi.erp.user;

public record UserFilter(
    String name,
    String email,
    Long roleId,
    Boolean active
) {}
