package com.memeboo2.haemi.m5.domain.model.care;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class VoiceAlarmRotationTest {

    private VoiceAlarm alarm(String initialVoice) {
        return VoiceAlarm.create("elder", "group", AlarmType.REMINISCENCE,
                LocalTime.of(9, 0), initialVoice, RepeatRule.DAILY);
    }

    @Test
    @DisplayName("초기 음성 1개면 로테이션은 그 음성을 유지한다")
    void singleVoice_staysOnRotate() {
        VoiceAlarm alarm = alarm("v1");

        assertThat(alarm.voiceCount()).isEqualTo(1);
        assertThat(alarm.getVoiceKey()).isEqualTo("v1");

        alarm.rotateVoice();
        assertThat(alarm.getVoiceKey()).isEqualTo("v1");
    }

    @Test
    @DisplayName("음성을 추가하면 발송마다 순차 로테이션하고 끝에서 순환한다")
    void multipleVoices_rotateInOrderAndWrap() {
        VoiceAlarm alarm = alarm("v1");
        alarm.addVoice("v2");
        alarm.addVoice("v3");

        assertThat(alarm.voiceCount()).isEqualTo(3);
        assertThat(alarm.getVoiceKey()).isEqualTo("v1");

        alarm.rotateVoice();
        assertThat(alarm.getVoiceKey()).isEqualTo("v2");
        alarm.rotateVoice();
        assertThat(alarm.getVoiceKey()).isEqualTo("v3");
        alarm.rotateVoice(); // 순환
        assertThat(alarm.getVoiceKey()).isEqualTo("v1");
    }

    @Test
    @DisplayName("초기 음성이 없으면 TTS 대체이며 추가 음성이 현재 음성이 된다")
    void noInitialVoice_ttsThenAddedBecomesCurrent() {
        VoiceAlarm alarm = alarm(null);

        assertThat(alarm.voiceCount()).isZero();
        assertThat(alarm.usesTtsFallback()).isTrue();

        alarm.addVoice("v1");
        assertThat(alarm.usesTtsFallback()).isFalse();
        assertThat(alarm.getVoiceKey()).isEqualTo("v1");
    }

    @Test
    @DisplayName("공백 음성 추가는 무시한다")
    void addBlankVoice_ignored() {
        VoiceAlarm alarm = alarm("v1");
        alarm.addVoice("  ");

        assertThat(alarm.voiceCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("발송 직전 상태 검증: 활성 알람만 발송 가능하다")
    void isDispatchable_onlyWhenActive() {
        VoiceAlarm alarm = alarm("v1");
        assertThat(alarm.isDispatchable()).isTrue();

        alarm.deactivate();
        assertThat(alarm.isDispatchable()).isFalse();
    }
}
