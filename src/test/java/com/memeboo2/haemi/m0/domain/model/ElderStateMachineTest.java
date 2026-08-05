package com.memeboo2.haemi.m0.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ElderStateMachineTest {

    private static final int SILENT_DAYS = 7;
    private static final int RECOVERY_HOURS = 48;

    private Elder elder() {
        return Elder.create(UUID.randomUUID(), null, "김어르신", 1945,
                Gender.FEMALE, ResidenceType.HOME_WITH_FAMILY);
    }

    @Test
    @DisplayName("생존 상태 간 전이는 허용되고, DECEASED로의 직접 전이는 막는다")
    void transitionTo_livingAndBlocksDirectDeceased() {
        Elder elder = elder();

        elder.transitionTo(ElderStatus.HOSPITALIZED);
        assertThat(elder.getStatus()).isEqualTo(ElderStatus.HOSPITALIZED);

        assertThatThrownBy(() -> elder.transitionTo(ElderStatus.DECEASED))
                .isInstanceOf(M0ValidationException.class);
    }

    @Test
    @DisplayName("사별은 2단계(요청→확정)로만 처리되며 확정 시 무음기간이 설정된다")
    void bereavement_twoStep() {
        Elder elder = elder();
        LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);

        assertThatThrownBy(() -> elder.confirmBereavement(now, SILENT_DAYS))
                .isInstanceOf(M0ValidationException.class); // 요청 없이 확정 불가

        elder.requestBereavement(now);
        assertThat(elder.isBereavementPending()).isTrue();

        elder.confirmBereavement(now, SILENT_DAYS);
        assertThat(elder.getStatus()).isEqualTo(ElderStatus.DECEASED);
        assertThat(elder.getBereavedAt()).isEqualTo(now);
        assertThat(elder.getSilentUntil()).isEqualTo(now.plusDays(SILENT_DAYS));
        assertThat(elder.isBereavementPending()).isFalse();
    }

    @Test
    @DisplayName("EX-F005-01: 사별 확정 후에는 발송 대상에서 제외된다(발송 최종 검증)")
    void ex_f005_01_notDispatchableAfterBereavement() {
        Elder elder = elder();
        LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
        assertThat(elder.isDispatchable(now)).isTrue();

        elder.requestBereavement(now);
        elder.confirmBereavement(now, SILENT_DAYS);

        assertThat(elder.isDispatchable(now)).isFalse();
        // 무음기간이 끝난 뒤에도 DECEASED이므로 여전히 발송 불가
        assertThat(elder.isDispatchable(now.plusDays(SILENT_DAYS + 1))).isFalse();
    }

    @Test
    @DisplayName("입원 상태와 무음기간 중에는 발송 대상에서 제외된다")
    void notDispatchableWhenHospitalizedOrSilent() {
        Elder hospitalized = elder();
        hospitalized.transitionTo(ElderStatus.HOSPITALIZED);
        assertThat(hospitalized.isDispatchable(LocalDateTime.now())).isFalse();
    }

    @Test
    @DisplayName("EX-F005-06 인접: 48시간 내에는 사별 오등록을 ACTIVE로 복구한다")
    void recoverFromBereavement_within48Hours() {
        Elder elder = elder();
        LocalDateTime bereaved = LocalDateTime.of(2026, 8, 5, 10, 0);
        elder.requestBereavement(bereaved);
        elder.confirmBereavement(bereaved, SILENT_DAYS);

        elder.recoverFromBereavement(bereaved.plusHours(47), RECOVERY_HOURS);

        assertThat(elder.getStatus()).isEqualTo(ElderStatus.ACTIVE);
        assertThat(elder.getBereavedAt()).isNull();
        assertThat(elder.getSilentUntil()).isNull();
        assertThat(elder.isDispatchable(bereaved.plusHours(47))).isTrue();
    }

    @Test
    @DisplayName("48시간이 지나면 사별 복구를 거부한다")
    void recoverFromBereavement_rejectedAfterWindow() {
        Elder elder = elder();
        LocalDateTime bereaved = LocalDateTime.of(2026, 8, 5, 10, 0);
        elder.requestBereavement(bereaved);
        elder.confirmBereavement(bereaved, SILENT_DAYS);

        assertThatThrownBy(() -> elder.recoverFromBereavement(bereaved.plusHours(49), RECOVERY_HOURS))
                .isInstanceOf(M0ValidationException.class);
    }

    @Test
    @DisplayName("무음기간 경과 후에만 memorial로 봉인되고 memorial은 종결 상태다")
    void enshrineMemorial_afterSilentPeriod() {
        Elder elder = elder();
        LocalDateTime bereaved = LocalDateTime.of(2026, 8, 5, 10, 0);
        elder.requestBereavement(bereaved);
        elder.confirmBereavement(bereaved, SILENT_DAYS);

        assertThatThrownBy(() -> elder.enshrineMemorial(bereaved.plusDays(1)))
                .isInstanceOf(M0ValidationException.class); // 무음기간 중

        elder.enshrineMemorial(bereaved.plusDays(SILENT_DAYS));
        assertThat(elder.getStatus()).isEqualTo(ElderStatus.MEMORIAL);
        assertThat(elder.isMemorialArchiveOnly()).isTrue();
        assertThatThrownBy(() -> elder.transitionTo(ElderStatus.ACTIVE))
                .isInstanceOf(M0ValidationException.class); // 종결
    }
}
