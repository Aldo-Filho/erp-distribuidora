package com.pi.erp.company;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AppearanceRepository {

    @Autowired
    private JdbcTemplate jdbc;

    public record Settings(String palette, String filename, long version) {
        public Appearance response() {
            return new Appearance(
                palette,
                filename == null ? null : "/company/appearance/logo?v=" + version,
                version
            );
        }
    }

    public Settings find(boolean lock) {
        return jdbc.queryForObject(
            "SELECT palette, logo_filename, version FROM company_appearance WHERE id = 1" +
                (lock ? " FOR UPDATE" : ""),
            (row, index) ->
                new Settings(
                    row.getString("palette"),
                    row.getString("logo_filename"),
                    row.getLong("version")
                )
        );
    }

    public Appearance update(String palette, String filename, Long actor) {
        jdbc.update(
            """
            UPDATE company_appearance SET palette = ?, logo_filename = ?, version = version + 1,
                updated_at = now(), updated_by = ? WHERE id = 1
            """,
            palette,
            filename,
            actor
        );
        return find(false).response();
    }
}
