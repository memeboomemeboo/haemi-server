package com.memeboo2.haemi.m1.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memeboo2.haemi.m1.domain.model.reminiscence.CardType;
import com.memeboo2.haemi.m1.domain.model.reminiscence.GeneratedCard;
import com.memeboo2.haemi.m1.domain.port.ReminiscenceCardGeneratorPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import java.util.Map;
import java.util.Optional;

/** Gemini 호출 실패나 미설정 시에도 안전한 결정론적 카드로 서비스가 중단되지 않게 한다. */
@Component
public class GeminiReminiscenceCardGenerator implements ReminiscenceCardGeneratorPort {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiReminiscenceCardGenerator(@Value("${haemi.ai.gemini.api-key:}") String apiKey,
                                           @Value("${haemi.ai.gemini.model:gemini-3.6-flash}") String model,
                                           @Value("${haemi.ai.gemini.connect-timeout:5s}") Duration connectTimeout,
                                           @Value("${haemi.ai.gemini.read-timeout:20s}") Duration readTimeout) {
        // 타임아웃이 없으면 업스트림이 멈췄을 때 08:00 배치가 DB 트랜잭션을 연 채 영원히 대기한다.
        // 실패는 아래 catch가 안전한 기본 카드로 받아 주므로, 필요한 건 상한뿐이다. (#98)
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .requestFactory(requestFactory(connectTimeout, readTimeout))
                .build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.model = model;
    }

    private static ClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }

    @Override
    public Optional<GeneratedCard> generate(CardGenerationRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.of(fallback(request));
        }
        try {
            String instruction = """
                    사진 회상 카드 한 장을 JSON으로 작성하세요. 형식은 {\"cardType\":\"STORY_CARD|PERSON_CARD|PLACE_CARD\",\"promptText\":\"...\"} 입니다.
                    사실 정보를 먼저 말하고, 이야기를 청하는 한 문장 존댓말로 작성하세요. 30자 이내, 끝은 물음표여야 합니다.
                    사람을 시험하는 질문(누구/언제/어디인가요), 퀴즈·정답·점수·훈련 표현, 사별 인물 현재형, 금기 주제는 절대 쓰지 마세요.
                    사진 정보: %s
                    인물 정보: %s
                    금기 주제: %s
                    개인화 레벨: %d
                    """.formatted(request.factualContext(), request.personContext(), request.sensitiveTopics(),
                    request.personalizationLevel());
            Map<String, Object> body = Map.of(
                    "contents", new Object[]{Map.of("parts", new Object[]{Map.of("text", instruction)})},
                    "generationConfig", Map.of("temperature", 0.2, "responseMimeType", "application/json"));
            String response = restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey).build(model))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(response);
            String json = root.at("/candidates/0/content/parts/0/text").asText();
            JsonNode generated = objectMapper.readTree(json);
            return Optional.of(new GeneratedCard(request.photoId(),
                    CardType.valueOf(generated.path("cardType").asText("STORY_CARD")),
                    generated.path("promptText").asText()));
        } catch (Exception ignored) {
            return Optional.of(fallback(request));
        }
    }

    private GeneratedCard fallback(CardGenerationRequest request) {
        String fact = request.factualContext() == null || request.factualContext().isBlank()
                ? "함께 남긴 사진이에요" : request.factualContext();
        String trimmed = fact.length() > 18 ? fact.substring(0, 18) : fact;
        return new GeneratedCard(request.photoId(), CardType.STORY_CARD, trimmed + " 이야기 들려주실래요?");
    }
}
