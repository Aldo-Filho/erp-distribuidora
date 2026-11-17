-- Independent version: changing the profile must not invalidate appearance edits.
ALTER TABLE company_appearance ADD COLUMN profile_version BIGINT NOT NULL DEFAULT 0;

INSERT INTO permissions(module, action, permission_key)
VALUES ('company', 'read', 'company.read'), ('company', 'manage', 'company.manage')
ON CONFLICT (permission_key) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.role_id, p.permission_id FROM roles r CROSS JOIN permissions p
WHERE p.permission_key IN ('company.read', 'company.manage')
AND (
    SELECT count(DISTINCT existing.permission_key)
    FROM role_permissions rp JOIN permissions existing USING (permission_id)
    WHERE rp.role_id = r.role_id AND existing.permission_key IN
        ('access.users.manage', 'access.roles.manage', 'security.settings.manage')
) = 3
ON CONFLICT DO NOTHING;
