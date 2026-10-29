package com.pi.erp.auth.invitation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequestInvitationAcceptanceDTO(@NotBlank String token, @NotNull String password) {}
