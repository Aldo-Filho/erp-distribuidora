package com.pi.erp.auth.password;

import jakarta.validation.constraints.NotNull;

public record PatchPasswordDTO(@NotNull String currentPassword, @NotNull String newPassword) {}
