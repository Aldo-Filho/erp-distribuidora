package com.pi.erp.auth.invitation;

import com.pi.erp.auth.AccessAdministrationService;
import com.pi.erp.auth.CurrentUser;
import com.pi.erp.auth.audit.SecurityAuditService;
import com.pi.erp.auth.mail.AccessEmailService;
import com.pi.erp.auth.mail.AccessMailService;
import com.pi.erp.auth.password.AccessToken;
import com.pi.erp.auth.protection.AuthenticationThrottleService;
import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.user.RequestUserDTO;
import com.pi.erp.user.User;
import com.pi.erp.user.UserRepository;
import com.pi.erp.user.config.role.Role;
import com.pi.erp.user.config.role.RoleRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class UserInvitationService {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AccessAdministrationService accessAdministrationService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationThrottleService authenticationThrottleService;

    @Autowired
    private AccessMailService accessMailService;

    @Autowired
    private AccessEmailService accessEmailService;

    @Autowired
    private SecurityAuditService securityAuditService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    public Page<UserInvitation> search(String email, Pageable pageable) {
        String query =
            email == null || email.isBlank()
                ? "%"
                : "%" + email.trim().toLowerCase(Locale.ROOT) + "%";
        var invitations = jdbc.query(
            """
            SELECT * FROM user_invitations WHERE lower(email) LIKE ?
            ORDER BY issued_at DESC, id LIMIT ? OFFSET ?
            """,
            this::map,
            query,
            pageable.getPageSize(),
            pageable.getOffset()
        );
        Long total = jdbc.queryForObject(
            "SELECT count(*) FROM user_invitations WHERE lower(email) LIKE ?",
            Long.class,
            query
        );
        return new PageImpl<>(invitations, pageable, total);
    }

    @Transactional
    public UserInvitation register(RequestUserInvitationDTO data) {
        String email = accessEmailService.normalize(data.email());
        throttle(email);
        accessAdministrationService.lock();
        activeRole(data.roleId());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User email already exists.");
        }
        jdbc.update(
            """
            UPDATE user_invitations SET canceled_at = now() WHERE lower(email) = ?
            AND expires_at <= now() AND consumed_at IS NULL AND canceled_at IS NULL
            """,
            email
        );
        String token = AccessToken.create();
        UUID id = UUID.randomUUID();
        jdbc.update(
            """
            INSERT INTO user_invitations(id, name, email, role_id, created_by, token_hash, issued_at, expires_at)
            VALUES (?, ?, ?, ?, ?, ?, now(), now() + interval '24 hours')
            """,
            id,
            data.name().trim(),
            email,
            data.roleId(),
            CurrentUser.requireId(),
            AccessToken.hash(token)
        );
        accessMailService.link(email, "/accept-invitation", token, "24 hours", id);
        securityAuditService.record("INVITATION_CREATED", id.toString(), "SUCCESS");
        return findOne(id);
    }

    @Transactional
    public void resend(UUID id) {
        accessAdministrationService.lock();
        UserInvitation invitation = findOne(id);
        throttle(invitation.email());
        if (invitation.consumedAt() != null || invitation.canceledAt() != null) {
            throw new IllegalArgumentException("Invitation is no longer available.");
        }
        activeRole(invitation.roleId());
        if (userRepository.existsByEmail(invitation.email())) {
            throw new IllegalArgumentException("User email already exists.");
        }
        String token = AccessToken.create();
        jdbc.update(
            """
            UPDATE user_invitations SET token_hash = ?, issued_at = now(), expires_at = now() + interval '24 hours',
            delivery_status = 'PENDING' WHERE id = ?
            """,
            AccessToken.hash(token),
            id
        );
        accessMailService.link(invitation.email(), "/accept-invitation", token, "24 hours", id);
        securityAuditService.record("INVITATION_RESENT", id.toString(), "SUCCESS");
    }

    @Transactional
    public void delete(UUID id) {
        accessAdministrationService.lock();
        findOne(id);
        jdbc.update(
            "UPDATE user_invitations SET canceled_at = now() WHERE id = ? AND consumed_at IS NULL",
            id
        );
        securityAuditService.record("INVITATION_CANCELED", id.toString(), "SUCCESS");
    }

    public void accept(RequestInvitationAcceptanceDTO data) {
        AccessToken.password(data.password());
        String tokenHash = AccessToken.hash(data.token());
        boolean exists = Boolean.TRUE.equals(
            jdbc.queryForObject(
                """
                SELECT EXISTS(SELECT 1 FROM user_invitations WHERE token_hash = ?
                AND consumed_at IS NULL AND canceled_at IS NULL AND expires_at > now())
                """,
                Boolean.class,
                tokenHash
            )
        );
        if (!exists) {
            securityAuditService.failure("INVITATION_ACCEPTANCE_FAILED");
            throw new IllegalArgumentException("Invalid or expired link.");
        }
        String hash = authenticationThrottleService.passwordWork(() ->
            passwordEncoder.encode(data.password())
        );
        transactionTemplate.executeWithoutResult(status -> {
            accessAdministrationService.lock();
            var invitations = jdbc.query(
                """
                SELECT * FROM user_invitations WHERE token_hash = ? AND consumed_at IS NULL
                AND canceled_at IS NULL AND expires_at > now()
                """,
                this::map,
                tokenHash
            );
            if (invitations.isEmpty()) {
                throw new IllegalArgumentException("Invalid or expired link.");
            }
            UserInvitation invitation = invitations.getFirst();
            Role role = activeRole(invitation.roleId());
            if (userRepository.existsByEmail(invitation.email())) {
                throw new IllegalArgumentException("Invalid or expired link.");
            }
            User user = new User(
                new RequestUserDTO(
                    invitation.name(),
                    invitation.email(),
                    null,
                    invitation.roleId(),
                    true
                ),
                role,
                hash
            );
            userRepository.saveAndFlush(user);
            jdbc.update(
                "UPDATE user_invitations SET consumed_at = now() WHERE id = ?",
                invitation.id()
            );
            securityAuditService.recordAsUser("INVITATION_ACCEPTED", user.getId(), "SUCCESS");
        });
    }

    private Role activeRole(Long id) {
        Role role = roleRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found."));
        if (!Boolean.TRUE.equals(role.getActive())) {
            throw new IllegalArgumentException("Role must be active.");
        }
        return role;
    }

    private UserInvitation findOne(UUID id) {
        return jdbc
            .query("SELECT * FROM user_invitations WHERE id = ?", this::map, id)
            .stream()
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Invitation not found."));
    }

    private void throttle(String email) {
        String account = AccessToken.hash(email);
        if (
            !authenticationThrottleService.allow(
                "invite-actor:" + CurrentUser.requireId(),
                30,
                3600
            ) ||
            !authenticationThrottleService.allow("invite-email:" + account, 1, 60)
        ) {
            throw authenticationThrottleService.limited();
        }
    }

    private UserInvitation map(ResultSet row, int index) throws SQLException {
        return new UserInvitation(
            row.getObject("id", UUID.class),
            row.getString("name"),
            row.getString("email"),
            row.getLong("role_id"),
            row.getTimestamp("issued_at").toInstant(),
            row.getTimestamp("expires_at").toInstant(),
            row.getTimestamp("consumed_at") == null
                ? null
                : row.getTimestamp("consumed_at").toInstant(),
            row.getTimestamp("canceled_at") == null
                ? null
                : row.getTimestamp("canceled_at").toInstant(),
            row.getString("delivery_status")
        );
    }
}
