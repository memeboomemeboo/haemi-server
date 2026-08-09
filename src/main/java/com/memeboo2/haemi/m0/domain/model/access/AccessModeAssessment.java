package com.memeboo2.haemi.m0.domain.model.access;

import com.memeboo2.haemi.m0.domain.model.ElderAccessMode;
import com.memeboo2.haemi.m0.domain.model.M0ValidationException;

import java.util.List;

/**
 * 접근 모드 진단 (F0-03). 5문항 점수(각 0~2)를 합산해 Mode A(자립)/B(보조)를 추천한다.
 * 합계 10점 중 임계값 이상이면 자립(A), 미만이면 보조(B).
 */
public final class AccessModeAssessment {

    public static final int QUESTION_COUNT = 5;
    public static final int MAX_SCORE_PER_QUESTION = 2;
    // 자립(A) 추천 임계값: 10점 만점 중 6점 이상
    public static final int SELF_DIRECTED_THRESHOLD = 6;

    private AccessModeAssessment() {
    }

    public static ElderAccessMode recommend(List<Integer> answers) {
        validate(answers);
        int total = answers.stream().mapToInt(Integer::intValue).sum();
        return total >= SELF_DIRECTED_THRESHOLD ? ElderAccessMode.A : ElderAccessMode.B;
    }

    private static void validate(List<Integer> answers) {
        if (answers == null || answers.size() != QUESTION_COUNT) {
            throw new M0ValidationException("진단은 " + QUESTION_COUNT + "문항이어야 해요.");
        }
        for (Integer answer : answers) {
            if (answer == null || answer < 0 || answer > MAX_SCORE_PER_QUESTION) {
                throw new M0ValidationException("각 문항 점수는 0~" + MAX_SCORE_PER_QUESTION + "점이어야 해요.");
            }
        }
    }
}
