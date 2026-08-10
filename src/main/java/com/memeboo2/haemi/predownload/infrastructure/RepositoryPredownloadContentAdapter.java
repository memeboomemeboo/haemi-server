package com.memeboo2.haemi.predownload.infrastructure;

import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import com.memeboo2.haemi.m3.domain.model.training.CognitiveTrainingSession;
import com.memeboo2.haemi.m3.domain.repository.AccruedHintRepository;
import com.memeboo2.haemi.m3.domain.repository.TrainingSessionRepository;
import com.memeboo2.haemi.predownload.domain.PredownloadBundle;
import com.memeboo2.haemi.predownload.domain.port.PredownloadContentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 08:00에 생성된 오늘의 훈련 세션(카드·사진)과 적립 힌트를 어르신별로 조립한다.
 */
@Component
@RequiredArgsConstructor
public class RepositoryPredownloadContentAdapter implements PredownloadContentPort {

    private final AlbumRepository albumRepository;
    private final TrainingSessionRepository trainingSessionRepository;
    private final AccruedHintRepository accruedHintRepository;

    @Override
    @Transactional(readOnly = true)
    public List<String> eligibleElderIds(LocalDate date) {
        return albumRepository.findDistinctElderProfileIds();
    }

    @Override
    @Transactional(readOnly = true)
    public PredownloadBundle assemble(String elderId, LocalDate date) {
        List<String> cardKeys = new ArrayList<>();
        Set<UUID> photoIds = new LinkedHashSet<>();
        Set<String> hintKeys = new LinkedHashSet<>();

        trainingSessionRepository.findByElderIdAndSessionDate(elderId, date)
                .ifPresent(session -> collectCardsAndPhotos(session, cardKeys, photoIds));

        // 세션 사진별 힌트(L1) + 일반 최신 힌트(L2)를 선다운로드 대상에 포함
        photoIds.forEach(photoId ->
                accruedHintRepository.findLatestActiveByPhoto(elderId, photoId)
                        .ifPresent(hint -> hintKeys.add(hint.getId().toString())));
        accruedHintRepository.findLatestActiveGeneral(elderId)
                .ifPresent(hint -> hintKeys.add(hint.getId().toString()));

        List<String> photoKeys = photoIds.stream().map(UUID::toString).toList();
        return PredownloadBundle.of(elderId, date, cardKeys, photoKeys, new ArrayList<>(hintKeys));
    }

    private void collectCardsAndPhotos(CognitiveTrainingSession session,
                                       List<String> cardKeys, Set<UUID> photoIds) {
        session.getQuestions().forEach(question -> {
            cardKeys.add(question.getQuestionId());
            if (question.getPhotoId() != null) {
                photoIds.add(question.getPhotoId());
            }
        });
    }
}
