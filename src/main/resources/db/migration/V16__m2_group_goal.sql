-- #43 F1-03-A 그룹 협력 목표 (개인 순위·뱃지·스트릭 없는 협력형 목표, #42 랭킹 대체)

CREATE TABLE group_goals (
    id               UUID         PRIMARY KEY,
    album_id         UUID         NOT NULL,
    period           VARCHAR(16)  NOT NULL,
    period_start     DATE         NOT NULL,
    period_end       DATE         NOT NULL,
    target_count     INT          NOT NULL,
    current_progress INT          NOT NULL DEFAULT 0,
    status           VARCHAR(16)  NOT NULL,
    achieved_at      TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL,
    CONSTRAINT uq_group_goals_album_period UNIQUE (album_id, period, period_start)
);

CREATE INDEX idx_group_goals_album_active ON group_goals (album_id, status, period_start);

-- 순위 없이 "함께한 참여자"만 담는 집합
CREATE TABLE group_goal_participants (
    goal_id   UUID         NOT NULL,
    member_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_group_goal_participants_goal FOREIGN KEY (goal_id) REFERENCES group_goals (id)
);

CREATE INDEX idx_group_goal_participants_goal ON group_goal_participants (goal_id);
