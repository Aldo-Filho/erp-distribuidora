package com.pi.erp.user;

public record PatchUserDTO(
    String name,
    String email,
    String password,
    Long roleId,
    Boolean active
) {}
