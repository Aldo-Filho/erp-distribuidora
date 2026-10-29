-- Names are snapshots for new events. Historical names cannot be reconstructed reliably.
ALTER TABLE security_audit_events
    ADD COLUMN actor_name VARCHAR(150),
    ADD COLUMN actor_type VARCHAR(20),
    ADD COLUMN target_name VARCHAR(150),
    ADD COLUMN target_type VARCHAR(20);
