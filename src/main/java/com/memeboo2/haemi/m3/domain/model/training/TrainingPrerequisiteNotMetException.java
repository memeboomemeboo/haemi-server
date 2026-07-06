package com.memeboo2.haemi.m3.domain.model.training;

public class TrainingPrerequisiteNotMetException extends RuntimeException {

    private TrainingPrerequisiteNotMetException(String message) {
        super(message);
    }

    public static TrainingPrerequisiteNotMetException profileMismatch(
            String elderId,
            String elderProfileId
    ) {
        return new TrainingPrerequisiteNotMetException(
                "요청한 어르신과 앨범의 어르신 프로필이 일치하지 않습니다. elderId=%s, elderProfileId=%s"
                        .formatted(elderId, elderProfileId)
        );
    }

    public static TrainingPrerequisiteNotMetException insufficientPhotos(int current, int required) {
        return new TrainingPrerequisiteNotMetException(
                "인지 훈련을 시작하려면 기억 앨범 사진이 %d장 이상 필요합니다. current=%d"
                        .formatted(required, current)
        );
    }
}
