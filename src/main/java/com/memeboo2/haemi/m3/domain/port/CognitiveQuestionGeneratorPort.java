package com.memeboo2.haemi.m3.domain.port;

import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m3.domain.model.training.QuestionType;
import com.memeboo2.haemi.m3.domain.model.training.TrainingQuestion;

import java.util.List;

public interface CognitiveQuestionGeneratorPort {
    default List<TrainingQuestion> generate(Album album, int difficultyLevel) {
        return generate(album, difficultyLevel, List.of(QuestionType.values()));
    }

    List<TrainingQuestion> generate(
            Album album,
            int difficultyLevel,
            List<QuestionType> prioritizedQuestionTypes
    );
}
