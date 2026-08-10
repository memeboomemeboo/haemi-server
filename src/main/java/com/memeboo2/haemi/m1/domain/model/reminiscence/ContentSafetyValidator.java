package com.memeboo2.haemi.m1.domain.model.reminiscence;

import com.memeboo2.haemi.m0.domain.port.PersonExposurePort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** F1-05 S1 3중 안전 검증. 모델 프롬프트가 아니라 서버 규칙이 최종 결정권을 가진다. */
@Component
public class ContentSafetyValidator {

    private static final List<String> FORBIDDEN_TERMS = List.of(
            "문제", "퀴즈", "정답", "오답", "맞혔", "틀렸", "점수", "훈련", "치료", "회복", "랭킹", "뱃지");
    private static final List<String> QUIZ_STYLE_TERMS = List.of("누구인가요", "언제인가요", "어디인가요", "맞히", "알아맞");
    private static final List<String> PRESENT_TENSE_MARKERS = List.of(
            "지금", "요즘", "잘계세요", "잘계신가요", "잘계십니까", "잘계시는",
            "연락하", "만나", "뵈", "오시", "계시", "계세",
            "있어요", "있나요", "있으세요", "있습니다", "있죠", "살아계", "살고", "살아요", "사세요",
            "찾아가", "찾으러", "보러가");
    private static final List<String> PAST_CONTEXT_MARKERS = List.of(
            "그때", "예전", "지난", "함께", "추억", "기억", "사진", "였", "했", "던", "남긴", "찍은", "살던", "있었", "고인", "생전");

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
                .anyMatch(person -> containsUnsafeDeceasedReference(normalized, person.name(), person.nickname()));
        return deceasedPresent ? Optional.of(ContentSafetyViolation.DECEASED_PERSON_PRESENT_TENSE) : Optional.empty();
    }

    private boolean containsUnsafeDeceasedReference(String prompt, String name, String nickname) {
        return java.util.stream.Stream.of(name, nickname).filter(value -> value != null && !value.isBlank())
                .map(String::toLowerCase)
                .anyMatch(personName -> hasUnsafeOccurrence(prompt, personName));
    }

    private boolean hasUnsafeOccurrence(String prompt, String personName) {
        int index = prompt.indexOf(personName);
        while (index >= 0) {
            // 인물 이름 전후의 짧은 구문만 본다. 40자 전체 프롬프트의 다른 문장을 끌어오지 않는다.
            int from = Math.max(0, index - 4);
            int to = Math.min(prompt.length(), index + personName.length() + 10);
            String context = prompt.substring(from, to);
            // 명시적 현재형 언급은 과거 문맥 단서가 곁에 있어도 차단한다.
            // ("그때 영희는 잘 계세요?"처럼 회고 표현에 현재 안부를 섞는 문장이 통과하면 안 된다.)
            if (PRESENT_TENSE_MARKERS.stream().anyMatch(context::contains)) {
                return true;
            }
            // 현재형은 아니지만 과거 단서도 없으면 시제가 모호하다. 안전한 쪽으로 막는다.
            if (PAST_CONTEXT_MARKERS.stream().noneMatch(context::contains)) {
                return true;
            }
            index = prompt.indexOf(personName, index + personName.length());
        }
        return false;
    }
}
