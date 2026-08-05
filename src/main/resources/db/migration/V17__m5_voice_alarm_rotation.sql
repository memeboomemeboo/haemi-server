-- #46 F5-01 목소리 알람 — 가족 음성 로테이션 풀 도입

ALTER TABLE voice_alarms ADD COLUMN voice_rotation_index INT NOT NULL DEFAULT 0;

CREATE TABLE voice_alarm_voices (
    alarm_id    UUID         NOT NULL,
    voice_key   VARCHAR(255) NOT NULL,
    voice_order INT          NOT NULL,
    CONSTRAINT fk_voice_alarm_voices_alarm FOREIGN KEY (alarm_id) REFERENCES voice_alarms (id)
);

CREATE INDEX idx_voice_alarm_voices_alarm ON voice_alarm_voices (alarm_id);

-- 기존 단일 voice_key를 로테이션 풀 0번으로 이관
INSERT INTO voice_alarm_voices (alarm_id, voice_key, voice_order)
SELECT id, voice_key, 0 FROM voice_alarms WHERE voice_key IS NOT NULL;
