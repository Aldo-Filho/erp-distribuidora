-- No historical timestamp can be inferred safely.
ALTER TABLE orders ADD COLUMN confirmed_at TIMESTAMP;
CREATE INDEX ix_orders_dashboard_confirmation ON orders(status, confirmed_at);
CREATE TABLE dashboard_preferences (
 user_id BIGINT PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
 schema_version INTEGER NOT NULL DEFAULT 1 CHECK (schema_version = 1),
 payload JSONB NOT NULL,
 revision BIGINT NOT NULL DEFAULT 1,
 updated_at TIMESTAMP NOT NULL DEFAULT LOCALTIMESTAMP,
 CHECK (octet_length(payload::text) <= 16384)
);
