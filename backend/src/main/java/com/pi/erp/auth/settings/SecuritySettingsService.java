package com.pi.erp.auth.settings;

import com.pi.erp.auth.AccessAdministrationService;
import com.pi.erp.auth.AccessQueryService;
import com.pi.erp.auth.CurrentUser;
import com.pi.erp.auth.audit.SecurityAuditService;
import com.pi.erp.auth.session.UserSession;
import com.pi.erp.auth.session.UserSessionRepository;
import com.pi.erp.auth.session.UserSessionService;
import com.pi.erp.user.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecuritySettingsService {

    @Autowired
    private SecuritySettingsRepository repository;

    @Autowired
    private AccessAdministrationService accessAdministrationService;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SecurityAuditService securityAuditService;

    @Autowired
    private AccessQueryService accessQueryService;

    @Transactional
    public SecuritySettings update(PatchSecuritySettingsDTO data, HttpServletRequest request) {
        accessAdministrationService.lock();
        SecuritySettings policy = repository.findOne();
        int idle = data.idleMinutes() == null ? policy.idleMinutes() : data.idleMinutes();
        int absolute =
            data.absoluteMinutes() == null ? policy.absoluteMinutes() : data.absoluteMinutes();
        int maximum = data.maxSessions() == null ? policy.maxSessions() : data.maxSessions();
        if (
            idle < 15 ||
            idle > 240 ||
            absolute < 60 ||
            absolute > 1440 ||
            maximum < 1 ||
            maximum > 5 ||
            idle > absolute
        ) {
            throw new IllegalArgumentException("Session policy is outside the allowed range.");
        }
        List<Long> userIds = jdbc.queryForList(
            "SELECT user_id FROM users ORDER BY user_id",
            Long.class
        );
        // Expiration under the previous policy is final, even if the new limits are longer.
        for (Long userId : userIds) {
            User user = accessQueryService.findActive(userId);
            userSessionRepository
                .findByUserId(userId)
                .stream()
                .filter(session -> !userSessionService.valid(session, user, policy))
                .forEach(session -> userSessionRepository.revoke(session.id()));
        }
        SecuritySettings savedPolicy = repository.update(
            idle,
            absolute,
            maximum,
            CurrentUser.requireId()
        );
        String current = request.getSession().getId();
        for (Long userId : userIds) {
            var sessions = new ArrayList<>(userSessionRepository.findByUserId(userId));
            var valid = userSessionService.sessions(userId);
            sessions
                .stream()
                .filter(session ->
                    valid.stream().noneMatch(active -> active.id().equals(session.id()))
                )
                .forEach(session -> userSessionRepository.revoke(session.id()));
            var retained = new ArrayList<>(valid);
            retained.sort(
                Comparator.<UserSession, Boolean>comparing(
                    session -> !session.sessionId().equals(current)
                )
                    .thenComparing(UserSession::lastActivityAt, Comparator.reverseOrder())
                    .thenComparing(UserSession::id)
            );
            for (int index = maximum; index < retained.size(); index++) {
                userSessionRepository.revoke(retained.get(index).id());
            }
        }
        securityAuditService.policy(idle, absolute, maximum);
        return savedPolicy;
    }
}
