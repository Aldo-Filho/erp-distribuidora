package com.pi.erp.auth.password;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequestPasswordResetDTO(@NotBlank String token, @NotNull String newPassword) {}
