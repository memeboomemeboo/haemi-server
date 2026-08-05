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

import com.memeboo2.haemi.m0.domain.port.ElderStatusQuery;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
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
    @Mock ElderStatusQuery elderStatusQuery;

    private static final String ELDER_ID = UUID.randomUUID().toString();

    CareApplicationService service;

    @BeforeEach
    void setUp() {
        service = new CareApplicationService(
                voiceAlarmRepository, walkRoutineRepository, walkRecordRepository,
                storagePort, weatherPort, notificationPort, albumRepository, elderStatusQuery);
    }

    @Test
    @DisplayName("예약 시각에 어르신에게 알리고 10분 무응답이면 실제 가족 구성원에게 한 번 알린다")
    void processDueReminders_sendsAlarmAndNoResponseToAlbumMembers() {
        LocalDateTime due = LocalDateTime.of(2026, 7, 6, 9, 0);
        VoiceAlarm alarm = VoiceAlarm.create(
                ELDER_ID, "group-1", AlarmType.MEDICATION,
                LocalTime.of(9, 0), "voice-key", RepeatRule.DAILY);
        Album album = Album.create("elder-1", "group-1", "family-1");
        album.inviteMember("family-2");
        album.acceptInvite("family-2");
        when(voiceAlarmRepository.findAllActive()).thenReturn(List.of(alarm));
        when(albumRepository.findByGroupId("group-1")).thenReturn(Optional.of(album));
        when(elderStatusQuery.isDispatchable(anyString())).thenReturn(true);

        service.processDueReminders(due);
        service.processDueReminders(due.plusMinutes(10));
        service.processDueReminders(due.plusMinutes(11));

        verify(notificationPort).sendToMember(
                ELDER_ID, "약 드실 시간이에요", "가족의 목소리 알람이 도착했어요.");
        verify(notificationPort, times(1)).sendToGroup(
                album.getMemberIds(), "알람 무응답",
                "어르신이 알람을 10분 동안 확인하지 않았습니다.");
    }

    @Test
    @DisplayName("초대 수락 전 PENDING 구성원은 돌봄 알림 대상에서 제외된다")
    void processDueReminders_excludesPendingInvitee() {
        LocalDateTime due = LocalDateTime.of(2026, 7, 6, 9, 0);
        VoiceAlarm alarm = VoiceAlarm.create(
                ELDER_ID, "group-1", AlarmType.MEDICATION,
                LocalTime.of(9, 0), "voice-key", RepeatRule.DAILY);
        Album album = Album.create("elder-1", "group-1", "family-1");
        album.inviteMember("family-2"); // 아직 수락 전(PENDING)
        when(voiceAlarmRepository.findAllActive()).thenReturn(List.of(alarm));
        when(albumRepository.findByGroupId("group-1")).thenReturn(Optional.of(album));
        when(elderStatusQuery.isDispatchable(anyString())).thenReturn(true);

        service.processDueReminders(due);
        service.processDueReminders(due.plusMinutes(10));
        service.processDueReminders(due.plusMinutes(11));

        verify(notificationPort, times(1)).sendToGroup(
                Set.of("family-1"), "알람 무응답",
                "어르신이 알람을 10분 동안 확인하지 않았습니다.");
    }

    // F5-02 산책 알림 보류(#47): 스케줄러 산책 처리 및 관련 검증은 제거됨.
    // 산책 서비스 로직(createWalkRoutine/startWalk/completeWalk)은 보존되어 별도로 검증된다.
}
