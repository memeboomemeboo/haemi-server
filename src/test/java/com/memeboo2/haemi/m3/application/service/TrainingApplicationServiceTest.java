package com.memeboo2.haemi.m3.application.service;

import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import com.memeboo2.haemi.m1.domain.model.album.PhotoFile;
import com.memeboo2.haemi.m1.domain.model.album.PhotoMetadata;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import com.memeboo2.haemi.m3.application.command.AnswerTrainingQuestionCommand;
import com.memeboo2.haemi.m3.application.command.StartTrainingSessionCommand;
import com.memeboo2.haemi.m3.domain.event.DifficultyLevelChangedEvent;
import com.memeboo2.haemi.m3.domain.model.training.*;
import com.memeboo2.haemi.m3.domain.port.CognitiveQuestionGeneratorPort;
import com.memeboo2.haemi.m3.domain.port.TrainingSpeechSynthesisPort;
import com.memeboo2.haemi.m3.domain.repository.DifficultyPolicyRepository;
import com.memeboo2.haemi.m3.domain.repository.DifficultyProfileRepository;
import com.memeboo2.haemi.m3.domain.repository.TrainingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingApplicationServiceTest {

    @Mock TrainingSessionRepository sessionRepository;
    @Mock DifficultyProfileRepository profileRepository;
    @Mock DifficultyPolicyRepository policyRepository;
    @Mock AlbumRepository albumRepository;
    @Mock CognitiveQuestionGeneratorPort questionGenerator;
    @Mock TrainingSpeechSynthesisPort speechSynthesis;
    @Mock ApplicationEventPublisher eventPublisher;

    private TrainingApplicationService service;

    @BeforeEach
    void setUp() {
        service = new TrainingApplicationService(
                sessionRepository,
                profileRepository,
                policyRepository,
                albumRepository,
                questionGenerator,
                speechSynthesis,
                eventPublisher
        );
        lenient().when(sessionRepository.findByElderIdAndSessionDate(anyString(), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        lenient().when(speechSynthesis.synthesize(anyString()))
                .thenAnswer(invocation -> speech(invocation.getArgument(0)));
        lenient().when(policyRepository.findByLevel(anyInt()))
                .thenAnswer(invocation -> Optional.of(
                        DifficultyPolicy.defaultFor(invocation.getArgument(0))));
    }

    @Test
    void rejectsAlbumWhoseElderProfileDoesNotMatch() {
        Album album = albumWithPhotos("another-elder", 5);
        when(albumRepository.findById(album.getAlbumId())).thenReturn(Optional.of(album));

        assertThatThrownBy(() -> service.startSession(startCommand("elder-profile-1", album)))
                .isInstanceOf(TrainingPrerequisiteNotMetException.class)
                .hasMessageContaining("일치하지 않습니다");

        verifyNoInteractions(questionGenerator);
    }

    @Test
    void rejectsAlbumWithFewerThanFivePhotos() {
        Album album = albumWithPhotos("elder-profile-1", 4);
        when(albumRepository.findById(album.getAlbumId())).thenReturn(Optional.of(album));

        assertThatThrownBy(() -> service.startSession(startCommand("elder-profile-1", album)))
                .isInstanceOf(TrainingPrerequisiteNotMetException.class)
                .hasMessageContaining("5장 이상");

        verifyNoInteractions(questionGenerator);
    }

    @Test
    void startsPersonalizedSessionAndReturnsQuestionTtsGuide() {
        Album album = albumWithPhotos("elder-profile-1", 5);
        List<TrainingQuestion> questions = questions();
        prepareStart(album);
        when(questionGenerator.generate(eq(album), eq(2), anyList())).thenReturn(questions);

        var result = service.startSession(startCommand("elder-profile-1", album));

        assertThat(result.questions()).hasSize(3);
        assertThat(result.currentQuestion().questionId()).isEqualTo("q1");
        assertThat(result.speechGuide().text()).isEqualTo("첫 번째 질문");
        assertThat(result.speechGuide().ssml()).contains("<speak");
        assertThat(result.speechGuide().autoPlay()).isTrue();
        verify(sessionRepository).save(any(CognitiveTrainingSession.class));
    }

    @Test
    void fallsBackToLatestCompletedQuestionSetWhenGenerationFails() {
        Album album = albumWithPhotos("elder-profile-1", 5);
        CognitiveTrainingSession cached = completedSession(album, "elder-profile-1");
        prepareStart(album);
        when(questionGenerator.generate(eq(album), eq(2), anyList()))
                .thenThrow(new IllegalStateException("AI unavailable"));
        when(sessionRepository.findLatestCompleted("elder-profile-1", album.getId()))
                .thenReturn(Optional.of(cached));

        var result = service.startSession(startCommand("elder-profile-1", album));

        assertThat(result.questions()).hasSize(3);
        assertThat(result.questions())
                .allMatch(question -> question.questionId().startsWith("cached-"));
        verify(sessionRepository).findLatestCompleted("elder-profile-1", album.getId());
    }

    @Test
    void returnsServiceUnavailableErrorWhenGenerationAndCacheBothFail() {
        Album album = albumWithPhotos("elder-profile-1", 5);
        prepareStart(album);
        when(questionGenerator.generate(eq(album), eq(2), anyList()))
                .thenThrow(new IllegalStateException("AI unavailable"));
        when(sessionRepository.findLatestCompleted("elder-profile-1", album.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startSession(startCommand("elder-profile-1", album)))
                .isInstanceOf(TrainingQuestionGenerationException.class);
    }

    @Test
    void completionResponseIncludesPraiseAccuracyAndTts() {
        Album album = albumWithPhotos("elder-profile-1", 5);
        CognitiveTrainingSession session = CognitiveTrainingSession.start(
                "elder-profile-1", album.getId(), StartMode.MANUAL, 2, questions());
        session.answer("q1", "정답1", 10);
        session.answer("q2", "오답", 20);
        when(sessionRepository.findById(session.getSessionId())).thenReturn(Optional.of(session));
        when(profileRepository.findByElderId("elder-profile-1"))
                .thenReturn(Optional.of(DifficultyProfile.defaultFor("elder-profile-1")));

        var result = service.answerQuestion(
                new AnswerTrainingQuestionCommand(
                        session.getId().toString(),
                        "q3",
                        "정답3",
                        30
                )
        );

        assertThat(result.session().status()).isEqualTo(TrainingSessionStatus.COMPLETED);
        assertThat(result.message()).contains("정답률은 67퍼센트");
        assertThat(result.session().speechGuide().text()).contains("정답률은 67퍼센트");
        verify(speechSynthesis).synthesize(contains("정말 잘하셨어요"));
    }

    @Test
    void publishesDifficultyChangeWhenCompletedSessionRaisesLevel() {
        Album album = albumWithPhotos("elder-profile-1", 5);
        CognitiveTrainingSession session = CognitiveTrainingSession.start(
                "elder-profile-1", album.getId(), StartMode.MANUAL, 2, questions());
        session.answer("q1", "정답1", 10);
        session.answer("q2", "정답2", 10);
        when(sessionRepository.findById(session.getSessionId())).thenReturn(Optional.of(session));
        when(profileRepository.findByElderId("elder-profile-1"))
                .thenReturn(Optional.of(DifficultyProfile.defaultFor("elder-profile-1")));

        service.answerQuestion(new AnswerTrainingQuestionCommand(
                session.getId().toString(),
                "q3",
                "정답3",
                10
        ));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .isInstanceOfSatisfying(DifficultyLevelChangedEvent.class, changed -> {
                    assertThat(changed.previousLevel()).isEqualTo(2);
                    assertThat(changed.currentLevel()).isEqualTo(3);
                    assertThat(changed.elderId()).isEqualTo("elder-profile-1");
                });
    }

    private void prepareStart(Album album) {
        when(albumRepository.findById(album.getAlbumId())).thenReturn(Optional.of(album));
        when(profileRepository.findByElderId("elder-profile-1")).thenReturn(Optional.empty());
        when(profileRepository.save(any(DifficultyProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private StartTrainingSessionCommand startCommand(String elderId, Album album) {
        return new StartTrainingSessionCommand(elderId, album.getId().toString(), StartMode.MANUAL);
    }

    private Album albumWithPhotos(String elderProfileId, int count) {
        Album album = Album.create(elderProfileId, "group-1", "owner-1");
        for (int index = 0; index < count; index++) {
            album.addPhoto(
                    PhotoFile.of("key-" + index, "photo.jpg", "image/jpeg", 1024),
                    PhotoMetadata.of(LocalDateTime.of(1990 + index, 1, 1, 12, 0), null, null),
                    "hash-" + index,
                    "owner-1"
            );
        }
        return album;
    }

    private CognitiveTrainingSession completedSession(Album album, String elderId) {
        CognitiveTrainingSession session = CognitiveTrainingSession.start(
                elderId, album.getId(), StartMode.AUTO, 2, questions());
        questions().forEach(question ->
                session.answer(question.getQuestionId(), question.getCorrectAnswer(), 10));
        return session;
    }

    private List<TrainingQuestion> questions() {
        return List.of(
                TrainingQuestion.of("q1", QuestionType.WORD_ASSOCIATION,
                        "첫 번째 질문", "정답1", 2),
                TrainingQuestion.of("q2", QuestionType.PERSON_RECALL,
                        "두 번째 질문", "정답2", 2),
                TrainingQuestion.of("q3", QuestionType.SEQUENCE_MEMORY,
                        "세 번째 질문", "정답3", 2)
        );
    }

    private TrainingSpeech speech(String text) {
        return new TrainingSpeech(
                text,
                "<speak>" + text + "</speak>",
                "ko-KR",
                0.85
        );
    }
}
