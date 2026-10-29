package com.pi.erp.auth.session;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserSessionRepository {

    @Autowired
    private JdbcTemplate jdbc;

    public UserSession findBySessionId(String sessionId) {
        return jdbc
            .query("SELECT * FROM user_sessions WHERE session_id = ?", this::map, sessionId)
            .stream()
            .findFirst()
            .orElse(null);
    }

    public List<UserSession> findByUserId(Long userId) {
        return jdbc.query(
            """
            SELECT * FROM user_sessions WHERE user_id = ? AND revoked_at IS NULL
            ORDER BY last_activity_at DESC, id
            """,
            this::map,
            userId
        );
    }

    public void register(UUID id, String sessionId, Long userId, long version, String browser) {
        jdbc.update(
            """
            INSERT INTO user_sessions(id, session_id, user_id, authenticated_at,
            last_activity_at, credential_version, browser) VALUES (?, ?, ?, now(), now(), ?, ?)
            """,
            id,
            sessionId,
            userId,
            version,
            browser
        );
    }

    public void revoke(UUID id) {
        // Tombstone remains until physical session cleanup; stale requests cannot revive admission.
        jdbc.update(
            "UPDATE user_sessions SET revoked_at = now() WHERE id = ? AND revoked_at IS NULL",
            id
        );
    }

    public void activity(UUID id) {
        jdbc.update(
            """
            UPDATE user_sessions SET last_activity_at = now()
            WHERE id = ? AND revoked_at IS NULL AND last_activity_at <= now() - interval '1 minute'
            """,
            id
        );
    }

    private UserSession map(ResultSet row, int index) throws SQLException {
        return new UserSession(
            row.getObject("id", UUID.class),
            row.getString("session_id"),
            row.getLong("user_id"),
            row.getTimestamp("authenticated_at").toInstant(),
            row.getTimestamp("last_activity_at").toInstant(),
            row.getLong("credential_version"),
            row.getString("browser"),
            row.getTimestamp("revoked_at") == null
                ? null
                : row.getTimestamp("revoked_at").toInstant()
        );
    }
}
