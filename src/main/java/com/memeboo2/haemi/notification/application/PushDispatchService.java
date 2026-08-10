package com.memeboo2.haemi.notification.application;

import com.memeboo2.haemi.m0.domain.port.ElderStatusQuery;
import com.memeboo2.haemi.notification.domain.DeviceToken;
import com.memeboo2.haemi.notification.domain.PushMessage;
import com.memeboo2.haemi.notification.domain.PushSendResult;
import com.memeboo2.haemi.notification.domain.port.PushSenderPort;
import com.memeboo2.haemi.notification.domain.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 수신자(memberId) → 기기 토큰 → 발송 → 무효 토큰 정리 (#80).
 * 알림 실패가 본 업무 흐름을 깨지 않도록 모든 예외를 여기서 흡수한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushDispatchService {

    private final DeviceTokenRepository deviceTokens;
    private final PushSenderPort pushSender;
    private final ElderStatusQuery elderStatusQuery;

    public PushSendResult dispatchToMember(String memberId, PushMessage message) {
        return dispatchToMembers(Set.of(memberId), message);
    }

    /** F5-01 등의 어르신 기기 알림. 계정 ID가 아닌 어르신 프로필에 연결된 기기만 대상으로 한다. */
    public PushSendResult dispatchToElder(String elderId, PushMessage message) {
        if (!elderStatusQuery.isDispatchable(elderId)) {
            log.info("[PUSH] 발송 불가 어르신 상태 또는 잘못된 ID로 기기 알림을 건너뜁니다. elderId={}", elderId);
            return PushSendResult.empty();
        }
        try {
            List<String> tokens = deviceTokens.findByElderId(UUID.fromString(elderId)).stream()
                    .map(DeviceToken::getToken)
                    .toList();
            return sendAndPrune(tokens, message, "elder=" + elderId);
        } catch (Exception e) {
            log.error("[PUSH] 어르신 기기 알림 발송에 실패했습니다. elderId={}, title={}", elderId, message.title(), e);
            return PushSendResult.empty();
        }
    }

    /**
     * 발송 결과를 돌려주지만, 업무 흐름에서 호출하는 어댑터는 이 값을 쓰지 않는다.
     * 결과가 필요한 곳은 개발용 테스트 발송처럼 "무슨 일이 일어났는지" 보여줘야 하는 경로다.
     */
    public PushSendResult dispatchToMembers(Collection<String> memberIds, PushMessage message) {
        if (memberIds == null || memberIds.isEmpty()) {
            return PushSendResult.empty();
        }
        try {
            List<String> tokens = deviceTokens.findByMemberIds(memberIds).stream()
                    .map(DeviceToken::getToken)
                    .toList();
            if (tokens.isEmpty()) {
                log.debug("[PUSH] 등록된 기기 토큰이 없어 발송을 건너뜁니다. members={}", memberIds.size());
                return PushSendResult.empty();
            }

            return sendAndPrune(tokens, message, "members=" + memberIds.size());
        } catch (Exception e) {
            log.error("[PUSH] 알림 발송에 실패했습니다. title={}", message.title(), e);
            return PushSendResult.empty();
        }
    }

    private PushSendResult sendAndPrune(List<String> tokens, PushMessage message, String recipientDescription) {
        if (tokens.isEmpty()) {
            log.debug("[PUSH] 등록된 기기 토큰이 없어 발송을 건너뜁니다. {}", recipientDescription);
            return PushSendResult.empty();
        }
        PushSendResult result = pushSender.send(tokens, message);
        if (!result.invalidTokens().isEmpty()) {
            // 영구 실패 토큰만 정리한다. 일시 오류 토큰은 sender가 걸러 보내지 않는다.
            deviceTokens.deleteAllByTokens(result.invalidTokens());
            log.info("[PUSH] 무효 토큰 {}건을 정리했습니다.", result.invalidTokens().size());
        }
        if (result.failureCount() > 0) {
            log.warn("[PUSH] 발송 일부 실패: 성공={} 실패={}", result.successCount(), result.failureCount());
        }
        return result;
    }
}
