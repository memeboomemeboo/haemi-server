package com.memeboo2.haemi.m3.infrastructure.ai;

import com.memeboo2.haemi.m3.domain.model.training.QuestionType;
import com.memeboo2.haemi.m3.domain.model.training.TrainingQuestion;
import com.memeboo2.haemi.m3.domain.port.CognitiveQuestionGeneratorPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class StubCognitiveQuestionGeneratorAdapter implements CognitiveQuestionGeneratorPort {

    @Override
    public List<TrainingQuestion> generate(String elderId, UUID albumId, int difficultyLevel) {
        return List.of(
                TrainingQuestion.of("q-" + UUID.randomUUID(), QuestionType.FAMILY_PHOTO_PUZZLE,
                        "이 사진 조각은 가족 앨범의 어느 장면일까요?", "가족", difficultyLevel),
                TrainingQuestion.of("q-" + UUID.randomUUID(), QuestionType.WORD_ASSOCIATION,
                        "봄과 어울리는 단어를 골라주세요: 꽃 / 눈 / 얼음", "꽃", difficultyLevel),
                TrainingQuestion.of("q-" + UUID.randomUUID(), QuestionType.SEQUENCE_MEMORY,
                        "방금 들은 순서를 기억해 주세요: 사과-버스-모자. 두 번째 단어는?", "버스", difficultyLevel),
                TrainingQuestion.of("q-" + UUID.randomUUID(), QuestionType.PERSON_RECALL,
                        "가족 사진에서 가장 자주 함께 등장한 사람은 누구일까요?", "가족", difficultyLevel),
                TrainingQuestion.of("q-" + UUID.randomUUID(), QuestionType.PLACE_MATCH,
                        "이 추억과 어울리는 장소를 골라주세요: 집 / 시장 / 학교", "집", difficultyLevel)
        );
    }
}
