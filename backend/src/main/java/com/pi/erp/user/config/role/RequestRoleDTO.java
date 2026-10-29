package com.pi.erp.user.config.role;

import jakarta.validation.constraints.NotBlank;

public record RequestRoleDTO(
    @NotBlank String name,
    String description,
    Boolean active
) {}
