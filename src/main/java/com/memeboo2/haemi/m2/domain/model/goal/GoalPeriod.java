package com.memeboo2.haemi.m2.domain.model.goal;

import java.time.LocalDate;

/**
 * 그룹 협력 목표 집계 주기. 개인 순위 개념이 없는 협력형 목표의 기간 단위.
 */
public enum GoalPeriod {

    WEEKLY {
        @Override
        public LocalDate startOf(LocalDate date) {
            // ISO 기준 주 시작(월요일)
            return date.minusDays(date.getDayOfWeek().getValue() - 1L);
        }

        @Override
        public LocalDate endOf(LocalDate date) {
            return startOf(date).plusDays(6);
        }
    },

    MONTHLY {
        @Override
        public LocalDate startOf(LocalDate date) {
            return date.withDayOfMonth(1);
        }

        @Override
        public LocalDate endOf(LocalDate date) {
            return date.withDayOfMonth(date.lengthOfMonth());
        }
    };

    public abstract LocalDate startOf(LocalDate date);

    public abstract LocalDate endOf(LocalDate date);
}
