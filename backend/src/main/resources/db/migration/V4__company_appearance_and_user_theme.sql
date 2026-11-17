-- One application-wide appearance, referencing the existing company identity when available.
CREATE TABLE company_appearance (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    company_id BIGINT REFERENCES companies(company_id) ON DELETE SET NULL,
    palette VARCHAR(20) NOT NULL DEFAULT 'jade'
        CHECK (palette IN ('jade', 'ocean', 'violet', 'terracotta', 'amber', 'graphite')),
    logo_filename VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP,
    updated_by BIGINT REFERENCES users(user_id) ON DELETE SET NULL
);

INSERT INTO company_appearance (id, company_id) VALUES (1, (SELECT min(company_id) FROM companies));

-- NULL follows the operating system. This does not alter credentials or session versions.
ALTER TABLE users ADD COLUMN theme_preference VARCHAR(5)
    CHECK (theme_preference IS NULL OR theme_preference IN ('LIGHT', 'DARK'));

INSERT INTO permissions(module, action, permission_key)
VALUES ('company.appearance', 'manage', 'company.appearance.manage')
ON CONFLICT (permission_key) DO NOTHING;

-- Existing access administrators receive the dedicated permission; no role IDs/names assumed.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id FROM roles r
CROSS JOIN permissions p
WHERE p.permission_key = 'company.appearance.manage'
AND (
    SELECT count(DISTINCT existing.permission_key)
    FROM role_permissions rp JOIN permissions existing USING (permission_id)
    WHERE rp.role_id = r.role_id AND existing.permission_key IN
        ('access.users.manage', 'access.roles.manage', 'security.settings.manage')
) = 3
ON CONFLICT DO NOTHING;
