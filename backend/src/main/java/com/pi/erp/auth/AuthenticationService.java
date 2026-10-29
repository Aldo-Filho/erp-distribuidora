package com.pi.erp.auth;

import com.pi.erp.auth.audit.SecurityAuditService;
import com.pi.erp.auth.password.AccessToken;
import com.pi.erp.auth.protection.AuthenticationThrottleService;
import com.pi.erp.auth.session.LoginChallengeView;
import com.pi.erp.auth.session.RequestSessionReplacementDTO;
import com.pi.erp.auth.session.UserSession;
import com.pi.erp.auth.session.UserSessionRepository;
import com.pi.erp.auth.session.UserSessionService;
import com.pi.erp.auth.settings.SecuritySettingsRepository;
import com.pi.erp.user.User;
import com.pi.erp.user.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthenticationService {

    private static final Pattern ARGON2_HASH = Pattern.compile(
        "\\$argon2(?:id|i|d)\\$v=\\d+\\$m=\\d+,t=\\d+,p=\\d+\\$[A-Za-z0-9+/]+={0,2}\\$[A-Za-z0-9+/]+={0,2}"
    );

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccessQueryService accessQueryService;

    @Autowired
    private AccessAdministrationService accessAdministrationService;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private SecuritySettingsRepository securitySettingsRepository;

    @Autowired
    private SecurityAuditService securityAuditService;

    @Autowired
    private AuthenticationThrottleService authenticationThrottleService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private HttpSessionCsrfTokenRepository csrfTokenRepository;

    private String dummyHash;

    @PostConstruct
    public void initialize() {
        dummyHash = passwordEncoder.encode(AccessToken.create());
    }

    public Object login(
        RequestLoginDTO data,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        String email = AccessToken.email(data.email());
        if (email.length() > 150 || data.password().length() > 1024) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String account = AccessToken.hash(email);
        authenticationThrottleService.checkAccount(account);
        User snapshot = userRepository.findByEmailIgnoreCase(email).orElse(null);
        String storedHash = snapshot == null ? null : snapshot.getPasswordHash();
        boolean usableHash = storedHash != null && ARGON2_HASH.matcher(storedHash).matches();
        String hash = usableHash ? storedHash : dummyHash;
        boolean matches = authenticationThrottleService.passwordWork(() -> {
            try {
                return passwordEncoder.matches(data.password(), hash);
            } catch (IllegalArgumentException exception) {
                passwordEncoder.matches(data.password(), dummyHash);
                return false;
            }
        });
        if (
            !matches ||
            !usableHash ||
            snapshot == null ||
            !Boolean.TRUE.equals(snapshot.getActive()) ||
            !Boolean.TRUE.equals(snapshot.getRole().getActive())
        ) {
            authenticationThrottleService.failed(account);
            securityAuditService.failure("LOGIN_FAILED");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Object result = transactionTemplate.execute(status -> {
            accessAdministrationService.lock();
            requireCurrentContext(request);
            User user = accessQueryService.findActive(snapshot.getId());
            if (
                user == null ||
                user.getCredentialVersion() != snapshot.getCredentialVersion() ||
                !Objects.equals(user.getPasswordHash(), storedHash)
            ) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }
            Long existing = CurrentUser.idOrNull();
            if (existing != null) {
                if (!existing.equals(user.getId())) {
                    throw new IllegalArgumentException(
                        "Log out before signing in to another account."
                    );
                }
                return userSessionService.view(request);
            }
            List<UserSession> sessions = userSessionService.sessions(user.getId());
            if (sessions.size() >= securitySettingsRepository.findOne().maxSessions()) {
                return challenge(user, request, sessions);
            }
            return admit(user, request, response);
        });
        authenticationThrottleService.succeeded(account);
        return result;
    }

    public Object replace(
        RequestSessionReplacementDTO data,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        if (CurrentUser.idOrNull() != null || request.getSession(false) == null) {
            throw new IllegalArgumentException("Invalid or expired challenge.");
        }
        return transactionTemplate.execute(status -> {
            accessAdministrationService.lock();
            requireCurrentContext(request);
            var challenges = jdbc.queryForList(
                """
                SELECT * FROM login_challenges WHERE token_hash = ? AND context_hash = ?
                AND consumed_at IS NULL AND expires_at > now()
                """,
                AccessToken.hash(data.challengeToken()),
                AccessToken.hash(request.getSession(false).getId())
            );
            if (challenges.isEmpty()) {
                throw new IllegalArgumentException("Invalid or expired challenge.");
            }
            var challenge = challenges.getFirst();
            Long userId = ((Number) challenge.get("user_id")).longValue();
            User user = accessQueryService.findActive(userId);
            if (
                user == null ||
                user.getCredentialVersion() !=
                    ((Number) challenge.get("credential_version")).longValue()
            ) {
                throw new IllegalArgumentException("Invalid or expired challenge.");
            }
            List<UserSession> sessions = userSessionService.sessions(userId);
            int maximum = securitySettingsRepository.findOne().maxSessions();
            if (sessions.size() >= maximum) {
                UserSession selected = sessions
                    .stream()
                    .filter(session -> session.id().equals(data.sessionId()))
                    .findFirst()
                    .orElse(null);
                if (selected == null) {
                    jdbc.update(
                        "UPDATE login_challenges SET consumed_at = now() WHERE id = ?",
                        challenge.get("id")
                    );
                    return challenge(user, request, sessions);
                }
                userSessionRepository.revoke(selected.id());
            }
            jdbc.update(
                "UPDATE login_challenges SET consumed_at = now() WHERE id = ?",
                challenge.get("id")
            );
            return admit(user, request, response);
        });
    }

    private LoginChallengeView challenge(
        User user,
        HttpServletRequest request,
        List<UserSession> sessions
    ) {
        String token = AccessToken.create();
        String context = AccessToken.hash(request.getSession(true).getId());
        jdbc.update(
            "UPDATE login_challenges SET consumed_at = now() WHERE context_hash = ? AND consumed_at IS NULL",
            context
        );
        jdbc.update(
            """
            INSERT INTO login_challenges(id, user_id, credential_version, token_hash, context_hash, expires_at)
            VALUES (?, ?, ?, ?, ?, now() + interval '2 minutes')
            """,
            UUID.randomUUID(),
            user.getId(),
            user.getCredentialVersion(),
            AccessToken.hash(token),
            context
        );
        securityAuditService.recordAsUser("SESSION_LIMIT_REACHED", user.getId(), "CONFLICT");
        return new LoginChallengeView("SESSION_LIMIT_REACHED", token, sessions);
    }

    private void requireCurrentContext(HttpServletRequest request) {
        // Another request may have rotated this same anonymous context while Argon2 ran.
        // Never write that stale snapshot back over the newly authenticated session.
        if (
            request.getSession(false) == null ||
            !Boolean.TRUE.equals(
                jdbc.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM spring_session WHERE session_id = ?)",
                    Boolean.class,
                    request.getSession(false).getId()
                )
            )
        ) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
    }

    private SessionView admit(User user, HttpServletRequest request, HttpServletResponse response) {
        // A new physical row prevents stale framework writes from restoring the anonymous ID.
        // In-place changeSessionId would retain PRIMARY_ID, which JDBC updates by stale snapshots.
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        request.getSession(true);
        request.getSession().setMaxInactiveInterval(86400);
        csrfTokenRepository.saveToken(null, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        // Persist only identity/version. Current permissions are rebuilt by the policy filter.
        context.setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                new ErpPrincipal(user.getId(), user.getCredentialVersion()),
                null,
                List.of()
            )
        );
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
        String browser = browser(request.getHeader("User-Agent"));
        userSessionRepository.register(
            UUID.randomUUID(),
            request.getSession().getId(),
            user.getId(),
            user.getCredentialVersion(),
            browser
        );
        user.setLastLoginAt(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
        userRepository.saveAndFlush(user);
        securityAuditService.record("LOGIN", user.getId().toString(), "SUCCESS");
        return userSessionService.view(request);
    }

    private String browser(String agent) {
        if (agent == null) {
            return "Browser";
        }
        String name = agent.contains("Edg/")
            ? "Edge"
            : agent.contains("Firefox/")
              ? "Firefox"
              : agent.contains("Chrome/")
                ? "Chrome"
                : agent.contains("Safari/")
                  ? "Safari"
                  : "Browser";
        String platform = agent.contains("Windows")
            ? "Windows"
            : agent.contains("Android")
              ? "Android"
              : agent.contains("iPhone") || agent.contains("iPad")
                ? "iOS"
                : agent.contains("Mac")
                  ? "macOS"
                  : "Other";
        return name + " / " + platform;
    }
}
