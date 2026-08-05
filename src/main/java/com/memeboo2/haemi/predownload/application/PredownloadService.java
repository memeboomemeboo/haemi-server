package com.memeboo2.haemi.predownload.application;

import com.memeboo2.haemi.predownload.domain.PredownloadBundle;
import com.memeboo2.haemi.predownload.domain.port.PredownloadContentPort;
import com.memeboo2.haemi.predownload.domain.port.PredownloadDispatchPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 선다운로드 파이프라인 (#48). 08:45에 적격 어르신마다 오늘의 자산 번들을 조립해 사전 전송한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PredownloadService {

    private final PredownloadContentPort contentPort;
    private final PredownloadDispatchPort dispatchPort;

    public PredownloadSummary runDailyPredownload(LocalDate date) {
        List<String> elderIds = contentPort.eligibleElderIds(date);
        int dispatched = 0;
        int totalAssets = 0;

        for (String elderId : elderIds) {
            PredownloadBundle bundle = contentPort.assemble(elderId, date);
            if (bundle.isEmpty()) {
                log.debug("선다운로드 대상 자산 없음, 생략: elderId={}, date={}", elderId, date);
                continue;
            }
            dispatchPort.dispatch(bundle);
            dispatched++;
            totalAssets += bundle.totalAssets();
        }

        PredownloadSummary summary = new PredownloadSummary(date, elderIds.size(), dispatched, totalAssets);
        log.info("선다운로드 완료: date={}, 대상={}명, 전송={}건, 자산={}개",
                date, summary.elderCount(), summary.dispatchedCount(), summary.totalAssets());
        return summary;
    }
}
