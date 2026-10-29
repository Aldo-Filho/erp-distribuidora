package com.pi.erp.auth.password;

import com.pi.erp.auth.AccessAdministrationService;
import com.pi.erp.auth.AccessQueryService;
import com.pi.erp.auth.CurrentUser;
import com.pi.erp.auth.audit.SecurityAuditService;
import com.pi.erp.auth.mail.AccessEmailService;
import com.pi.erp.auth.mail.AccessMailService;
import com.pi.erp.auth.protection.AuthenticationThrottleService;
import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.user.User;
import com.pi.erp.user.UserRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordRecoveryService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccessQueryService accessQueryService;

    @Autowired
    private AccessAdministrationService accessAdministrationService;

    @Autowired
    private AuthenticationThrottleService authenticationThrottleService;

    @Autowired
    private SecurityAuditService securityAuditService;

    @Autowired
    private AccessMailService accessMailService;

    @Autowired
    private AccessEmailService accessEmailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transactionTemplate;

    public void recovery(String input) {
        String email = accessEmailService.normalize(input);
        String key = AccessToken.hash(email);
        if (
            !authenticationThrottleService.allow("reset-hour:" + key, 3, 3600) ||
            !authenticationThrottleService.allow("reset-minute:" + key, 1, 60)
        ) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            accessAdministrationService.lock();
            User found = userRepository.findByEmailIgnoreCase(email).orElse(null);
            User user = found == null ? null : accessQueryService.findActive(found.getId());
            if (user != null) {
                String token = AccessToken.create();
                jdbc.update(
                    "UPDATE password_reset_tokens SET consumed_at = now() WHERE user_id = ? AND consumed_at IS NULL",
                    user.getId()
                );
                jdbc.update(
                    """
                    INSERT INTO password_reset_tokens(id, user_id, credential_version, token_hash, issued_at, expires_at)
                    VALUES (?, ?, ?, ?, now(), now() + interval '15 minutes')
                    """,
                    UUID.randomUUID(),
                    user.getId(),
                    user.getCredentialVersion(),
                    AccessToken.hash(token)
                );
                accessMailService.link(email, "/reset-password", token, "15 minutes", null);
            }
            securityAuditService.record(
                "PASSWORD_RECOVERY_REQUESTED",
                user == null ? null : user.getId().toString(),
                "ACCEPTED"
            );
        });
    }

    public void administrative(Long id) {
        CurrentUser.require("access.users.manage");
        User user = userRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        recovery(user.getEmail());
    }

    public void reset(RequestPasswordResetDTO data) {
        AccessToken.password(data.newPassword());
        String tokenHash = AccessToken.hash(data.token());
        var tokens = jdbc.queryForList(
            """
            SELECT user_id FROM password_reset_tokens WHERE token_hash = ?
            AND consumed_at IS NULL AND expires_at > now()
            """,
            tokenHash
        );
        if (tokens.isEmpty()) {
            securityAuditService.failure("PASSWORD_RESET_FAILED");
            throw new IllegalArgumentException("Invalid or expired link.");
        }
        String hash = authenticationThrottleService.passwordWork(() ->
            passwordEncoder.encode(data.newPassword())
        );
        transactionTemplate.executeWithoutResult(status -> {
            accessAdministrationService.lock();
            var current = jdbc.queryForList(
                """
                SELECT user_id, credential_version FROM password_reset_tokens WHERE token_hash = ?
                AND consumed_at IS NULL AND expires_at > now()
                """,
                tokenHash
            );
            if (current.isEmpty()) {
                throw new IllegalArgumentException("Invalid or expired link.");
            }
            Long userId = ((Number) current.getFirst().get("user_id")).longValue();
            User user = accessQueryService.findActive(userId);
            if (
                user == null ||
                user.getCredentialVersion() !=
                    ((Number) current.getFirst().get("credential_version")).longValue()
            ) {
                throw new IllegalArgumentException("Invalid or expired link.");
            }
            change(user, hash);
            securityAuditService.recordAsUser("PASSWORD_RESET", userId, "SUCCESS");
        });
    }

    public void update(PatchPasswordDTO data) {
        AccessToken.password(data.newPassword());
        Long id = CurrentUser.requireId();
        User snapshot = userRepository.findById(id).orElseThrow();
        String previousHash = snapshot.getPasswordHash();
        long version = snapshot.getCredentialVersion();
        String key = AccessToken.hash(snapshot.getEmail());
        authenticationThrottleService.checkAccount(key);
        boolean matches = authenticationThrottleService.passwordWork(() ->
            passwordEncoder.matches(data.currentPassword(), previousHash)
        );
        if (!matches) {
            authenticationThrottleService.failed(key);
            securityAuditService.failure("PASSWORD_CHANGE_FAILED");
            throw new IllegalArgumentException("Current password is invalid.");
        }
        String hash = authenticationThrottleService.passwordWork(() ->
            passwordEncoder.encode(data.newPassword())
        );
        transactionTemplate.executeWithoutResult(status -> {
            accessAdministrationService.lock();
            User user = accessQueryService.findActive(id);
            if (
                user == null ||
                version != user.getCredentialVersion() ||
                !Objects.equals(previousHash, user.getPasswordHash())
            ) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }
            change(user, hash);
            securityAuditService.record("PASSWORD_CHANGED", id.toString(), "SUCCESS");
        });
        authenticationThrottleService.succeeded(key);
    }

    private void change(User user, String hash) {
        user.setPasswordHash(hash);
        user.setCredentialVersion(user.getCredentialVersion() + 1);
        userRepository.saveAndFlush(user);
        accessAdministrationService.invalidate(user.getId());
        accessMailService.notice(user.getEmail(), "Your ERP password was changed. Sign in again.");
    }
}
