package com.pi.erp.auth.invitation;

import java.time.Instant;
import java.util.UUID;

public record UserInvitation(
    UUID id,
    String name,
    String email,
    Long roleId,
    Instant issuedAt,
    Instant expiresAt,
    Instant consumedAt,
    Instant canceledAt,
    String deliveryStatus
) {}
