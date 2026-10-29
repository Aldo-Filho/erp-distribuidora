package com.pi.erp.auth.settings;

public record SecuritySettings(
    int idleMinutes,
    int absoluteMinutes,
    int maxSessions,
    long policyVersion
) {}
