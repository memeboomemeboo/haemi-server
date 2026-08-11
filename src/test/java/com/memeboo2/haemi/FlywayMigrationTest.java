package com.memeboo2.haemi;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationTest {

    private static final String JDBC_URL = """
            jdbc:h2:mem:flyway-migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=MEMORY;DB_CLOSE_DELAY=-1
            """.trim();

    @Test
    void initialMigrationCreatesSchemaAndIsIdempotent() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(JDBC_URL, "sa", "")
                .locations("classpath:db/migration")
                .load();

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(29);
        assertThat(flyway.migrate().migrationsExecuted).isZero();

        try (var connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            assertThat(tableExists(connection, "members")).isTrue();
            assertThat(tableExists(connection, "photos")).isTrue();
            assertThat(tableExists(connection, "flyway_schema_history")).isTrue();
            assertThat(columnExists(connection, "training_questions", "question_photo_id")).isTrue();
            assertThat(tableExists(connection, "difficulty_policies")).isTrue();
            // #38: 정오답 → 응답 신호 대체
            assertThat(tableExists(connection, "difficulty_profile_response_history")).isTrue();
            assertThat(tableExists(connection, "difficulty_profile_accuracy_history")).isFalse();
            assertThat(tableExists(connection, "difficulty_profile_wrong_patterns")).isFalse();
            assertThat(columnExists(connection, "training_questions", "correct_answer")).isFalse();
            assertThat(columnExists(connection, "training_question_attempts", "responded")).isTrue();
            // #40: 상향 2주기 카운터로 전환
            assertThat(columnExists(connection, "difficulty_profiles", "increase_eligible_sessions")).isTrue();
            assertThat(columnExists(connection, "difficulty_profiles", "consecutive_responded")).isFalse();
            assertThat(columnExists(connection, "difficulty_profiles", "consecutive_no_response")).isFalse();
            assertThat(tableExists(connection, "difficulty_level_changes")).isTrue();
            assertThat(columnExists(connection, "cognitive_training_sessions", "last_chance_status")).isTrue();
            assertThat(columnExists(connection, "cognitive_reports", "viewed_at")).isTrue();
            assertThat(tableExists(connection, "cognitive_report_accuracy_trend")).isTrue();
            assertThat(tableExists(connection, "cognitive_alert_recipient_settings")).isTrue();
            assertThat(tableExists(connection, "cognitive_alert_institution_managers")).isTrue();
            assertThat(columnExists(connection, "voice_alarms", "last_no_response_notified_at")).isTrue();
            assertThat(columnExists(connection, "walk_routines", "last_reminded_at")).isTrue();
            assertThat(columnExists(connection, "album_members", "status")).isTrue();
            assertThat(columnExists(connection, "album_members", "invited_at")).isTrue();
            assertThat(tableExists(connection, "photo_sync_logs")).isTrue();
            assertThat(tableExists(connection, "family_groups")).isTrue();
            assertThat(tableExists(connection, "elders")).isTrue();
            assertThat(tableExists(connection, "persons")).isTrue();
            assertThat(tableExists(connection, "photo_persons")).isTrue();
            assertThat(columnExists(connection, "reminiscence_slides", "safety_passed")).isTrue();
            assertThat(tableExists(connection, "memory")).isTrue();
            assertThat(tableExists(connection, "memory_media")).isTrue();
            assertThat(columnExists(connection, "cognitive_reports", "report_mode")).isTrue();
            assertThat(columnExists(connection, "cognitive_daily_metrics", "voice_detected_count")).isTrue();
            // #41: 사전 적립형 손주 한마디
            assertThat(tableExists(connection, "accrued_hints")).isTrue();
            // #42: M2 랭킹 폐기
            assertThat(tableExists(connection, "family_rankings")).isFalse();
            assertThat(tableExists(connection, "ranking_entries")).isFalse();
            assertThat(tableExists(connection, "member_star_summaries")).isFalse();
            // #43: 그룹 협력 목표 (랭킹 대체)
            assertThat(tableExists(connection, "group_goals")).isTrue();
            assertThat(tableExists(connection, "group_goal_participants")).isTrue();
            // #46: 목소리 알람 음성 로테이션
            assertThat(tableExists(connection, "voice_alarm_voices")).isTrue();
            assertThat(columnExists(connection, "voice_alarms", "voice_rotation_index")).isTrue();
            // #49: 오프라인 결과 멱등 수신 영수증
            assertThat(tableExists(connection, "offline_result_receipts")).isTrue();
            // #36: 어르신 사별 생명주기 컬럼
            assertThat(columnExists(connection, "elders", "bereaved_at")).isTrue();
            assertThat(columnExists(connection, "elders", "silent_until")).isTrue();
            assertThat(columnExists(connection, "elders", "bereavement_requested_at")).isTrue();
            // #37: 이벤트 로깅 파이프라인
            assertThat(tableExists(connection, "logged_events")).isTrue();
            assertThat(tableExists(connection, "event_collection_consent")).isTrue();
            // #35: 접근 모드 추천
            assertThat(tableExists(connection, "access_mode_recommendations")).isTrue();
            // #80: FCM 기기 토큰
            assertThat(tableExists(connection, "device_tokens")).isTrue();
            assertThat(columnExists(connection, "device_tokens", "member_id")).isTrue();
            assertThat(columnExists(connection, "device_tokens", "platform")).isTrue();
            assertThat(columnExists(connection, "device_tokens", "last_used_at")).isTrue();
            // #81 보완: 계정 소유자와 어르신 본인 기기 수신 대상을 분리
            assertThat(columnExists(connection, "elders", "member_id")).isTrue();
            assertThat(columnExists(connection, "device_tokens", "elder_id")).isTrue();
            // #86: 회원 식별자 타입을 다른 테이블과 맞춘다
            assertThat(columnType(connection, "device_tokens", "member_id")).isEqualTo("UUID");
            assertThat(tableExists(connection, "institution_assignments")).isTrue();
            assertThat(tableExists(connection, "email_verifications")).isTrue();
            assertThat(columnExists(connection, "members", "email_verified_at")).isTrue();
            // 사별 기기 명령은 실패해도 아웃박스에 남아 재시도한다.
            assertThat(tableExists(connection, "device_commands")).isTrue();
            assertThat(columnExists(connection, "device_commands", "next_attempt_at")).isTrue();
            // 회상 세션은 음성 원문 대신 VAD 길이만 보관한다.
            assertThat(columnExists(connection, "training_question_attempts", "submitted_answer")).isFalse();
            assertThat(columnExists(connection, "training_question_attempts", "vad_duration_ms")).isTrue();
            assertThat(columnExists(connection, "cognitive_change_alerts", "false_positive_at")).isTrue();
        }
    }

    private boolean tableExists(java.sql.Connection connection, String tableName) throws Exception {
        try (var tables = connection.getMetaData().getTables(null, "public", tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    private String columnType(
            java.sql.Connection connection,
            String tableName,
            String columnName
    ) throws Exception {
        try (var columns = connection.getMetaData().getColumns(null, "public", tableName, columnName)) {
            return columns.next() ? columns.getString("TYPE_NAME") : null;
        }
    }

    private boolean columnExists(
            java.sql.Connection connection,
            String tableName,
            String columnName
    ) throws Exception {
        try (var columns = connection.getMetaData().getColumns(null, "public", tableName, columnName)) {
            return columns.next();
        }
    }
}
