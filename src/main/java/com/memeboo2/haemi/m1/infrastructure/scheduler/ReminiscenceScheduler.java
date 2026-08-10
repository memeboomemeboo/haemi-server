package com.memeboo2.haemi.m1.infrastructure.scheduler;

import com.memeboo2.haemi.m1.application.service.ReminiscenceApplicationService;
import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminiscenceScheduler {

    private final AlbumRepository albumRepository;
    private final ReminiscenceApplicationService reminiscenceService;

    // F1-05: 매일 오전 08:00 자동 실행
    @Scheduled(cron = "${haemi.reminiscence.cron:0 0 8 * * *}",
            zone = "${haemi.reminiscence.time-zone:Asia/Seoul}")
    public void generateDailyReminiscence() {
        log.info("=== 오늘의 회상 일괄 생성 시작 ===");
        List<Album> albums = albumRepository.findAll();
        log.info("대상 앨범 수: {}", albums.size());

        int success = 0, skipped = 0;
        for (Album album : albums) {
            try {
                var result = reminiscenceService.generateTodayReminiscence(album.getId().toString());
                if (result.isPresent()) success++;
                else skipped++;
            } catch (Exception e) {
                log.error("회상 생성 실패: albumId={}", album.getId(), e);
            }
        }
        log.info("=== 오늘의 회상 생성 완료: success={}, skipped={} ===", success, skipped);
    }
}
