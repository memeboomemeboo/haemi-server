package com.memeboo2.haemi.m2.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memeboo2.haemi.m2.domain.model.post.AiGenerationRateLimitedException;
import com.memeboo2.haemi.m2.domain.model.post.AiGenerationRejectedException;
import com.memeboo2.haemi.m2.domain.model.post.AiGenerationUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * M2의 시 초안·비실시간 음성 전사를 Gemini generateContent API로 요청한다.
 *
 * <p>M1의 {@code GeminiReminiscenceCardGenerator}와 HTTP 형태가 비슷하지만 공유하지 않는다.
 * M1은 08:00 배치에서 검증된 결정론적 카드로 안전하게 폴백해야 하고, M2는 사용자가 보낸
 * 음성·시 초안 요청이므로 불완전한 결과를 저장하지 않고 명시적으로 실패시켜야 한다.</p>
 */
@Component
@Slf4j
public class GeminiGenerationClient {

    private static final int POEM_MAX_OUTPUT_TOKENS = 512;
    private static final int TRANSCRIPT_MAX_OUTPUT_TOKENS = 1024;
    private static final int LOGGED_ERROR_BODY_MAX_LENGTH = 500;

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
        return generate(List.of(Map.of("text", prompt)), 0.5, POEM_MAX_OUTPUT_TOKENS);
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
                ))), 0.0, TRANSCRIPT_MAX_OUTPUT_TOKENS);
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
                            .build(model))
                    // URL에 API 키를 넣으면 예외와 액세스 로그를 통해 유출될 수 있다.
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response);
            String finishReason = root.at("/candidates/0/finishReason").asText();
            if (!"STOP".equals(finishReason)) {
                log.warn("Gemini M2 응답이 완결되지 않음: finishReason={}", finishReason);
                throw new AiGenerationUnavailableException(
                        "AI 응답이 끝까지 생성되지 않았어요. 잠시 후 다시 시도해주세요.");
            }
            String generatedText = root.at("/candidates/0/content/parts/0/text").asText().trim();
            if (generatedText.isBlank()) {
                throw new AiGenerationUnavailableException("AI가 사용할 수 있는 응답을 만들지 못했어요. 잠시 후 다시 시도해주세요.");
            }
            return generatedText;
        } catch (AiGenerationUnavailableException | AiGenerationRateLimitedException
                 | AiGenerationRejectedException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            log.warn("Gemini M2 업스트림 오류: status={}, body={}", status,
                    summarizeResponseBody(exception.getResponseBodyAsString()));
            throw translateUpstreamFailure(status);
        } catch (Exception exception) {
            log.warn("Gemini M2 생성 요청 실패: {}", exception.getClass().getSimpleName());
            throw new AiGenerationUnavailableException("AI 요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.", exception);
        }
    }

    private RuntimeException translateUpstreamFailure(int status) {
        if (status == 429) {
            return new AiGenerationRateLimitedException("AI 요청이 많아요. 잠시 후 다시 시도해주세요.");
        }
        if (status >= 400 && status < 500 && status != 401 && status != 403) {
            return new AiGenerationRejectedException("AI 서비스가 요청을 처리하지 못했어요. 다시 시도해주세요.");
        }
        if (status == 401 || status == 403) {
            return new AiGenerationUnavailableException("AI 인증 설정을 확인해주세요. 잠시 후 다시 시도해주세요.");
        }
        return new AiGenerationUnavailableException("AI 요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.");
    }

    private static String summarizeResponseBody(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "<empty>";
        }
        String normalized = responseBody.replaceAll("[\\r\\n\\t]+", " ");
        return normalized.length() <= LOGGED_ERROR_BODY_MAX_LENGTH
                ? normalized
                : normalized.substring(0, LOGGED_ERROR_BODY_MAX_LENGTH) + "…";
    }

    private static ClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
