package com.memeboo2.haemi.m4.infrastructure.event;

import com.memeboo2.haemi.m3.domain.event.TrainingSessionCompletedEvent;
import com.memeboo2.haemi.m4.application.service.DashboardApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrainingCompletionMetricListener {

    private final DashboardApplicationService dashboardService;

    @EventListener
    public void onTrainingCompleted(TrainingSessionCompletedEvent event) {
        dashboardService.recordTrainingCompletion(
                event.elderId(), event.albumId(), event.sessionDate(),
                event.accuracyRate(), event.averageResponseSeconds());
    }
}
