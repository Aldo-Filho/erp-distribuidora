package com.pi.erp.auth;

import com.pi.erp.auth.settings.SecuritySettings;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionView(
    Long userId,
    String name,
    String email,
    Long roleId,
    String roleName,
    List<String> permissionKeys,
    UUID publicSessionId,
    Instant serverTime,
    Instant lastActivityAt,
    Instant idleExpiresAt,
    Instant absoluteExpiresAt,
    SecuritySettings policy
) {}
