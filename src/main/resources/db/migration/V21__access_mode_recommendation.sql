-- #35 F0-03 접근 모드 추천 (진단·재평가 제안, 대행 실행 기록)

CREATE TABLE access_mode_recommendations (
    id               UUID        PRIMARY KEY,
    elder_id         UUID        NOT NULL,
    recommended_mode VARCHAR(10) NOT NULL,
    source           VARCHAR(20) NOT NULL,
    status           VARCHAR(12) NOT NULL,
    entry_path       VARCHAR(12),
    operator_id      UUID,
    created_at       TIMESTAMP   NOT NULL,
    applied_at       TIMESTAMP,
    CONSTRAINT fk_access_mode_reco_elder FOREIGN KEY (elder_id) REFERENCES elders (id)
);

CREATE INDEX idx_access_mode_reco_elder ON access_mode_recommendations (elder_id, created_at);
