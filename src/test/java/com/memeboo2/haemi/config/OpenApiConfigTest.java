package com.memeboo2.haemi.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    private final OpenApiConfig openApiConfig = new OpenApiConfig();

    @Test
    void usesConfiguredServerUrlBeforeLocalServer() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("haemi.openapi.server-url", "http://ec2.example.com:8080");

        OpenAPI openAPI = openApiConfig.haemiOpenAPI(environment);

        assertThat(openAPI.getServers())
                .extracting("url")
                .containsExactly("http://ec2.example.com:8080", "http://localhost:8080");
    }

    @Test
    void excludesLocalServerWhenDisabled() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("haemi.openapi.server-url", "http://ec2.example.com:8080")
                .withProperty("haemi.openapi.include-local-server", "false");

        OpenAPI openAPI = openApiConfig.haemiOpenAPI(environment);

        assertThat(openAPI.getServers())
                .extracting("url")
                .containsExactly("http://ec2.example.com:8080");
    }
}
