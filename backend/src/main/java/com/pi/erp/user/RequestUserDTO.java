package com.pi.erp.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequestUserDTO(
    @NotBlank String name,
    @NotBlank @Email String email,
    @NotBlank String password,
    @NotNull Long roleId,
    Boolean active
) {}
