package com.memeboo2.haemi.m3.infrastructure.ai;

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
            throw new IllegalArgumentException("인지 훈련 문제 생성에 사용할 사진이 없습니다.");
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
        throw new IllegalArgumentException("연속 중복 방지를 위해 문제 유형이 두 개 이상 필요합니다.");
    }

    private TrainingQuestion createQuestion(QuestionType type, Photo photo, int difficultyLevel) {
        String questionId = "album-" + UUID.randomUUID();
        String person = photo.getPersonTags().stream()
                .map(tag -> tag.getMemberName())
                .filter(this::hasText)
                .findFirst()
                .orElse("가족");
        PhotoMetadata metadata = photo.getMetadata();
        String location = metadata != null && hasText(metadata.getLocationText())
                ? metadata.getLocationText()
                : "집";
        String timePeriod = resolveTimePeriod(metadata);

        return switch (type) {
            case FAMILY_PHOTO_PUZZLE -> TrainingQuestion.withPhoto(
                    questionId,
                    type,
                    "가족사진을 보고 이 사진이 언제의 추억인지 말씀해 주세요.",
                    timePeriod,
                    difficultyLevel,
                    photo.getId()
            );
            case WORD_ASSOCIATION -> TrainingQuestion.of(
                    questionId,
                    type,
                    "%s의 추억과 가장 잘 어울리는 단어를 골라주세요: 가족 / 날씨 / 숫자".formatted(timePeriod),
                    "가족",
                    difficultyLevel
            );
            case SEQUENCE_MEMORY -> TrainingQuestion.of(
                    questionId,
                    type,
                    "다음 순서를 기억해 주세요: 사진, 가족, 미소. 두 번째 단어는 무엇인가요?",
                    "가족",
                    difficultyLevel
            );
            case PERSON_RECALL -> TrainingQuestion.withPhoto(
                    questionId,
                    type,
                    "사진 속 함께한 분의 이름은 무엇인가요?",
                    person,
                    difficultyLevel,
                    photo.getId()
            );
            case PLACE_MATCH -> TrainingQuestion.withPhoto(
                    questionId,
                    type,
                    "사진과 관련된 장소를 골라주세요: %s / 병원 / 공항".formatted(location),
                    location,
                    difficultyLevel,
                    photo.getId()
            );
            case COLOR_SHAPE -> TrainingQuestion.of(
                    questionId,
                    type,
                    "같은 모양을 찾아주세요: 동그라미 / 세모 / 네모. 공처럼 둥근 모양은 무엇인가요?",
                    "동그라미",
                    difficultyLevel
            );
        };
    }

    private String resolveTimePeriod(PhotoMetadata metadata) {
        if (metadata == null) {
            return "그때";
        }
        if (hasText(metadata.getTimePeriod())) {
            return metadata.getTimePeriod();
        }
        if (metadata.getShotAt() != null) {
            return metadata.getShotAt().getYear() + "년";
        }
        return "그때";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
