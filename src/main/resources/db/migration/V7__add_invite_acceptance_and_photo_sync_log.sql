ALTER TABLE album_members
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED';

ALTER TABLE album_members
    ADD COLUMN invited_at TIMESTAMP(6) NOT NULL DEFAULT now();

CREATE TABLE photo_sync_logs (
    id UUID PRIMARY KEY,
    album_id UUID NOT NULL,
    member_id VARCHAR(255) NOT NULL,
    synced_at TIMESTAMP(6) NOT NULL,
    requested_count INTEGER NOT NULL,
    saved_count INTEGER NOT NULL,
    skipped_count INTEGER NOT NULL,
    network_type VARCHAR(20),
    battery_level INTEGER,
    background_sync BOOLEAN NOT NULL,
    CONSTRAINT fk_photo_sync_logs_album FOREIGN KEY (album_id) REFERENCES albums(id)
);
