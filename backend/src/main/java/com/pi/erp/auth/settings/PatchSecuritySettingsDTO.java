package com.pi.erp.auth.settings;

public record PatchSecuritySettingsDTO(
    Integer idleMinutes,
    Integer absoluteMinutes,
    Integer maxSessions
) {}
