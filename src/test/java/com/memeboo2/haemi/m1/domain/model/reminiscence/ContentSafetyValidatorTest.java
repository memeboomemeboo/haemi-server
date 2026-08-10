package com.memeboo2.haemi.m1.domain.model.reminiscence;

import com.memeboo2.haemi.m0.domain.model.PersonContentTense;
import com.memeboo2.haemi.m0.domain.port.PersonExposurePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ContentSafetyValidatorTest {

    private final ContentSafetyValidator validator = new ContentSafetyValidator();
    private final List<PersonExposurePort.PhotoPersonExposure> deceased = List.of(
            new PersonExposurePort.PhotoPersonExposure(java.util.UUID.randomUUID(), "영희", null,
                    PersonContentTense.PAST_ONLY, true));

    @Test
    void blocksPresentTenseReferenceToDeceasedPerson() {
        assertThat(validator.validate("영희는 지금 잘 계시죠?", deceased, List.of()))
                .contains(ContentSafetyViolation.DECEASED_PERSON_PRESENT_TENSE);
    }

    @Test
    void blocksAmbiguousOrPresentReferencesToDeceasedPersonWhileAllowingClearPastContext() {
        assertThat(validator.validate("영희는 여기 있어요?", deceased, List.of()))
                .contains(ContentSafetyViolation.DECEASED_PERSON_PRESENT_TENSE);
        assertThat(validator.validate("영희는 살아계시나요?", deceased, List.of()))
                .contains(ContentSafetyViolation.DECEASED_PERSON_PRESENT_TENSE);
        assertThat(validator.validate("영희는 잘 계세요?", deceased, List.of()))
                .contains(ContentSafetyViolation.DECEASED_PERSON_PRESENT_TENSE);
        assertThat(validator.validate("영희를 찾아가고 싶으세요?", deceased, List.of()))
                .contains(ContentSafetyViolation.DECEASED_PERSON_PRESENT_TENSE);
        assertThat(validator.validate("지금 영희를 보러 가고 싶으세요?", deceased, List.of()))
                .contains(ContentSafetyViolation.DECEASED_PERSON_PRESENT_TENSE);
        assertThat(validator.validate("영희와 함께 남긴 오래된 사진이에요. 지금 영희는 어디 계세요?", deceased, List.of()))
                .contains(ContentSafetyViolation.DECEASED_PERSON_PRESENT_TENSE);
        assertThat(validator.validate("영희와 함께 남긴 사진이에요, 이야기를 들려주실래요?", deceased, List.of()))
                .isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("clearPastPrompts")
    void clearPastContextDoesNotSuppressSafeReminiscenceCards(String prompt) {
        assertThat(validator.validate(prompt, deceased, List.of())).isEmpty();
    }

    @Test
    void blocksSensitiveTopicAndForbiddenQuizVocabulary() {
        assertThat(validator.validate("사별한 배우자 이야기를 들려주세요?", List.of(), List.of("사별한 배우자")))
                .contains(ContentSafetyViolation.SENSITIVE_TOPIC);
        assertThat(validator.validate("이 사진 퀴즈를 풀어보실래요?", List.of(), List.of()))
                .contains(ContentSafetyViolation.FORBIDDEN_TERM);
    }

    @Test
    void acceptsSupportiveStoryPrompt() {
        assertThat(validator.validate("1978년 여름 사진이에요, 이야기 들려주실래요?", deceased, List.of()))
                .isEmpty();
    }

    private static Stream<String> clearPastPrompts() {
        return Stream.of(
                "영희와 함께 있었던 그때 사진이에요?",
                "영희가 살던 고향집 기억나세요?",
                "영희와 만났던 날 이야기예요?",
                "영희가 남긴 편지 기억나세요?",
                "영희와 찍은 봄 사진이에요?",
                "영희와 보냈던 여름날이에요?",
                "영희가 좋아했던 노래예요?",
                "영희와 여행했던 곳이에요?",
                "영희가 일하던 가게 앞이에요?",
                "영희와 살던 집 사진이에요?",
                "영희가 해주셨던 음식이에요?",
                "영희와 웃었던 순간이에요?",
                "영희가 들려줬던 이야기예요?",
                "영희와 연락했던 시절이에요?",
                "영희가 찾아왔던 날이에요?",
                "영희와 함께한 추억이에요?",
                "영희가 계셨던 자리 사진이에요?",
                "영희와 나눴던 인사예요?",
                "영희가 보러 왔던 전시예요?",
                "영희와 지냈던 겨울이에요?"
        );
    }
}
