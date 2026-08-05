package com.memeboo2.haemi.m2.infrastructure.event;

import com.memeboo2.haemi.m2.application.command.RecordGoalContributionCommand;
import com.memeboo2.haemi.m2.application.service.GroupGoalApplicationService;
import com.memeboo2.haemi.m2.domain.event.ElderRepliedEvent;
import com.memeboo2.haemi.m2.domain.event.MemoryPostPublishedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 추억글 게시·어르신 답변을 그룹 협력 목표 진척으로 누적한다 (개인 순위 아님).
 * 알림 전용 {@link MemoryPostEventListener}와 별개로 동작한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupGoalEventListener {

    private static final int POST_CONTRIBUTION  = 1;
    private static final int REPLY_CONTRIBUTION = 1;
    private static final String ELDER_CONTRIBUTOR = "elder";

    private final GroupGoalApplicationService groupGoalService;

    @EventListener
    public void onPostPublished(MemoryPostPublishedEvent event) {
        groupGoalService.recordContribution(new RecordGoalContributionCommand(
                event.albumId(), POST_CONTRIBUTION, event.authorInfo().getMemberId()));
    }

    @EventListener
    public void onElderReplied(ElderRepliedEvent event) {
        groupGoalService.recordContribution(new RecordGoalContributionCommand(
                event.albumId(), REPLY_CONTRIBUTION, ELDER_CONTRIBUTOR));
    }
}
