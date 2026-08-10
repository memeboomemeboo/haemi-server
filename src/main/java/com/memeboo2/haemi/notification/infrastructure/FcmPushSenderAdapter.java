package com.memeboo2.haemi.notification.infrastructure;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.memeboo2.haemi.notification.domain.PushMessage;
import com.memeboo2.haemi.notification.domain.PushSendResult;
import com.memeboo2.haemi.notification.domain.port.PushSenderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * FCM 멀티캐스트 발송 어댑터 (#80).
 * {@link PushSenderConfig}가 FirebaseMessaging 빈이 있을 때만 등록한다.
 */
@Slf4j
@RequiredArgsConstructor
public class FcmPushSenderAdapter implements PushSenderPort {

    // FCM 멀티캐스트 1회 상한
    private static final int BATCH_SIZE = 500;

    /**
     * 재시도해도 살아나지 않는 토큰만 정리 대상으로 본다.
     * UNAVAILABLE·INTERNAL·QUOTA_EXCEEDED 같은 일시 오류는 토큰을 지우지 않는다.
     */
    private static final Set<MessagingErrorCode> PERMANENT_FAILURES = EnumSet.of(
            MessagingErrorCode.UNREGISTERED,
            MessagingErrorCode.INVALID_ARGUMENT,
            MessagingErrorCode.SENDER_ID_MISMATCH
    );

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public PushSendResult send(List<String> tokens, PushMessage message) {
        int success = 0;
        int failure = 0;
        List<String> invalidTokens = new ArrayList<>();

        for (int start = 0; start < tokens.size(); start += BATCH_SIZE) {
            List<String> chunk = tokens.subList(start, Math.min(start + BATCH_SIZE, tokens.size()));
            BatchResponse response;
            try {
                response = firebaseMessaging.sendEachForMulticast(buildMessage(chunk, message));
            } catch (FirebaseMessagingException e) {
                // 청크 단위 실패는 토큰 문제로 단정할 수 없다. 정리하지 않고 실패로만 센다.
                log.warn("[FCM] 배치 발송 실패 (tokens={}): {}", chunk.size(), e.getMessagingErrorCode(), e);
                failure += chunk.size();
                continue;
            }

            success += response.getSuccessCount();
            failure += response.getFailureCount();
            invalidTokens.addAll(collectInvalidTokens(chunk, response));
        }

        return new PushSendResult(success, failure, invalidTokens);
    }

    private MulticastMessage buildMessage(List<String> tokens, PushMessage message) {
        return MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .putAllData(message.data())
                .build();
    }

    private List<String> collectInvalidTokens(List<String> tokens, BatchResponse response) {
        List<String> invalid = new ArrayList<>();
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse each = responses.get(i);
            if (each.isSuccessful()) {
                continue;
            }
            FirebaseMessagingException exception = each.getException();
            if (exception != null && PERMANENT_FAILURES.contains(exception.getMessagingErrorCode())) {
                invalid.add(tokens.get(i));
            }
        }
        return invalid;
    }
}
