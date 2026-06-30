package com.memeboo2.haemi.m1.domain.port;

import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.Photo;
import com.memeboo2.haemi.m1.domain.model.reminiscence.QuestionCard;
import com.memeboo2.haemi.m1.domain.model.reminiscence.QuizItem;
import com.memeboo2.haemi.m1.domain.model.reminiscence.SlideCard;

import java.util.List;

public interface ReminiscenceGeneratorPort {

    // 슬라이드형 사진 회상 카드 생성 (3~5장)
    List<SlideCard> generateSlideCards(Album album, List<Photo> candidatePhotos);

    // 회상 유도 질문 카드 생성 (2~3개)
    List<QuestionCard> generateQuestionCards(Album album, List<Photo> candidatePhotos);

    // 인물·장소 퀴즈 생성 (1~2개)
    List<QuizItem> generateQuizItems(Album album, List<Photo> candidatePhotos);
}
