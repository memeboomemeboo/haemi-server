package com.memeboo2.haemi.m2.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memeboo2.haemi.m2.domain.model.post.AiGenerationUnavailableException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiGenerationClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<String> requestPath = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private HttpServer server;
    private int responseStatus;
    private String responseBody;

    @BeforeEach
    void setUp() throws IOException {
        responseStatus = 200;
        responseBody = """
                {"candidates":[{"content":{"parts":[{"text":"생성 결과"}]}}]}
                """;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestPath.set(exchange.getRequestURI().getPath() + "?" + exchange.getRequestURI().getQuery());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsPoemPromptToGeminiGenerateContentEndpoint() throws Exception {
        String result = client("test-key").generatePoem("추억글 원문");

        assertThat(result).isEqualTo("생성 결과");
        assertThat(requestPath.get()).isEqualTo("/v1beta/models/test-model:generateContent?key=test-key");
        JsonNode request = objectMapper.readTree(requestBody.get());
        assertThat(request.at("/contents/0/parts/0/text").asText()).isEqualTo("추억글 원문");
        assertThat(request.at("/generationConfig/temperature").asDouble()).isEqualTo(0.5);
        assertThat(request.at("/generationConfig/maxOutputTokens").asInt()).isEqualTo(160);
    }

    @Test
    void sendsAudioAsBase64InlineDataForTranscription() throws Exception {
        String result = client("test-key").transcribe("voice".getBytes(StandardCharsets.UTF_8), "audio/mpeg");

        assertThat(result).isEqualTo("생성 결과");
        JsonNode request = objectMapper.readTree(requestBody.get());
        assertThat(request.at("/contents/0/parts/1/inlineData/mimeType").asText()).isEqualTo("audio/mpeg");
        assertThat(request.at("/contents/0/parts/1/inlineData/data").asText()).isEqualTo("dm9pY2U=");
        assertThat(request.at("/generationConfig/temperature").asDouble()).isZero();
    }

    @Test
    void failsExplicitlyWhenApiKeyIsMissing() {
        assertThatThrownBy(() -> client("").generatePoem("추억글"))
                .isInstanceOf(AiGenerationUnavailableException.class)
                .hasMessageContaining("설정되지 않았어요");
        assertThat(requestBody.get()).isNull();
    }

    @Test
    void convertsUpstreamFailureToServiceUnavailableException() {
        responseStatus = 503;
        responseBody = "{\"error\":{\"message\":\"unavailable\"}}";

        assertThatThrownBy(() -> client("test-key").generatePoem("추억글"))
                .isInstanceOf(AiGenerationUnavailableException.class)
                .hasMessageContaining("AI 요청을 처리하지 못했어요");
    }

    private GeminiGenerationClient client(String apiKey) {
        return new GeminiGenerationClient(
                RestClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build(),
                objectMapper, apiKey, "test-model");
    }
}
