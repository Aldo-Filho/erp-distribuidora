package com.pi.erp.auth.invitation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequestUserInvitationDTO(
    @NotBlank String name,
    @NotBlank String email,
    @NotNull Long roleId
) {}
