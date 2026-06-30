package com.memeboo2.haemi.m1.application.service;

import com.memeboo2.haemi.m1.application.command.RecordReactionCommand;
import com.memeboo2.haemi.m1.application.dto.ReminiscenceResult;
import com.memeboo2.haemi.m1.application.query.GetTodayReminiscenceQuery;
import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import com.memeboo2.haemi.m1.domain.model.album.AnalysisStatus;
import com.memeboo2.haemi.m1.domain.model.album.Photo;
import com.memeboo2.haemi.m1.domain.model.reminiscence.*;
import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m1.domain.port.ReminiscenceGeneratorPort;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import com.memeboo2.haemi.m1.domain.repository.ReminiscenceContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminiscenceApplicationService {

    private final AlbumRepository albumRepository;
    private final ReminiscenceContentRepository reminiscenceContentRepository;
    private final ReminiscenceGeneratorPort generatorPort;
    private final NotificationPort notificationPort;

    @Value("${haemi.album.min-photos-for-ai:5}")
    private int minPhotosForAi;

    @Value("${haemi.ai.rotation-days:7}")
    private int rotationDays;

    // F1-05: 오늘의 회상 콘텐츠 생성 (스케줄러에서 호출)
    @Transactional
    public Optional<ReminiscenceResult> generateTodayReminiscence(String albumId) {
        Album album = loadAlbumOrThrow(albumId);

        if (!album.hasEnoughPhotosForAi(minPhotosForAi)) {
            log.info("AI 생성 최소 조건 미충족: albumId={}, required={}", albumId, minPhotosForAi);
            return Optional.empty();
        }

        // 이미 오늘 생성된 콘텐츠가 있으면 재사용
        Optional<ReminiscenceContent> existing = reminiscenceContentRepository
                .findByAlbumIdAndDate(AlbumId.of(albumId), LocalDate.now());
        if (existing.isPresent()) {
            return Optional.of(ReminiscenceResult.from(existing.get()));
        }

        // 최근 N일 노출된 사진 제외 (로테이션)
        Set<UUID> recentlyUsed = reminiscenceContentRepository
                .findRecentlyUsedPhotoIds(AlbumId.of(albumId), rotationDays);

        List<Photo> candidates = album.getPhotos().stream()
                .filter(p -> p.getAnalysisStatus() == AnalysisStatus.COMPLETED)
                .filter(p -> !recentlyUsed.contains(p.getId()))
                .toList();

        // 로테이션 후 후보가 부족하면 전체에서 선택
        if (candidates.size() < minPhotosForAi) {
            candidates = album.getPhotos().stream()
                    .filter(p -> p.getAnalysisStatus() == AnalysisStatus.COMPLETED)
                    .toList();
        }

        List<SlideCard> slides = generatorPort.generateSlideCards(album, candidates);
        List<QuestionCard> questions = generatorPort.generateQuestionCards(album, candidates);
        List<QuizItem> quizzes = generatorPort.generateQuizItems(album, candidates);

        ReminiscenceContent content = ReminiscenceContent.create(
                AlbumId.of(albumId), slides, questions, quizzes);
        reminiscenceContentRepository.save(content);

        notificationPort.sendToGroup(album.getMemberIds(),
                "오늘의 회상",
                "오늘의 회상 콘텐츠가 준비되었습니다.");

        log.info("회상 콘텐츠 생성 완료: contentId={}, albumId={}", content.getContentId(), albumId);
        return Optional.of(ReminiscenceResult.from(content));
    }

    // F1-05: 오늘의 회상 조회
    @Transactional(readOnly = true)
    public Optional<ReminiscenceResult> getTodayReminiscence(GetTodayReminiscenceQuery query) {
        return reminiscenceContentRepository
                .findByAlbumIdAndDate(AlbumId.of(query.albumId()), LocalDate.now())
                .map(ReminiscenceResult::from);
    }

    // F1-05: 어르신 반응 기록
    @Transactional
    public void recordReaction(RecordReactionCommand command) {
        ReminiscenceContent content = reminiscenceContentRepository
                .findById(ReminiscenceContentId.of(command.contentId()))
                .orElseThrow(() -> new ReminiscenceContentNotFoundException(command.contentId()));
        content.recordElderReaction(command.reaction());
        reminiscenceContentRepository.save(content);
        log.info("반응 기록: contentId={}, reaction={}", command.contentId(), command.reaction());
    }

    private Album loadAlbumOrThrow(String albumId) {
        return albumRepository.findById(AlbumId.of(albumId))
                .orElseThrow(() -> new AlbumNotFoundException(albumId));
    }
}
