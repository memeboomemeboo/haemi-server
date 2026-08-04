package com.memeboo2.haemi;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationTest {

    private static final String JDBC_URL = """
            jdbc:h2:mem:flyway-migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
            """.trim();

    @Test
    void initialMigrationCreatesSchemaAndIsIdempotent() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(JDBC_URL, "sa", "")
                .locations("classpath:db/migration")
                .load();
              
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(11);

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
            assertThat(columnExists(connection, "difficulty_profiles", "consecutive_responded")).isTrue();
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
        }
    }

    private boolean tableExists(java.sql.Connection connection, String tableName) throws Exception {
        try (var tables = connection.getMetaData().getTables(null, "public", tableName, new String[]{"TABLE"})) {
            return tables.next();
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
