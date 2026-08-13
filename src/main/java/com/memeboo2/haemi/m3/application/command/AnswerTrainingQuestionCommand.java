package com.memeboo2.haemi.m3.application.command;

public record AnswerTrainingQuestionCommand(
        String sessionId,
        String questionId,
        boolean voiceDetected,
        int vadDurationMs
) {
    /** 기존 내부 호출 호환용. 답변 원문은 VAD 감지 여부로만 변환하고 보관하지 않는다. */
    @Deprecated
    public AnswerTrainingQuestionCommand(String sessionId, String questionId,
                                         String ignoredSubmittedAnswer, int responseSeconds) {
        this(sessionId, questionId, ignoredSubmittedAnswer != null && !ignoredSubmittedAnswer.isBlank(),
                Math.max(responseSeconds, 0) * 1_000);
    }
}
