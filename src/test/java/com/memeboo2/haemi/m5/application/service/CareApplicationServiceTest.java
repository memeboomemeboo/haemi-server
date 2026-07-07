package com.memeboo2.haemi.m5.application.service;

import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m1.domain.port.PhotoStoragePort;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import com.memeboo2.haemi.m5.domain.model.care.*;
import com.memeboo2.haemi.m5.domain.port.WeatherPort;
import com.memeboo2.haemi.m5.domain.repository.VoiceAlarmRepository;
import com.memeboo2.haemi.m5.domain.repository.WalkRecordRepository;
import com.memeboo2.haemi.m5.domain.repository.WalkRoutineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CareApplicationServiceTest {

    @Mock VoiceAlarmRepository voiceAlarmRepository;
    @Mock WalkRoutineRepository walkRoutineRepository;
    @Mock WalkRecordRepository walkRecordRepository;
    @Mock PhotoStoragePort storagePort;
    @Mock WeatherPort weatherPort;
    @Mock NotificationPort notificationPort;
    @Mock AlbumRepository albumRepository;

    CareApplicationService service;

    @BeforeEach
    void setUp() {
        service = new CareApplicationService(
                voiceAlarmRepository, walkRoutineRepository, walkRecordRepository,
                storagePort, weatherPort, notificationPort, albumRepository);
    }

    @Test
    @DisplayName("예약 시각에 어르신에게 알리고 10분 무응답이면 실제 가족 구성원에게 한 번 알린다")
    void processDueReminders_sendsAlarmAndNoResponseToAlbumMembers() {
        LocalDateTime due = LocalDateTime.of(2026, 7, 6, 9, 0);
        VoiceAlarm alarm = VoiceAlarm.create(
                "elder-1", "group-1", AlarmType.MEDICATION,
                LocalTime.of(9, 0), "voice-key", RepeatRule.DAILY);
        Album album = Album.create("elder-1", "group-1", "family-1");
        album.inviteMember("family-2");
        album.acceptInvite("family-2");
        when(voiceAlarmRepository.findAllActive()).thenReturn(List.of(alarm));
        when(walkRoutineRepository.findAllActive()).thenReturn(List.of());
        when(albumRepository.findByGroupId("group-1")).thenReturn(Optional.of(album));

        service.processDueReminders(due);
        service.processDueReminders(due.plusMinutes(10));
        service.processDueReminders(due.plusMinutes(11));

        verify(notificationPort).sendToMember(
                "elder-1", "약 드실 시간이에요", "가족의 목소리 알람이 도착했어요.");
        verify(notificationPort, times(1)).sendToGroup(
                album.getMemberIds(), "알람 무응답",
                "어르신이 알람을 10분 동안 확인하지 않았습니다.");
    }

    @Test
    @DisplayName("산책 예약 시각의 날씨가 좋지 않으면 어르신에게 실내 활동을 안내한다")
    void processDueReminders_usesIndoorFallbackForSevereWeather() {
        LocalDateTime due = LocalDateTime.of(2026, 7, 6, 9, 0);
        WalkRoutine routine = WalkRoutine.create(
                "elder-1", "group-1", LocalTime.of(9, 0), null, 10);
        when(voiceAlarmRepository.findAllActive()).thenReturn(List.of());
        when(walkRoutineRepository.findAllActive()).thenReturn(List.of(routine));
        when(weatherPort.currentCondition("elder-1")).thenReturn(WeatherCondition.RAIN);

        service.processDueReminders(due);
        service.processDueReminders(due.plusSeconds(30));

        verify(notificationPort, times(1)).sendToMember(
                "elder-1", "오늘은 실내 운동을 해봐요",
                "날씨가 좋지 않아 안전한 실내 활동을 안내합니다.");
    }
}
