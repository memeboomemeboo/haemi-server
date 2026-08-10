ALTER TABLE elders ADD COLUMN elder_member_id UUID;

ALTER TABLE elders
    ADD CONSTRAINT uk_elders_elder_member UNIQUE (elder_member_id);

ALTER TABLE elders
    ADD CONSTRAINT fk_elders_elder_member FOREIGN KEY (elder_member_id) REFERENCES members(id);
