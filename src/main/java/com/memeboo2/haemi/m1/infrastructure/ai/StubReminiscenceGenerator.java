package com.memeboo2.haemi.m1.infrastructure.ai;

import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.Photo;
import com.memeboo2.haemi.m1.domain.model.reminiscence.QuestionCard;
import com.memeboo2.haemi.m1.domain.model.reminiscence.QuizItem;
import com.memeboo2.haemi.m1.domain.model.reminiscence.SlideCard;
import com.memeboo2.haemi.m1.domain.port.ReminiscenceGeneratorPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 실 서비스에서는 Claude API 등 AI 서비스로 교체한다.
 */
@Slf4j
@Component
public class StubReminiscenceGenerator implements ReminiscenceGeneratorPort {

    @Override
    public List<SlideCard> generateSlideCards(Album album, List<Photo> candidatePhotos) {
        log.info("[AI-STUB] 슬라이드 카드 생성: albumId={}, candidates={}", album.getAlbumId(), candidatePhotos.size());
        List<SlideCard> cards = new ArrayList<>();
        List<Photo> selected = candidatePhotos.stream().limit(5).toList();
        for (int i = 0; i < selected.size(); i++) {
            Photo photo = selected.get(i);
            String caption = buildCaption(photo);
            cards.add(SlideCard.of(photo.getId(), i + 1, caption));
        }
        return cards;
    }

    @Override
    public List<QuestionCard> generateQuestionCards(Album album, List<Photo> candidatePhotos) {
        log.info("[AI-STUB] 질문 카드 생성: albumId={}", album.getAlbumId());
        List<QuestionCard> cards = new ArrayList<>();
        candidatePhotos.stream().limit(3).forEach(photo -> {
            String period = photo.getMetadata().getTimePeriod() != null
                    ? photo.getMetadata().getTimePeriod() : "그때";
            String question = period + " 사진입니다. 당시 어떤 기억이 나시나요?";
            cards.add(QuestionCard.of(question, cards.size() + 1));
        });
        return cards;
    }

    @Override
    public List<QuizItem> generateQuizItems(Album album, List<Photo> candidatePhotos) {
        log.info("[AI-STUB] 퀴즈 생성: albumId={}", album.getAlbumId());
        List<QuizItem> items = new ArrayList<>();
        candidatePhotos.stream()
                .filter(p -> !p.getPersonTags().isEmpty())
                .limit(2)
                .forEach(photo -> {
                    String question = "이 사진에 함께 계신 분은 누구인가요?";
                    String answer = photo.getPersonTags().get(0).getMemberName();
                    items.add(QuizItem.of(photo.getId(), question, answer, items.size() + 1));
                });
        return items;
    }

    private String buildCaption(Photo photo) {
        StringBuilder sb = new StringBuilder();
        if (photo.getMetadata().getTimePeriod() != null) {
            sb.append(photo.getMetadata().getTimePeriod()).append(" ");
        }
        if (photo.getMetadata().getLocationText() != null) {
            sb.append(photo.getMetadata().getLocationText()).append("에서 ");
        }
        if (photo.getMetadata().getMemo() != null) {
            sb.append(photo.getMetadata().getMemo());
        }
        return sb.isEmpty() ? "소중한 추억" : sb.toString().trim();
    }
}
