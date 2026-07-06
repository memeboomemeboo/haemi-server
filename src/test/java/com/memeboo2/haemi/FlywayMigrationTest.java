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

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(6);
        assertThat(flyway.migrate().migrationsExecuted).isZero();

        try (var connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            assertThat(tableExists(connection, "members")).isTrue();
            assertThat(tableExists(connection, "photos")).isTrue();
            assertThat(tableExists(connection, "flyway_schema_history")).isTrue();
            assertThat(columnExists(connection, "training_questions", "question_photo_id")).isTrue();
            assertThat(tableExists(connection, "difficulty_policies")).isTrue();
            assertThat(tableExists(connection, "difficulty_profile_accuracy_history")).isTrue();
            assertThat(tableExists(connection, "difficulty_profile_wrong_patterns")).isTrue();
            assertThat(tableExists(connection, "difficulty_level_changes")).isTrue();
            assertThat(columnExists(connection, "cognitive_training_sessions", "last_chance_status")).isTrue();
            assertThat(columnExists(connection, "cognitive_reports", "viewed_at")).isTrue();
            assertThat(tableExists(connection, "cognitive_report_accuracy_trend")).isTrue();
            assertThat(tableExists(connection, "cognitive_alert_recipient_settings")).isTrue();
            assertThat(tableExists(connection, "cognitive_alert_institution_managers")).isTrue();
            assertThat(columnExists(connection, "voice_alarms", "last_no_response_notified_at")).isTrue();
            assertThat(columnExists(connection, "walk_routines", "last_reminded_at")).isTrue();
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
