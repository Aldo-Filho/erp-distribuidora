package com.pi.erp.auth.audit;

import com.pi.erp.auth.CurrentUser;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecurityAuditService {

    @Autowired
    private JdbcTemplate jdbc;

    // Only fixed action names, identifiers and allowlisted changes; never request bodies.
    public void record(String action, String target, String outcome) {
        record(action, target, outcome, null);
    }

    public void record(String action, String target, String outcome, String targetName) {
        boolean system =
            action.equals("ACCESS_EMAIL") || action.equals("ADMINISTRATOR_BOOTSTRAPPED");
        Long actorId = system ? null : CurrentUser.idOrNull();
        write(
            action,
            target,
            outcome,
            targetName,
            actorId,
            system ? "SYSTEM" : actorType(actorId),
            null
        );
    }

    // Use only after proving identity, such as accepting a valid invitation or reset token.
    public void recordAsUser(String action, Long userId, String outcome) {
        write(action, userId.toString(), outcome, null, userId, "USER", null);
    }

    public void policy(int idle, int absolute, int maximum) {
        write(
            "SESSION_POLICY_UPDATED",
            "1",
            "SUCCESS",
            null,
            CurrentUser.requireId(),
            "USER",
            "idleMinutes=" + idle + ";absoluteMinutes=" + absolute + ";maxSessions=" + maximum
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failure(String action) {
        record(action, null, "FAILURE");
    }

    private void write(
        String action,
        String target,
        String outcome,
        String targetName,
        Long actorId,
        String actorType,
        String changes
    ) {
        String targetType = target == null ? null : targetType(action);
        String name = targetName == null ? findTargetName(targetType, target) : targetName;
        String actorName = actorId == null ? null : findUserName(actorId.toString());
        if (
            actorName == null &&
            "USER".equals(targetType) &&
            actorId != null &&
            actorId.toString().equals(target)
        ) {
            actorName = name;
        }
        jdbc.update(
            """
            INSERT INTO security_audit_events(
                actor_id, actor_name, actor_type, action, target, target_name, target_type,
                outcome, correlation_id, changes
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            actorId,
            actorName,
            actorType,
            action,
            target,
            name,
            targetType,
            outcome,
            UUID.randomUUID(),
            changes
        );
    }

    private String actorType(Long actorId) {
        return actorId == null ? "UNIDENTIFIED" : "USER";
    }

    private String targetType(String action) {
        return switch (action) {
            case
                "LOGIN",
                "LOGOUT",
                "SESSION_LIMIT_REACHED",
                "SESSION_REVOKED",
                "ADMINISTRATOR_BOOTSTRAPPED",
                "USER_UPDATED",
                "USER_DELETED",
                "INVITATION_ACCEPTED",
                "PASSWORD_RECOVERY_REQUESTED",
                "PASSWORD_RESET",
                "PASSWORD_CHANGED" -> "USER";
            case
                "ROLE_CREATED",
                "ROLE_UPDATED",
                "ROLE_DELETED",
                "ROLE_PERMISSION_GRANTED",
                "ROLE_PERMISSION_REVOKED" -> "ROLE";
            case
                "INVITATION_CREATED",
                "INVITATION_RESENT",
                "INVITATION_CANCELED",
                "ACCESS_EMAIL" -> "INVITATION";
            case "SESSION_POLICY_UPDATED" -> "POLICY";
            default -> null;
        };
    }

    private String findTargetName(String type, String target) {
        if (type == null || target == null) {
            return null;
        }
        return switch (type) {
            case "USER" -> findUserName(target);
            case "ROLE" -> jdbc.queryForList(
                "SELECT name FROM roles WHERE role_id = ?",
                String.class,
                Long.valueOf(target)
            )
                .stream()
                .findFirst()
                .orElse(null);
            case "INVITATION" -> jdbc.queryForList(
                "SELECT name FROM user_invitations WHERE id = ?",
                String.class,
                UUID.fromString(target)
            )
                .stream()
                .findFirst()
                .orElse(null);
            // Labels are translated by the client; no editable name exists for the singleton policy.
            default -> null;
        };
    }

    private String findUserName(String id) {
        return jdbc
            .queryForList(
                "SELECT name FROM users WHERE user_id = ?",
                String.class,
                Long.valueOf(id)
            )
            .stream()
            .findFirst()
            .orElse(null);
    }
}
