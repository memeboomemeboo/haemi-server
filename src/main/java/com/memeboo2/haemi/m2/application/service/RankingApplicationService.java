package com.memeboo2.haemi.m2.application.service;

import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m2.application.command.ComputeRankingCommand;
import com.memeboo2.haemi.m2.application.dto.RankingResult;
import com.memeboo2.haemi.m2.application.query.GetRankingQuery;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPost;
import com.memeboo2.haemi.m2.domain.model.ranking.FamilyRanking;
import com.memeboo2.haemi.m2.domain.repository.FamilyRankingRepository;
import com.memeboo2.haemi.m2.domain.repository.MemoryPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingApplicationService {

    private final MemoryPostRepository postRepository;
    private final FamilyRankingRepository rankingRepository;
    private final NotificationPort notificationPort;

    // F2-04: 랭킹 집계 (스케줄러에서 호출)
    @Transactional
    public RankingResult computeRanking(ComputeRankingCommand command) {
        UUID albumId = UUID.fromString(command.albumId());

        LocalDateTime from = command.periodStart().atStartOfDay();
        LocalDateTime to   = command.periodEnd().atTime(23, 59, 59);
        List<MemoryPost> posts = postRepository
                .findPublishedByAlbumIdAndPeriod(albumId, from, to);

        Map<String, Integer> prevStars = rankingRepository.findTotalStarsByAlbumId(albumId);

        FamilyRanking ranking = FamilyRanking.compute(
                albumId, command.period(),
                command.periodStart(), command.periodEnd(),
                posts, prevStars);

        rankingRepository.save(ranking);
        log.info("랭킹 집계 완료: albumId={}, period={}", albumId, command.period());

        // 뱃지 수여 알림은 FamilyRanking이 발행한 BadgeAwardedEvent 리스너에서 처리
        return RankingResult.from(ranking);
    }

    // F2-04: 랭킹 조회
    @Transactional(readOnly = true)
    public Optional<RankingResult> getRanking(GetRankingQuery query) {
        return rankingRepository
                .findLatestByAlbumIdAndPeriod(UUID.fromString(query.albumId()), query.period())
                .map(RankingResult::from);
    }
}
