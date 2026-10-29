package com.pi.erp.auth.password;

import jakarta.validation.constraints.NotBlank;

public record RequestPasswordRecoveryDTO(@NotBlank String email) {}
