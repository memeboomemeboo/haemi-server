package com.memeboo2.haemi.m2.domain.model.post;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HeartEmojiTest {

    @Test
    @DisplayName("마음 이모지는 6종이며 코드가 서로 다르다")
    void hasSixDistinctEmojis() {
        assertThat(HeartEmoji.values()).hasSize(6);
        long distinctCodes = java.util.Arrays.stream(HeartEmoji.values())
                .map(HeartEmoji::getCode).distinct().count();
        assertThat(distinctCodes).isEqualTo(6);
    }

    @Test
    @DisplayName("등록된 코드만 유효로 판정하고 조회할 수 있다")
    void validatesAndResolvesCode() {
        assertThat(HeartEmoji.isValidCode("❤️")).isTrue();
        assertThat(HeartEmoji.isValidCode("🐶")).isFalse();
        assertThat(HeartEmoji.isValidCode(null)).isFalse();
        assertThat(HeartEmoji.fromCode("🙏")).isEqualTo(HeartEmoji.THANKS);
    }

    @Test
    @DisplayName("미지원 코드는 예외를 던진다")
    void rejectsUnknownCode() {
        assertThatThrownBy(() -> HeartEmoji.fromCode("🐶"))
                .isInstanceOf(InvalidHeartEmojiException.class);
    }
}
