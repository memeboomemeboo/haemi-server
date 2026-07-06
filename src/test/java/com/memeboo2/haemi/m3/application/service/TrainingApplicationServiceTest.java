package com.memeboo2.haemi.m3.application.service;

import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import com.memeboo2.haemi.m3.application.command.RequestGrandchildChanceCommand;
import com.memeboo2.haemi.m3.application.dto.ChanceResult;
import com.memeboo2.haemi.m3.domain.model.training.*;
import com.memeboo2.haemi.m3.domain.port.CognitiveQuestionGeneratorPort;
import com.memeboo2.haemi.m3.domain.repository.DifficultyProfileRepository;
import com.memeboo2.haemi.m3.domain.repository.TrainingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingApplicationServiceTest {

    @Mock
    TrainingSessionRepository sessionRepository;

    @Mock
    DifficultyProfileRepository profileRepository;

    @Mock
    CognitiveQuestionGeneratorPort questionGeneratorPort;

    @Mock
    AlbumRepository albumRepository;

    TrainingApplicationService service;

    @BeforeEach
    void setUp() {
        service = new TrainingApplicationService(
                sessionRepository,
                profileRepository,
                questionGeneratorPort,
                albumRepository
        );
    }

    @Test
    @DisplayName("손주 찬스 요청은 앨범의 실제 가족 구성원을 알림 대상으로 사용한다")
    void requestGrandchildChance_usesAlbumMembersAsRecipients() {
        CognitiveTrainingSession session = session();
        Album album = Album.create("elder-1", "group-1", "family-1");
        album.inviteMember("family-2");
        when(sessionRepository.findById(session.getSessionId())).thenReturn(Optional.of(session));
        when(albumRepository.findById(AlbumId.of(session.getAlbumId()))).thenReturn(Optional.of(album));
        when(sessionRepository.save(any(CognitiveTrainingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChanceResult result = service.requestGrandchildChance(
                new RequestGrandchildChanceCommand(session.getId().toString(), "elder-1"));

        assertThat(result.remainingChanceCount()).isEqualTo(1);
        assertThat(session.getLastChanceStatus()).isEqualTo(GrandchildChanceStatus.PENDING);
        verify(sessionRepository).save(session);
    }

    @Test
    @DisplayName("앨범에 알림을 받을 가족 구성원이 없으면 손주 찬스 요청을 저장하지 않는다")
    @SuppressWarnings("unchecked")
    void requestGrandchildChance_rejectsAlbumWithoutMembers() {
        CognitiveTrainingSession session = session();
        Album album = Album.create("elder-1", "group-1", "family-1");
        ((LinkedHashSet<String>) ReflectionTestUtils.getField(album, "memberIds")).clear();
        when(sessionRepository.findById(session.getSessionId())).thenReturn(Optional.of(session));
        when(albumRepository.findById(AlbumId.of(session.getAlbumId()))).thenReturn(Optional.of(album));

        assertThatThrownBy(() -> service.requestGrandchildChance(
                new RequestGrandchildChanceCommand(session.getId().toString(), "elder-1")))
                .isInstanceOf(GrandchildChanceUnavailableException.class);

        verify(sessionRepository, never()).save(any());
    }

    private CognitiveTrainingSession session() {
        return CognitiveTrainingSession.start(
                "elder-1",
                UUID.randomUUID(),
                StartMode.AUTO,
                2,
                List.of(
                        question("q1", QuestionType.WORD_ASSOCIATION, "a"),
                        question("q2", QuestionType.PERSON_RECALL, "b"),
                        question("q3", QuestionType.SEQUENCE_MEMORY, "c")
                )
        );
    }

    private TrainingQuestion question(String id, QuestionType type, String answer) {
        return TrainingQuestion.of(id, type, "문제", answer, 2);
    }
}
