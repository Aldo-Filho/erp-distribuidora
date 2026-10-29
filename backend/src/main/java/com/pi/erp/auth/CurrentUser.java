package com.pi.erp.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

public final class CurrentUser {

    private CurrentUser() {}

    public static Long idOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (
            authentication != null &&
            authentication.getPrincipal() instanceof ErpPrincipal principal
        ) {
            return principal.userId();
        }
        return null;
    }

    public static Long requireId() {
        Long id = idOrNull();
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return id;
    }

    public static boolean has(String key) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (
            authentication != null &&
            authentication
                .getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals(key))
        );
    }

    public static void require(String key) {
        requireId();
        if (!has(key)) {
            throw new AccessDeniedException("Permission denied.");
        }
    }

    public static Long operator(Long legacyId) {
        Long id = requireId();
        if (legacyId != null && !legacyId.equals(id)) {
            throw new IllegalArgumentException("Operator must match the authenticated user.");
        }
        return id;
    }
}
