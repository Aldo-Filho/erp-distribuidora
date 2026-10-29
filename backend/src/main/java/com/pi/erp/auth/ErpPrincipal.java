package com.pi.erp.auth;

import java.io.Serializable;
import java.security.Principal;

public record ErpPrincipal(Long userId, long credentialVersion) implements Principal, Serializable {
    @Override
    public String getName() {
        return userId.toString();
    }
}
