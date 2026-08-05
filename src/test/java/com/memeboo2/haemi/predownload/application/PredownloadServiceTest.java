package com.memeboo2.haemi.predownload.application;

import com.memeboo2.haemi.predownload.domain.PredownloadBundle;
import com.memeboo2.haemi.predownload.domain.port.PredownloadContentPort;
import com.memeboo2.haemi.predownload.domain.port.PredownloadDispatchPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PredownloadServiceTest {

    @Mock PredownloadContentPort contentPort;
    @Mock PredownloadDispatchPort dispatchPort;

    PredownloadService service;

    private final LocalDate date = LocalDate.of(2026, 8, 5);

    @BeforeEach
    void setUp() {
        service = new PredownloadService(contentPort, dispatchPort);
    }

    private PredownloadBundle bundle(String elderId, int cards, int photos, int hints) {
        return PredownloadBundle.of(elderId, date,
                keys("c", cards), keys("p", photos), keys("h", hints));
    }

    private List<String> keys(String prefix, int n) {
        return java.util.stream.IntStream.range(0, n).mapToObj(i -> prefix + i).toList();
    }

    @Test
    @DisplayName("적격 어르신마다 번들을 조립해 전송하고 자산을 집계한다")
    void dispatchesNonEmptyBundlesAndCounts() {
        when(contentPort.eligibleElderIds(date)).thenReturn(List.of("elder-1", "elder-2"));
        when(contentPort.assemble("elder-1", date)).thenReturn(bundle("elder-1", 2, 1, 1)); // 4
        when(contentPort.assemble("elder-2", date)).thenReturn(bundle("elder-2", 3, 0, 2)); // 5

        PredownloadSummary summary = service.runDailyPredownload(date);

        verify(dispatchPort, times(2)).dispatch(any(PredownloadBundle.class));
        assertThat(summary.elderCount()).isEqualTo(2);
        assertThat(summary.dispatchedCount()).isEqualTo(2);
        assertThat(summary.totalAssets()).isEqualTo(9);
    }

    @Test
    @DisplayName("빈 번들은 전송하지 않고 집계에서 제외한다")
    void skipsEmptyBundles() {
        when(contentPort.eligibleElderIds(date)).thenReturn(List.of("elder-1", "elder-2"));
        when(contentPort.assemble("elder-1", date)).thenReturn(bundle("elder-1", 0, 0, 0)); // empty
        when(contentPort.assemble("elder-2", date)).thenReturn(bundle("elder-2", 1, 1, 0)); // 2

        PredownloadSummary summary = service.runDailyPredownload(date);

        verify(dispatchPort, times(1)).dispatch(any(PredownloadBundle.class));
        assertThat(summary.elderCount()).isEqualTo(2);
        assertThat(summary.dispatchedCount()).isEqualTo(1);
        assertThat(summary.totalAssets()).isEqualTo(2);
    }

    @Test
    @DisplayName("적격 어르신이 없으면 아무것도 전송하지 않는다")
    void noEligibleElders() {
        when(contentPort.eligibleElderIds(date)).thenReturn(List.of());

        PredownloadSummary summary = service.runDailyPredownload(date);

        verify(dispatchPort, never()).dispatch(any());
        assertThat(summary.dispatchedCount()).isZero();
        assertThat(summary.totalAssets()).isZero();
    }
}
