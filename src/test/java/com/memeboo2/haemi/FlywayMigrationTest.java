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

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(flyway.migrate().migrationsExecuted).isZero();

        try (var connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            assertThat(tableExists(connection, "members")).isTrue();
            assertThat(tableExists(connection, "photos")).isTrue();
            assertThat(tableExists(connection, "flyway_schema_history")).isTrue();
        }
    }

    private boolean tableExists(java.sql.Connection connection, String tableName) throws Exception {
        try (var tables = connection.getMetaData().getTables(null, "public", tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }
}
