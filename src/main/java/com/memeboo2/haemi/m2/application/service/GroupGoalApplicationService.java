package com.memeboo2.haemi.m2.application.service;

import com.memeboo2.haemi.common.support.DomainIds;

import com.memeboo2.haemi.m2.application.command.RecordGoalContributionCommand;
import com.memeboo2.haemi.m2.application.dto.GroupGoalResult;
import com.memeboo2.haemi.m2.application.dto.HighlightCardResult;
import com.memeboo2.haemi.m2.application.query.GetCurrentGoalQuery;
import com.memeboo2.haemi.m2.application.query.GetHighlightCardQuery;
import com.memeboo2.haemi.m2.domain.model.goal.GoalPeriod;
import com.memeboo2.haemi.m2.domain.model.goal.GroupGoal;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPost;
import com.memeboo2.haemi.m2.domain.repository.GroupGoalRepository;
import com.memeboo2.haemi.m2.domain.repository.MemoryPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 그룹 협력 목표 (F1-03-A). 개인 순위·뱃지·스트릭 없이 가족 전체가 공동 목표를 채우고,
 * 기간 하이라이트 카드로 함께 이룬 성취를 축하한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupGoalApplicationService {

    private final GroupGoalRepository goalRepository;
    private final MemoryPostRepository postRepository;

    // 활성 목표가 없을 때 지연 자동 생성되는 주간 목표 기본치
    @Value("${haemi.group-goal.weekly-target:20}")
    private int weeklyTarget;

    // 진행 중 그룹 협력 목표 조회 (없으면 주간 목표 자동 개시)
    @Transactional
    public GroupGoalResult getCurrentGoal(GetCurrentGoalQuery query) {
        UUID albumId = DomainIds.parseUuid(query.albumId(), "앨범 ID");
        GroupGoal goal = loadOrStartActiveGoal(albumId);
        return GroupGoalResult.from(goal);
    }

    // 협력 진척 기록 (이벤트 리스너에서 호출). 활성 목표 없으면 주간 목표 자동 개시 후 누적.
    @Transactional
    public void recordContribution(RecordGoalContributionCommand command) {
        if (command.amount() <= 0) return;
        GroupGoal goal = loadOrStartActiveGoal(command.albumId());
        goal.recordProgress(command.amount(), command.contributorId());
        goalRepository.save(goal);
        log.info("그룹 협력 목표 진척: albumId={}, progress={}/{}, achieved={}",
                command.albumId(), goal.getCurrentProgress(), goal.getTargetCount(), goal.isAchieved());
    }

    // 기간 하이라이트 카드
    @Transactional(readOnly = true)
    public HighlightCardResult getHighlightCard(GetHighlightCardQuery query) {
        UUID albumId = DomainIds.parseUuid(query.albumId(), "앨범 ID");
        LocalDate today = LocalDate.now();

        GroupGoal goal = goalRepository.findActiveByAlbumId(albumId, today).orElse(null);
        GoalPeriod period      = goal != null ? goal.getPeriod()      : GoalPeriod.WEEKLY;
        LocalDate periodStart  = goal != null ? goal.getPeriodStart() : period.startOf(today);
        LocalDate periodEnd    = goal != null ? goal.getPeriodEnd()   : period.endOf(today);

        List<MemoryPost> posts = postRepository.findPublishedByAlbumIdAndPeriod(
                albumId, periodStart.atStartOfDay(), periodEnd.atTime(LocalTime.MAX));

        int totalPosts      = posts.size();
        int elderReplyCount = (int) posts.stream().filter(MemoryPost::hasElderReply).count();
        int totalLikes      = posts.stream().mapToInt(MemoryPost::getLikeCount).sum();

        HighlightCardResult.TopMemory topMemory = posts.stream()
                .filter(p -> p.getLikeCount() > 0)
                .max(Comparator.comparingInt(MemoryPost::getLikeCount)
                        .thenComparing(MemoryPost::getPublishedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toTopMemory)
                .orElse(null);

        return new HighlightCardResult(
                albumId, period.name(), periodStart, periodEnd,
                goal != null && goal.isAchieved(),
                goal != null ? goal.getTargetCount()     : weeklyTarget,
                goal != null ? goal.getCurrentProgress() : totalPosts,
                goal != null ? goal.participantCount()   : 0,
                totalPosts, elderReplyCount, totalLikes, topMemory);
    }

    private GroupGoal loadOrStartActiveGoal(UUID albumId) {
        LocalDate today = LocalDate.now();
        return goalRepository.findActiveByAlbumId(albumId, today)
                .orElseGet(() -> {
                    GroupGoal started = GroupGoal.start(albumId, GoalPeriod.WEEKLY, today, weeklyTarget);
                    log.info("주간 그룹 협력 목표 자동 개시: albumId={}, target={}", albumId, weeklyTarget);
                    return goalRepository.save(started);
                });
    }

    private HighlightCardResult.TopMemory toTopMemory(MemoryPost post) {
        return new HighlightCardResult.TopMemory(
                post.getPostId().value(),
                post.getAuthorInfo().getMemberName(),
                post.getAuthorInfo().getRelation(),
                post.getLikeCount(),
                buildPreview(post));
    }

    private String buildPreview(MemoryPost post) {
        if (post.getTextContent() != null && !post.getTextContent().isBlank()) {
            String text = post.getTextContent();
            return text.length() > 50 ? text.substring(0, 50) + "…" : text;
        }
        if (!post.getPhotoKeys().isEmpty()) return "사진을 보내셨어요 📸";
        if (post.getVoiceMemoKey() != null) return "음성 메시지를 보내셨어요 🎵";
        return "추억글";
    }
}
