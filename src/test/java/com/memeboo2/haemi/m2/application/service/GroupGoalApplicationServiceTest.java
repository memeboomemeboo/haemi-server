package com.memeboo2.haemi.m2.application.service;

import com.memeboo2.haemi.m2.application.command.RecordGoalContributionCommand;
import com.memeboo2.haemi.m2.application.dto.GroupGoalResult;
import com.memeboo2.haemi.m2.application.dto.HighlightCardResult;
import com.memeboo2.haemi.m2.application.query.GetCurrentGoalQuery;
import com.memeboo2.haemi.m2.application.query.GetHighlightCardQuery;
import com.memeboo2.haemi.m2.domain.model.goal.GoalPeriod;
import com.memeboo2.haemi.m2.domain.model.goal.GroupGoal;
import com.memeboo2.haemi.m2.domain.model.post.AuthorInfo;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPost;
import com.memeboo2.haemi.m2.domain.model.post.ReplyType;
import com.memeboo2.haemi.m2.domain.repository.GroupGoalRepository;
import com.memeboo2.haemi.m2.domain.repository.MemoryPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupGoalApplicationServiceTest {

    @Mock GroupGoalRepository goalRepository;
    @Mock MemoryPostRepository postRepository;

    GroupGoalApplicationService service;

    private final UUID albumId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new GroupGoalApplicationService(goalRepository, postRepository);
        ReflectionTestUtils.setField(service, "weeklyTarget", 20);
    }

    @Test
    @DisplayName("활성 목표가 없으면 주간 목표를 자동 개시해 조회한다")
    void getCurrentGoal_autoStartsWhenNoneActive() {
        when(goalRepository.findActiveByAlbumId(eq(albumId), any())).thenReturn(Optional.empty());
        when(goalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GroupGoalResult result = service.getCurrentGoal(new GetCurrentGoalQuery(albumId.toString()));

        assertThat(result.targetCount()).isEqualTo(20);
        assertThat(result.currentProgress()).isZero();
        assertThat(result.period()).isEqualTo("WEEKLY");
        verify(goalRepository).save(any(GroupGoal.class));
    }

    @Test
    @DisplayName("진척 기록은 활성 목표에 누적되고 저장된다")
    void recordContribution_accumulatesOnActiveGoal() {
        GroupGoal goal = GroupGoal.start(albumId, GoalPeriod.WEEKLY, LocalDate.now(), 20);
        when(goalRepository.findActiveByAlbumId(eq(albumId), any())).thenReturn(Optional.of(goal));
        when(goalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordContribution(new RecordGoalContributionCommand(albumId, 1, "member-1"));

        ArgumentCaptor<GroupGoal> captor = ArgumentCaptor.forClass(GroupGoal.class);
        verify(goalRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentProgress()).isEqualTo(1);
        assertThat(captor.getValue().participantCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("0 이하 진척 명령은 저장을 유발하지 않는다")
    void recordContribution_ignoresNonPositive() {
        service.recordContribution(new RecordGoalContributionCommand(albumId, 0, "member-1"));
        verifyNoInteractions(goalRepository);
    }

    @Test
    @DisplayName("하이라이트 카드는 그룹 전체 집계와 가장 사랑받은 추억을 담는다")
    void getHighlightCard_aggregatesGroupStats() {
        GroupGoal goal = GroupGoal.start(albumId, GoalPeriod.WEEKLY, LocalDate.now(), 20);
        goal.recordProgress(3, "member-1");
        when(goalRepository.findActiveByAlbumId(eq(albumId), any())).thenReturn(Optional.of(goal));

        MemoryPost loved = publishedPost("member-1", "홍길동", "딸", "가장 좋아요 많은 추억");
        loved.toggleLike("m2");
        loved.toggleLike("m3");
        MemoryPost replied = publishedPost("member-2", "김철수", "아들", "어르신 답변 있는 추억");
        replied.submitElderReply(ReplyType.SHORT_TEXT, "고맙다");
        replied.toggleLike("m4");

        when(postRepository.findPublishedByAlbumIdAndPeriod(eq(albumId), any(), any()))
                .thenReturn(List.of(loved, replied));

        HighlightCardResult card = service.getHighlightCard(new GetHighlightCardQuery(albumId.toString()));

        assertThat(card.totalPosts()).isEqualTo(2);
        assertThat(card.elderReplyCount()).isEqualTo(1);
        assertThat(card.totalLikes()).isEqualTo(3);
        assertThat(card.participantCount()).isEqualTo(1);
        assertThat(card.topMemory()).isNotNull();
        assertThat(card.topMemory().likeCount()).isEqualTo(2);
        assertThat(card.topMemory().authorName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("활성 목표가 없어도 하이라이트 카드는 이번 주 기준으로 집계한다")
    void getHighlightCard_worksWithoutActiveGoal() {
        when(goalRepository.findActiveByAlbumId(eq(albumId), any())).thenReturn(Optional.empty());
        when(postRepository.findPublishedByAlbumIdAndPeriod(eq(albumId), any(), any()))
                .thenReturn(List.of());

        HighlightCardResult card = service.getHighlightCard(new GetHighlightCardQuery(albumId.toString()));

        assertThat(card.goalAchieved()).isFalse();
        assertThat(card.totalPosts()).isZero();
        assertThat(card.topMemory()).isNull();
        assertThat(card.targetCount()).isEqualTo(20);
    }

    private MemoryPost publishedPost(String memberId, String name, String relation, String text) {
        MemoryPost post = MemoryPost.createDraft(
                albumId, AuthorInfo.of(memberId, name, relation), text, null, null);
        post.publish();
        return post;
    }
}
