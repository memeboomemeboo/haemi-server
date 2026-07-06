package com.memeboo2.haemi.m5.domain.model.care;

import java.time.DayOfWeek;

public enum RepeatRule {
    DAILY,
    WEEKDAYS,
    WEEKENDS,
    CUSTOM_DAYS;

    public boolean appliesTo(DayOfWeek dayOfWeek) {
        return switch (this) {
            case DAILY -> true;
            case WEEKDAYS -> dayOfWeek != DayOfWeek.SATURDAY
                    && dayOfWeek != DayOfWeek.SUNDAY;
            case WEEKENDS -> dayOfWeek == DayOfWeek.SATURDAY
                    || dayOfWeek == DayOfWeek.SUNDAY;
            case CUSTOM_DAYS -> false;
        };
    }
}
