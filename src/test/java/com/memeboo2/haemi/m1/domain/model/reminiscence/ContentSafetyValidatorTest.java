package com.memeboo2.haemi.m1.domain.model.reminiscence;

import com.memeboo2.haemi.m0.domain.model.PersonContentTense;
import com.memeboo2.haemi.m0.domain.port.PersonExposurePort;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}
