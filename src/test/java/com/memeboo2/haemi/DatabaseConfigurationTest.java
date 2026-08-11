package com.memeboo2.haemi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConfigurationTest {

    @Test
    void mainProfileDefaultsToPostgresql() {
        Properties properties = loadYaml("application.yaml");

        assertThat(properties)
                .containsEntry(
                        "spring.autoconfigure.exclude",
                        "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration"
                )
                .containsEntry("spring.datasource.driver-class-name", "org.postgresql.Driver")
                .containsEntry("spring.datasource.url", "${DB_URL:jdbc:postgresql://localhost:5432/haemi}")
                .containsEntry("spring.jpa.hibernate.ddl-auto", "validate")
                .containsEntry("spring.flyway.enabled", true)
                .containsEntry("haemi.openapi.server-url", "${OPENAPI_SERVER_URL:}")
                .containsEntry("haemi.openapi.local-server-url", "${OPENAPI_LOCAL_SERVER_URL:http://localhost:8080}")
                .containsEntry("haemi.openapi.include-local-server", "${OPENAPI_INCLUDE_LOCAL_SERVER:true}");
    }

    @Test
    void productionProfileValidatesSchemaWithoutChangingIt() {
        Properties properties = loadYaml("application-prod.yaml");

        assertThat(properties)
                .containsEntry("spring.datasource.url", "${DB_URL}")
                .containsEntry("spring.datasource.username", "${DB_USERNAME}")
                .containsEntry("spring.datasource.password", "${DB_PASSWORD}")
                .containsEntry("spring.jpa.hibernate.ddl-auto", "validate")
                .containsEntry("haemi.jwt.secret", "${JWT_SECRET}")
                .containsEntry("haemi.openapi.include-local-server", false)
                // #100: 기본은 지금처럼 켜 두되, 배포 설정만으로 끌 수 있어야 한다.
                .containsEntry("springdoc.api-docs.enabled", "${API_DOCS_ENABLED:true}")
                .containsEntry("springdoc.swagger-ui.enabled", "${API_DOCS_ENABLED:true}");
    }

    @Test
    void developmentProfileEnablesApiDocumentation() {
        Properties properties = loadYaml("application-dev.yaml");

        assertThat(properties)
                .containsEntry("springdoc.api-docs.enabled", true)
                .containsEntry("springdoc.swagger-ui.enabled", true);
    }

    private Properties loadYaml(String filename) {
        var factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(filename));
        return factory.getObject();
    }
}
