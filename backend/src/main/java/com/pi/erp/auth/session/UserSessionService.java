package com.pi.erp.auth.session;

import com.pi.erp.auth.AccessAdministrationService;
import com.pi.erp.auth.AccessQueryService;
import com.pi.erp.auth.CurrentUser;
import com.pi.erp.auth.SessionView;
import com.pi.erp.auth.settings.SecuritySettings;
import com.pi.erp.auth.settings.SecuritySettingsRepository;
import com.pi.erp.user.User;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserSessionService {

    @Autowired
    private UserSessionRepository repository;

    @Autowired
    private SecuritySettingsRepository securitySettingsRepository;

    @Autowired
    private AccessQueryService accessQueryService;

    @Autowired
    private AccessAdministrationService accessAdministrationService;

    public boolean valid(UserSession session, User user, SecuritySettings policy) {
        Instant now = Instant.now();
        return (
            session != null &&
            user != null &&
            session.revokedAt() == null &&
            session.userId().equals(user.getId()) &&
            session.credentialVersion() == user.getCredentialVersion() &&
            session.lastActivityAt().plus(policy.idleMinutes(), ChronoUnit.MINUTES).isAfter(now) &&
            session
                .authenticatedAt()
                .plus(policy.absoluteMinutes(), ChronoUnit.MINUTES)
                .isAfter(now)
        );
    }

    public List<UserSession> sessions(Long userId) {
        User user = accessQueryService.findActive(userId);
        SecuritySettings policy = securitySettingsRepository.findOne();
        return repository
            .findByUserId(userId)
            .stream()
            .filter(session -> valid(session, user, policy))
            .toList();
    }

    public SessionView view(HttpServletRequest request) {
        if (request.getSession(false) == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        User user = accessQueryService.findActive(CurrentUser.requireId());
        UserSession session = repository.findBySessionId(request.getSession(false).getId());
        SecuritySettings policy = securitySettingsRepository.findOne();
        if (!valid(session, user, policy)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return new SessionView(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole().getId(),
            user.getRole().getName(),
            accessQueryService.permissions(user.getRole().getId()),
            session.id(),
            Instant.now(),
            session.lastActivityAt(),
            session.lastActivityAt().plus(policy.idleMinutes(), ChronoUnit.MINUTES),
            session.authenticatedAt().plus(policy.absoluteMinutes(), ChronoUnit.MINUTES),
            policy
        );
    }

    @Transactional
    public void activity(HttpServletRequest request) {
        accessAdministrationService.lock();
        SessionView view = view(request);
        repository.activity(view.publicSessionId());
    }

    @Transactional
    public void revoke(Long userId, UUID id) {
        accessAdministrationService.lock();
        repository
            .findByUserId(userId)
            .stream()
            .filter(session -> session.id().equals(id))
            .findFirst()
            .ifPresent(session -> repository.revoke(session.id()));
        accessAdministrationService.audit("SESSION_REVOKED", userId);
    }

    @Transactional
    public void logout(HttpServletRequest request) {
        accessAdministrationService.lock();
        if (request.getSession(false) != null) {
            UserSession session = repository.findBySessionId(request.getSession(false).getId());
            if (session != null) {
                repository.revoke(session.id());
                accessAdministrationService.audit("LOGOUT", session.userId());
            }
        }
    }
}
