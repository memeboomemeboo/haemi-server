package com.memeboo2.haemi.m1.application.dto;

import com.memeboo2.haemi.m1.domain.model.reminiscence.ReactionType;
import com.memeboo2.haemi.m1.domain.model.reminiscence.ReminiscenceContent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReminiscenceResult(
        String contentId,
        String albumId,
        LocalDate generatedDate,
        LocalDateTime generatedAt,
        List<SlideCardResult> slideCards,
        List<QuestionCardResult> questionCards,
        List<QuizItemResult> quizItems,
        ReactionType elderReaction
) {
    public record SlideCardResult(UUID photoId, int sequence, String caption) {}
    public record QuestionCardResult(String questionText, int sequence) {}
    public record QuizItemResult(UUID photoId, String quizText, String answer, int sequence) {}

    public static ReminiscenceResult from(ReminiscenceContent content) {
        List<SlideCardResult> slides = content.getSlideCards().stream()
                .map(s -> new SlideCardResult(s.getPhotoId(), s.getSequence(), s.getCaption()))
                .toList();
        List<QuestionCardResult> questions = content.getQuestionCards().stream()
                .map(q -> new QuestionCardResult(q.getQuestionText(), q.getSequence()))
                .toList();
        List<QuizItemResult> quizzes = content.getQuizItems().stream()
                .map(q -> new QuizItemResult(q.getPhotoId(), q.getQuizText(), q.getAnswer(), q.getSequence()))
                .toList();
        return new ReminiscenceResult(
                content.getId().toString(),
                content.getAlbumId().toString(),
                content.getGeneratedDate(),
                content.getGeneratedAt(),
                slides,
                questions,
                quizzes,
                content.getElderReaction()
        );
    }
}
