package com.memeboo2.haemi.m1.domain.model.reminiscence;

import com.memeboo2.haemi.m1.domain.event.ReminiscenceGeneratedEvent;
import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reminiscence_contents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReminiscenceContent extends AbstractAggregateRoot<ReminiscenceContent> {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "album_id", nullable = false)
    private UUID albumId;

    @Column(name = "generated_date", nullable = false)
    private LocalDate generatedDate;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reminiscence_slides", joinColumns = @JoinColumn(name = "content_id"))
    @OrderBy("slide_sequence ASC")
    private List<SlideCard> slideCards = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reminiscence_questions", joinColumns = @JoinColumn(name = "content_id"))
    @OrderBy("question_sequence ASC")
    private List<QuestionCard> questionCards = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reminiscence_quizzes", joinColumns = @JoinColumn(name = "content_id"))
    @OrderBy("quiz_sequence ASC")
    private List<QuizItem> quizItems = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "elder_reaction")
    private ReactionType elderReaction;

    public static ReminiscenceContent create(AlbumId albumId,
                                              List<SlideCard> slideCards,
                                              List<QuestionCard> questionCards,
                                              List<QuizItem> quizItems) {
        ReminiscenceContent content = new ReminiscenceContent();
        content.id = UUID.randomUUID();
        content.albumId = albumId.value();
        content.generatedDate = LocalDate.now();
        content.generatedAt = LocalDateTime.now();
        content.slideCards.addAll(slideCards);
        content.questionCards.addAll(questionCards);
        content.quizItems.addAll(quizItems);
        content.registerEvent(new ReminiscenceGeneratedEvent(
                content.getContentId(), albumId, content.generatedAt));
        return content;
    }

    public ReminiscenceContentId getContentId() {
        return ReminiscenceContentId.of(id.toString());
    }

    public AlbumId getAlbumId() {
        return AlbumId.of(albumId);
    }

    // 어르신 반응 기록 - AI 학습 데이터로 활용
    public void recordElderReaction(ReactionType reaction) {
        this.elderReaction = reaction;
    }

    public List<SlideCard> getSlideCards() {
        return Collections.unmodifiableList(slideCards);
    }

    public List<QuestionCard> getQuestionCards() {
        return Collections.unmodifiableList(questionCards);
    }

    public List<QuizItem> getQuizItems() {
        return Collections.unmodifiableList(quizItems);
    }
}
