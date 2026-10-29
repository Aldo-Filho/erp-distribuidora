package com.pi.erp.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequestLoginDTO(@NotBlank String email, @NotNull String password) {}
