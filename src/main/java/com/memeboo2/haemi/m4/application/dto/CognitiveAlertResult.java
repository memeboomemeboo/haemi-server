package com.memeboo2.haemi.m4.application.dto;

import com.memeboo2.haemi.m4.domain.model.dashboard.AlertType;
import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveChangeAlert;

import java.time.LocalDateTime;

public record CognitiveAlertResult(
        String alertId,
        String elderId,
        String albumId,
        AlertType alertType,
        String message,
        String guideLink,
        LocalDateTime sentAt
) {
    public static CognitiveAlertResult from(CognitiveChangeAlert alert) {
        return new CognitiveAlertResult(
                alert.getId().toString(),
                alert.getElderId(),
                alert.getAlbumId() != null ? alert.getAlbumId().toString() : null,
                alert.getAlertType(),
                alert.getMessage(),
                alert.getGuideLink(),
                alert.getSentAt()
        );
    }
}
