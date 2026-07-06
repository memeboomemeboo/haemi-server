package com.memeboo2.haemi.m5.domain.model.care;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CareDomainTest {

    @Test
    @DisplayName("음성 파일이 없으면 TTS 대체 음성을 사용한다")
    void voiceAlarm_usesTtsFallbackWithoutVoiceKey() {
        VoiceAlarm blankVoice = alarm(" ");
        VoiceAlarm recordedVoice = alarm("voice-key");

        assertThat(blankVoice.usesTtsFallback()).isTrue();
        assertThat(recordedVoice.usesTtsFallback()).isFalse();
    }

    @Test
    @DisplayName("알람 발생과 확인 시각을 기록하고 비활성화할 수 있다")
    void voiceAlarm_tracksLifecycle() {
        VoiceAlarm alarm = alarm("voice-key");

        alarm.markTriggered();
        alarm.acknowledge();
        alarm.deactivate();

        assertThat(alarm.getLastTriggeredAt()).isNotNull();
        assertThat(alarm.getLastAcknowledgedAt()).isNotNull();
        assertThat(alarm.isActive()).isFalse();
    }

    @Test
    @DisplayName("산책 목표 시간이 0 이하이면 기본 10분을 적용한다")
    void walkRoutine_appliesDefaultTargetMinutes() {
        WalkRoutine routine = WalkRoutine.create(
                "elder", "group", LocalTime.of(9, 0), LocalTime.of(17, 0), 0);

        assertThat(routine.getTargetMinutes()).isEqualTo(10);
        assertThat(routine.isActive()).isTrue();
    }

    @Test
    @DisplayName("악천후에는 산책을 시작하지 않고 날씨 취소 상태가 된다")
    void walkRecord_cancelsOnSevereWeather() {
        WalkRoutine routine = routine();

        assertThat(WalkRecord.start(routine, WeatherCondition.RAIN).getStatus())
                .isEqualTo(WalkStatus.CANCELLED_BY_WEATHER);
        assertThat(WalkRecord.start(routine, WeatherCondition.HEAVY_SNOW).getStatus())
                .isEqualTo(WalkStatus.CANCELLED_BY_WEATHER);
        assertThat(WalkRecord.start(routine, WeatherCondition.HEAT_WAVE).getStatus())
                .isEqualTo(WalkStatus.CANCELLED_BY_WEATHER);
        assertThat(WalkRecord.start(routine, WeatherCondition.CLEAR).getStatus())
                .isEqualTo(WalkStatus.STARTED);
    }

    @Test
    @DisplayName("산책 완료 시 음수 기록을 0으로 보정한다")
    void walkRecord_completeClampsNegativeValues() {
        WalkRecord record = WalkRecord.start(routine(), WeatherCondition.CLEAR);

        record.complete(-5, -100);

        assertThat(record.getStatus()).isEqualTo(WalkStatus.COMPLETED);
        assertThat(record.getDurationMinutes()).isZero();
        assertThat(record.getStepCount()).isZero();
        assertThat(record.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("산책 기록은 루틴의 식별 정보를 복사한다")
    void walkRecord_copiesRoutineIdentity() {
        WalkRoutine routine = routine();

        WalkRecord record = WalkRecord.start(routine, WeatherCondition.CLEAR);

        assertThat(record.getRoutineId()).isEqualTo(routine.getId());
        assertThat(record.getElderId()).isEqualTo(routine.getElderId());
        assertThat(record.getGroupId()).isEqualTo(routine.getGroupId());
    }

    private VoiceAlarm alarm(String voiceKey) {
        return VoiceAlarm.create(
                "elder", "group", AlarmType.MEDICATION,
                LocalTime.of(9, 0), voiceKey, RepeatRule.DAILY);
    }

    private WalkRoutine routine() {
        WalkRoutine routine = WalkRoutine.create(
                "elder", "group", LocalTime.of(9, 0), null, 30);
        assertThat(routine.getId()).isInstanceOf(UUID.class);
        return routine;
    }
}
