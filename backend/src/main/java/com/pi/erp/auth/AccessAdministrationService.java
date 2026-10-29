package com.pi.erp.auth;

import com.pi.erp.auth.audit.SecurityAuditService;
import com.pi.erp.auth.settings.SecuritySettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AccessAdministrationService {

    @Autowired
    private SecuritySettingsRepository securitySettingsRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SecurityAuditService securityAuditService;

    public void lock() {
        securitySettingsRepository.lock();
    }

    public boolean hasAdministrator() {
        return Boolean.TRUE.equals(
            jdbc.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1 FROM users u JOIN roles r ON r.role_id = u.role_id
                    WHERE u.active AND r.active AND (
                        SELECT count(DISTINCT p.permission_key)
                        FROM role_permissions rp JOIN permissions p USING(permission_id)
                        WHERE rp.role_id = r.role_id AND p.permission_key IN
                        ('access.users.manage', 'access.roles.manage', 'security.settings.manage')
                    ) = 3
                )
                """,
                Boolean.class
            )
        );
    }

    public void verifyAdministrator() {
        if (!hasAdministrator()) {
            throw new IllegalArgumentException(
                "At least one active access administrator must remain."
            );
        }
    }

    public void invalidate(Long userId) {
        jdbc.update(
            "UPDATE user_sessions SET revoked_at = now() WHERE user_id = ? AND revoked_at IS NULL",
            userId
        );
        jdbc.update(
            "UPDATE login_challenges SET consumed_at = now() WHERE user_id = ? AND consumed_at IS NULL",
            userId
        );
        jdbc.update(
            "UPDATE password_reset_tokens SET consumed_at = now() WHERE user_id = ? AND consumed_at IS NULL",
            userId
        );
    }

    public void invalidateRole(Long roleId) {
        jdbc.queryForList(
            "SELECT user_id FROM users WHERE role_id = ? ORDER BY user_id",
            Long.class,
            roleId
        ).forEach(this::invalidate);
    }

    public void audit(String action, Long id) {
        securityAuditService.record(action, id.toString(), "SUCCESS");
    }

    public void audit(String action, Long id, String name) {
        securityAuditService.record(action, id.toString(), "SUCCESS", name);
    }
}
