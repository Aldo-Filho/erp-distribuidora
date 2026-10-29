-- Never merge accounts or rewrite fixtures. Resolve collisions before retrying Flyway.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM users GROUP BY lower(btrim(email)) HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Normalized user emails collide. Resolve accounts explicitly before migration.';
    END IF;
END $$;
CREATE UNIQUE INDEX uk_users_email_normalized ON users (lower(btrim(email)));
ALTER TABLE users ADD COLUMN credential_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE spring_session (
    primary_id CHAR(36) PRIMARY KEY,
    session_id CHAR(36) NOT NULL,
    creation_time BIGINT NOT NULL,
    last_access_time BIGINT NOT NULL,
    max_inactive_interval INT NOT NULL,
    expiry_time BIGINT NOT NULL,
    principal_name VARCHAR(100)
);
CREATE UNIQUE INDEX spring_session_ix1 ON spring_session (session_id);
CREATE INDEX spring_session_ix2 ON spring_session (expiry_time);
CREATE INDEX spring_session_ix3 ON spring_session (principal_name);
CREATE TABLE spring_session_attributes (
    session_primary_id CHAR(36) NOT NULL REFERENCES spring_session(primary_id) ON DELETE CASCADE,
    attribute_name VARCHAR(200) NOT NULL,
    attribute_bytes BYTEA NOT NULL,
    PRIMARY KEY (session_primary_id, attribute_name)
);
CREATE TABLE security_settings (
    id BIGINT PRIMARY KEY CHECK (id = 1),
    idle_minutes INT NOT NULL CHECK (idle_minutes BETWEEN 15 AND 240),
    absolute_minutes INT NOT NULL CHECK (absolute_minutes BETWEEN 60 AND 1440),
    max_sessions INT NOT NULL CHECK (max_sessions BETWEEN 1 AND 5),
    policy_version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by BIGINT,
    CHECK (idle_minutes <= absolute_minutes)
);
INSERT INTO security_settings (id, idle_minutes, absolute_minutes, max_sessions) VALUES (1, 60, 720, 2);
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY,
    session_id CHAR(36) NOT NULL UNIQUE REFERENCES spring_session(session_id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    authenticated_at TIMESTAMPTZ NOT NULL,
    last_activity_at TIMESTAMPTZ NOT NULL,
    credential_version BIGINT NOT NULL,
    browser VARCHAR(120) NOT NULL,
    revoked_at TIMESTAMPTZ
);
CREATE INDEX ix_user_sessions_user ON user_sessions(user_id, last_activity_at);
CREATE INDEX ix_user_sessions_validity ON user_sessions(authenticated_at, last_activity_at) WHERE revoked_at IS NULL;
CREATE TABLE user_invitations (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    role_id BIGINT NOT NULL REFERENCES roles(role_id),
    created_by BIGINT,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    canceled_at TIMESTAMPTZ,
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);
CREATE UNIQUE INDEX uk_pending_invitation_email ON user_invitations(lower(btrim(email)))
    WHERE consumed_at IS NULL AND canceled_at IS NULL;
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    credential_version BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ
);
CREATE TABLE login_challenges (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    credential_version BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    context_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ
);
CREATE INDEX ix_reset_user ON password_reset_tokens(user_id);
CREATE INDEX ix_challenge_user ON login_challenges(user_id);
CREATE TABLE security_audit_events (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT,
    action VARCHAR(100) NOT NULL,
    target VARCHAR(150),
    outcome VARCHAR(30) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    correlation_id UUID NOT NULL,
    changes VARCHAR(500)
);
CREATE INDEX ix_security_audit_time ON security_audit_events(occurred_at DESC, id DESC);

