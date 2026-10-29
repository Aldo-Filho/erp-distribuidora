package com.pi.erp.auth.session;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.UUID;

public record UserSession(
    UUID id,
    @JsonIgnore String sessionId,
    @JsonIgnore Long userId,
    Instant authenticatedAt,
    Instant lastActivityAt,
    @JsonIgnore long credentialVersion,
    String browser,
    @JsonIgnore Instant revokedAt
) {}
