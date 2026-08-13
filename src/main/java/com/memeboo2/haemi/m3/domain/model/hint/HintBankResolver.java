package com.memeboo2.haemi.m3.domain.model.hint;

import java.util.Optional;

/**
 * F3-03 즉시 재생 리졸버: L1(사진 특정) → L3(시스템 기본).
 * L2는 60초 동안 온라인 가족의 응답을 기다리는 별도 실시간 요청 경로다.
 * 비활성(active=false) 힌트는 저장소 조회 단계에서 제외되어 여기로 넘어오지 않는다.
 */
public final class HintBankResolver {

    public static final String SYSTEM_RESPONDER = "해미";
    public static final String DEFAULT_HINT_TEXT = "천천히 사진을 보면서 떠오르는 것을 편하게 이야기해 주세요.";

    private HintBankResolver() {
    }

    public static ResolvedHint resolve(Optional<AccruedHint> l1Specific) {
        return l1Specific.map(HintBankResolver::fromAccrued)
                .orElseGet(() -> new ResolvedHint(HintTier.L3, DEFAULT_HINT_TEXT, SYSTEM_RESPONDER));
    }

    private static ResolvedHint fromAccrued(AccruedHint hint) {
        return new ResolvedHint(hint.tier(), hint.getText(), hint.getAuthorName());
    }
}
