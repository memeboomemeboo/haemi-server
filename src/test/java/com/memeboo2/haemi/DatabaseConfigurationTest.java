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
                .containsEntry("spring.flyway.enabled", true);
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
                .containsEntry("springdoc.api-docs.enabled", true)
                .containsEntry("springdoc.swagger-ui.enabled", true);
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
