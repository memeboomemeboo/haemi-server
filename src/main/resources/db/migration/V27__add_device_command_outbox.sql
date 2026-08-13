CREATE TABLE device_commands (
    id UUID PRIMARY KEY,
    elder_id UUID NOT NULL REFERENCES elders(id),
    action VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL,
    delivered_at TIMESTAMP,
    last_error VARCHAR(500),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_device_commands_retry
    ON device_commands (status, next_attempt_at, created_at);
