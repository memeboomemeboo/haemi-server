package com.memeboo2.haemi.m4.domain.model.dashboard;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportTrendPoint {

    @Column(name = "metric_date", nullable = false)
    private LocalDate date;

    @Column(name = "accuracy_rate", nullable = false)
    private double accuracyRate;

    public ReportTrendPoint(LocalDate date, double accuracyRate) {
        this.date = date;
        this.accuracyRate = Math.max(0.0, Math.min(1.0, accuracyRate));
    }
}
