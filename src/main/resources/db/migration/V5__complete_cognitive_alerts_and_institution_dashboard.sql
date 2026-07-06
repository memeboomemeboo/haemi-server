CREATE TABLE cognitive_alert_recipient_settings (
    id UUID PRIMARY KEY,
    elder_id VARCHAR(255) NOT NULL UNIQUE,
    primary_caregiver_member_id VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE cognitive_alert_institution_managers (
    setting_id UUID NOT NULL,
    member_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (setting_id, member_id),
    CONSTRAINT fk_cognitive_alert_manager_setting
        FOREIGN KEY (setting_id) REFERENCES cognitive_alert_recipient_settings(id)
);
