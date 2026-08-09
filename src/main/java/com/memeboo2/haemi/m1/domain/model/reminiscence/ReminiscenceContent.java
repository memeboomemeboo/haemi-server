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

    @Enumerated(EnumType.STRING)
    @Column(name = "elder_reaction")
    private ReactionType elderReaction;

    public static ReminiscenceContent create(AlbumId albumId, List<SlideCard> slideCards) {
        ReminiscenceContent content = new ReminiscenceContent();
        content.id = UUID.randomUUID();
        content.albumId = albumId.value();
        content.generatedDate = LocalDate.now();
        content.generatedAt = LocalDateTime.now();
        content.slideCards.addAll(slideCards);
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

}
