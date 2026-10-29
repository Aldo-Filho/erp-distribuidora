package com.pi.erp.auth.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RequestSessionReplacementDTO(
    @NotBlank String challengeToken,
    @NotNull UUID sessionId
) {}
