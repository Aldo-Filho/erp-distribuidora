package com.pi.erp.auth.settings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SecuritySettingsRepository {

    @Autowired
    private JdbcTemplate jdbc;

    public SecuritySettings findOne() {
        return jdbc.queryForObject("SELECT * FROM security_settings WHERE id = 1", (row, index) ->
            new SecuritySettings(
                row.getInt("idle_minutes"),
                row.getInt("absolute_minutes"),
                row.getInt("max_sessions"),
                row.getLong("policy_version")
            )
        );
    }

    // Must be called inside the caller's transaction, before any access mutation.
    public void lock() {
        jdbc.queryForObject("SELECT id FROM security_settings WHERE id = 1 FOR UPDATE", Long.class);
    }

    public SecuritySettings update(int idle, int absolute, int maximum, Long actor) {
        jdbc.update(
            """
            UPDATE security_settings SET idle_minutes = ?, absolute_minutes = ?, max_sessions = ?,
            policy_version = policy_version + 1, updated_at = now(), updated_by = ? WHERE id = 1
            """,
            idle,
            absolute,
            maximum,
            actor
        );
        return findOne();
    }
}
