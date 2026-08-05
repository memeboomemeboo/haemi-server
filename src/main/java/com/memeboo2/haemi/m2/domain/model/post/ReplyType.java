package com.memeboo2.haemi.m2.domain.model.post;

/**
 * 어르신 답변 유형 (F2-02). 음성 우선, 마음 이모지 보조. 텍스트 직접 입력은 제공하지 않는다.
 */
public enum ReplyType {
    VOICE,   // 음성 — STT 전사 텍스트로 저장 (1순위)
    EMOJI    // 마음 이모지 6종 중 하나
}
