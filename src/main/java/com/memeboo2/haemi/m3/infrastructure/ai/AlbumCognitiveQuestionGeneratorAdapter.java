package com.memeboo2.haemi.m3.infrastructure.ai;

import com.memeboo2.haemi.common.exception.DomainValidationException;

import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.Photo;
import com.memeboo2.haemi.m1.domain.model.album.PhotoMetadata;
import com.memeboo2.haemi.m3.domain.model.training.QuestionType;
import com.memeboo2.haemi.m3.domain.model.training.TrainingQuestion;
import com.memeboo2.haemi.m3.domain.port.CognitiveQuestionGeneratorPort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class AlbumCognitiveQuestionGeneratorAdapter implements CognitiveQuestionGeneratorPort {

    @Override
    public List<TrainingQuestion> generate(
            Album album,
            int difficultyLevel,
            List<QuestionType> prioritizedQuestionTypes
    ) {
        List<Photo> photos = album.getPhotos();
        if (photos.isEmpty()) {
            throw new DomainValidationException("인지 훈련 문제 생성에 사용할 사진이 없습니다.");
        }

        int normalizedLevel = Math.max(1, Math.min(5, difficultyLevel));
        int questionCount = normalizedLevel <= 2 ? 3 : normalizedLevel <= 4 ? 4 : 5;
        List<QuestionType> types = prioritizedQuestionTypes == null || prioritizedQuestionTypes.isEmpty()
                ? List.of(QuestionType.values())
                : List.copyOf(new java.util.LinkedHashSet<>(prioritizedQuestionTypes));
        int typeOffset = Math.floorMod(
                album.getId().hashCode() + LocalDate.now().getDayOfYear() + normalizedLevel,
                types.size()
        );
        List<TrainingQuestion> questions = new ArrayList<>(questionCount);
        for (int index = 0; index < questionCount; index++) {
            QuestionType type = index == 0
                    ? types.getFirst()
                    : nextDifferentType(types, typeOffset + index - 1, questions.getLast().getType());
            Photo photo = photos.get(index % photos.size());
            questions.add(createQuestion(type, photo, normalizedLevel));
        }
        return questions;
    }

    private QuestionType nextDifferentType(
            List<QuestionType> types,
            int candidateIndex,
            QuestionType previousType
    ) {
        for (int offset = 0; offset < types.size(); offset++) {
            QuestionType candidate = types.get(Math.floorMod(candidateIndex + offset, types.size()));
            if (candidate != previousType) {
                return candidate;
            }
        }
        throw new DomainValidationException("연속 중복 방지를 위해 문제 유형이 두 개 이상 필요합니다.");
    }

    private TrainingQuestion createQuestion(QuestionType type, Photo photo, int difficultyLevel) {
        String questionId = "album-" + UUID.randomUUID();
        PhotoMetadata metadata = photo.getMetadata();
        String location = metadata != null && hasText(metadata.getLocationText())
                ? metadata.getLocationText()
                : "집";

        return switch (type) {
            case PERSON_RECALL -> TrainingQuestion.withPhoto(
                    questionId,
                    type,
                    "사진 속 함께한 분은 누구인지 편하게 이야기해 주세요.",
                    difficultyLevel,
                    photo.getId()
            );
            case PLACE_MATCH -> TrainingQuestion.withPhoto(
                    questionId,
                    type,
                    "이 사진은 어디에서 찍은 걸까요? %s에서의 추억을 떠올려 이야기해 주세요.".formatted(location),
                    difficultyLevel,
                    photo.getId()
            );
            case COLOR_SHAPE -> TrainingQuestion.of(
                    questionId,
                    type,
                    "사진 속에서 기억에 남는 색이나 모양이 있다면 이야기해 주세요.",
                    difficultyLevel
            );
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
