package com.pi.erp.preference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ThemePreferenceRepository {

    @Autowired
    private JdbcTemplate jdbc;

    public ThemePreference find(Long userId) {
        return jdbc.queryForObject(
            "SELECT theme_preference FROM users WHERE user_id = ?",
            (row, index) -> new ThemePreference(row.getString(1)),
            userId
        );
    }

    public void update(Long userId, String theme) {
        if (
            jdbc.update("UPDATE users SET theme_preference = ? WHERE user_id = ?", theme, userId) !=
            1
        ) throw new com.pi.erp.exception.ResourceNotFoundException("Usuário não encontrado.");
    }
}
