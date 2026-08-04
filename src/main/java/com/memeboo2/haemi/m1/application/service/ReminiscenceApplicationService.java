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
import com.memeboo2.haemi.m1.domain.port.ReminiscenceCardGeneratorPort;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import com.memeboo2.haemi.m1.domain.repository.ReminiscenceContentRepository;
import com.memeboo2.haemi.m0.domain.port.ElderContentContextPort;
import com.memeboo2.haemi.m0.domain.port.PersonExposurePort;
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
    private final ReminiscenceCardGeneratorPort generatorPort;
    private final NotificationPort notificationPort;
    private final ElderContentContextPort elderContexts;
    private final PersonExposurePort personExposures;
    private final ContentSafetyValidator safetyValidator;

    @Value("${haemi.album.min-photos-for-ai:20}")
    private int minPhotosForAi;

    @Value("${haemi.ai.rotation-days:7}")
    private int rotationDays;

    // F1-05: 오늘의 회상 콘텐츠 생성 (스케줄러에서 호출)
    @Transactional
    public Optional<ReminiscenceResult> generateTodayReminiscence(String albumId) {
        Album album = loadAlbumOrThrow(albumId);

        java.util.UUID elderId;
        try {
            elderId = java.util.UUID.fromString(album.getElderProfileId());
        } catch (IllegalArgumentException e) {
            // v3 M0 프로필과 연결되지 않은 v2 앨범은 안전하게 생성 대상에서 제외한다.
            return Optional.empty();
        }
        ElderContentContextPort.ElderContentContext elderContext = elderContexts.getContentContext(elderId);
        if (elderContext.completeness() < 0.3) {
            return Optional.empty();
        }

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
                .filter(p -> personExposures.findByPhotoId(p.getId()).stream()
                        .allMatch(PersonExposurePort.PhotoPersonExposure::isPhotoEligible))
                .toList();

        // 로테이션 후 후보가 부족하면 전체에서 선택
        if (candidates.size() < minPhotosForAi) {
            candidates = album.getPhotos().stream()
                    .filter(p -> p.getAnalysisStatus() == AnalysisStatus.COMPLETED)
                    .filter(p -> personExposures.findByPhotoId(p.getId()).stream()
                            .allMatch(PersonExposurePort.PhotoPersonExposure::isPhotoEligible))
                    .toList();
        }

        List<SlideCard> cards = new java.util.ArrayList<>();
        for (Photo photo : candidates.stream().limit(elderContext.personalizationLevel() <= 2 ? 3 : 5).toList()) {
            List<PersonExposurePort.PhotoPersonExposure> people = personExposures.findByPhotoId(photo.getId());
            String factualContext = buildFactualContext(photo);
            String personContext = people.stream().filter(PersonExposurePort.PhotoPersonExposure::nameUsable)
                    .map(person -> person.name() + "(" + person.tense() + ")").collect(java.util.stream.Collectors.joining(", "));
            for (int attempt = 0; attempt < 3; attempt++) {
                Optional<GeneratedCard> generated = generatorPort.generate(new ReminiscenceCardGeneratorPort.CardGenerationRequest(
                        photo.getId(), factualContext, personContext, elderContext.personalizationLevel(), elderContext.sensitiveTopics()));
                if (generated.isEmpty()) continue;
                if (safetyValidator.validate(generated.get().promptText(), people, elderContext.sensitiveTopics()).isEmpty()) {
                    cards.add(SlideCard.of(generated.get(), cards.size() + 1));
                    break;
                }
            }
        }
        if (cards.size() < 3) return Optional.empty();

        ReminiscenceContent content = ReminiscenceContent.create(AlbumId.of(albumId), cards);
        reminiscenceContentRepository.save(content);

        notificationPort.sendToGroup(album.getAllMemberIds(),
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

    private String buildFactualContext(Photo photo) {
        if (photo.getMetadata().getMemo() != null && !photo.getMetadata().getMemo().isBlank()) {
            return photo.getMetadata().getMemo();
        }
        if (photo.getMetadata().getLocationText() != null && !photo.getMetadata().getLocationText().isBlank()) {
            return photo.getMetadata().getLocationText() + "에서 찍은 사진";
        }
        if (photo.getMetadata().getTimePeriod() != null && !photo.getMetadata().getTimePeriod().isBlank()) {
            return photo.getMetadata().getTimePeriod() + " 사진";
        }
        return "함께 남긴 사진";
    }
}
