package com.memeboo2.haemi.m1.domain.model.reminiscence;

import com.memeboo2.haemi.m0.domain.port.PersonExposurePort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** F1-05 S1 3중 안전 검증. 모델 프롬프트가 아니라 서버 규칙이 최종 결정권을 가진다. */
@Component
public class ContentSafetyValidator {

    private static final List<String> FORBIDDEN_TERMS = List.of(
            "문제", "퀴즈", "정답", "오답", "맞혔", "틀렸", "점수", "훈련", "치료", "회복", "랭킹", "뱃지");
    private static final List<String> QUIZ_STYLE_TERMS = List.of("누구인가요", "언제인가요", "어디인가요", "맞히", "알아맞");
    private static final List<String> PRESENT_TENSE_MARKERS = List.of("지금", "요즘", "잘 계", "연락", "만나", "뵈", "오시", "계시");

    public Optional<ContentSafetyViolation> validate(String prompt, List<PersonExposurePort.PhotoPersonExposure> persons,
                                                     List<String> sensitiveTopics) {
        if (prompt == null || prompt.isBlank() || prompt.length() > 40 || !prompt.endsWith("?")) {
            return Optional.of(ContentSafetyViolation.INVALID_FORMAT);
        }
        String normalized = prompt.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        if (FORBIDDEN_TERMS.stream().anyMatch(term -> normalized.contains(term))) {
            return Optional.of(ContentSafetyViolation.FORBIDDEN_TERM);
        }
        if (QUIZ_STYLE_TERMS.stream().anyMatch(term -> normalized.contains(term))) {
            return Optional.of(ContentSafetyViolation.QUIZ_STYLE_PROMPT);
        }
        if (sensitiveTopics.stream().filter(topic -> topic != null && !topic.isBlank())
                .anyMatch(topic -> normalized.contains(topic.replaceAll("\\s+", "").toLowerCase(Locale.ROOT)))) {
            return Optional.of(ContentSafetyViolation.SENSITIVE_TOPIC);
        }
        boolean deceasedPresent = persons.stream()
                .filter(person -> person.tense().name().equals("PAST_ONLY") && person.nameUsable())
                .anyMatch(person -> containsPresentTenseReference(normalized, person.name(), person.nickname()));
        return deceasedPresent ? Optional.of(ContentSafetyViolation.DECEASED_PERSON_PRESENT_TENSE) : Optional.empty();
    }

    private boolean containsPresentTenseReference(String prompt, String name, String nickname) {
        return java.util.stream.Stream.of(name, nickname).filter(value -> value != null && !value.isBlank())
                .map(String::toLowerCase)
                .anyMatch(personName -> PRESENT_TENSE_MARKERS.stream()
                        .anyMatch(marker -> Pattern.compile(Pattern.quote(personName) + ".{0,12}" + marker)
                                .matcher(prompt).find()));
    }
}
