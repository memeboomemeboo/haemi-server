package com.memeboo2.haemi.m2.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memeboo2.haemi.m2.domain.model.post.AiGenerationUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/** M2의 시 초안·비실시간 음성 전사를 Gemini generateContent API로 요청한다. */
@Component
@Slf4j
public class GeminiGenerationClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    @Autowired
    public GeminiGenerationClient(
            @Value("${haemi.ai.gemini.api-key:}") String apiKey,
            @Value("${haemi.ai.gemini.model:gemini-3.6-flash}") String model,
            @Value("${haemi.ai.gemini.connect-timeout:5s}") Duration connectTimeout,
            @Value("${haemi.ai.gemini.read-timeout:20s}") Duration readTimeout
    ) {
        this(RestClient.builder()
                        .baseUrl("https://generativelanguage.googleapis.com")
                        .requestFactory(requestFactory(connectTimeout, readTimeout))
                        .build(),
                new ObjectMapper(), apiKey, model);
    }

    GeminiGenerationClient(RestClient restClient, ObjectMapper objectMapper, String apiKey, String model) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String generatePoem(String prompt) {
        return generate(List.of(Map.of("text", prompt)), 0.5, 160);
    }

    public String transcribe(byte[] audio, String contentType) {
        return generate(List.of(
                Map.of("text", """
                        이 오디오에서 실제로 들리는 한국어 발화만 전사하세요.
                        해설, 요약, 화자 표기, 추측, 인사말을 덧붙이지 마세요.
                        알아들을 수 없는 구간은 [알아들을 수 없음]으로 표기하고, 전사 결과만 반환하세요.
                        """),
                Map.of("inlineData", Map.of(
                        "mimeType", contentType,
                        "data", Base64.getEncoder().encodeToString(audio)
                ))), 0.0, 256);
    }

    private String generate(List<Map<String, Object>> parts, double temperature, int maxOutputTokens) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiGenerationUnavailableException("AI 기능이 아직 설정되지 않았어요. 잠시 후 다시 시도해주세요.");
        }
        try {
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", parts)),
                    "generationConfig", Map.of(
                            "temperature", temperature,
                            "maxOutputTokens", maxOutputTokens
                    )
            );
            String response = restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey).build(model))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response);
            String generatedText = root.at("/candidates/0/content/parts/0/text").asText().trim();
            if (generatedText.isBlank()) {
                throw new AiGenerationUnavailableException("AI가 사용할 수 있는 응답을 만들지 못했어요. 잠시 후 다시 시도해주세요.");
            }
            return generatedText;
        } catch (AiGenerationUnavailableException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Gemini M2 생성 요청 실패: {}", exception.getClass().getSimpleName());
            throw new AiGenerationUnavailableException("AI 요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.", exception);
        }
    }

    private static ClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
