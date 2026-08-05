package com.memeboo2.haemi.m2.domain.model.post;

import java.util.Arrays;

/**
 * 어르신 답변용 마음 이모지 6종 (F2-02). 텍스트 대신 감정을 한 번의 탭으로 전한다.
 */
public enum HeartEmoji {
    LOVE("❤️", "사랑해요"),
    HUG("🤗", "안아주고 싶어요"),
    HAPPY("😊", "행복해요"),
    MOVED("🥹", "뭉클해요"),
    THANKS("🙏", "고마워요"),
    MISS("🥰", "보고싶어요");

    private final String code;
    private final String label;

    HeartEmoji(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static boolean isValidCode(String code) {
        return code != null && Arrays.stream(values()).anyMatch(e -> e.code.equals(code));
    }

    public static HeartEmoji fromCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new InvalidHeartEmojiException(code));
    }
}
