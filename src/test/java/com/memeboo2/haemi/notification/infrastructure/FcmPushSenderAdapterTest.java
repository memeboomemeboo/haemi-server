package com.memeboo2.haemi.notification.infrastructure;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.memeboo2.haemi.notification.domain.PushMessage;
import com.memeboo2.haemi.notification.domain.PushSendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FcmPushSenderAdapterTest {

    @Mock FirebaseMessaging firebaseMessaging;

    FcmPushSenderAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FcmPushSenderAdapter(firebaseMessaging);
    }

    private SendResponse success() {
        SendResponse response = mock(SendResponse.class);
        when(response.isSuccessful()).thenReturn(true);
        return response;
    }

    private SendResponse failure(MessagingErrorCode code) {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(code);
        SendResponse response = mock(SendResponse.class);
        when(response.isSuccessful()).thenReturn(false);
        when(response.getException()).thenReturn(exception);
        return response;
    }

    private BatchResponse batch(List<SendResponse> responses) {
        long successCount = responses.stream().filter(SendResponse::isSuccessful).count();
        BatchResponse batch = mock(BatchResponse.class);
        when(batch.getResponses()).thenReturn(responses);
        when(batch.getSuccessCount()).thenReturn((int) successCount);
        when(batch.getFailureCount()).thenReturn(responses.size() - (int) successCount);
        return batch;
    }

    @Test
    @DisplayName("토큰 자체의 영구 오류만 정리 대상으로 돌려준다")
    void reportsOnlyPermanentFailuresAsInvalid() throws Exception {
        // 스터빙 진행 중에 새 mock을 만들지 않도록 응답을 먼저 조립한다.
        BatchResponse response = batch(List.of(
                success(),
                failure(MessagingErrorCode.UNREGISTERED),
                // INVALID_ARGUMENT는 payload 오류에도 쓰이므로 토큰 무효로 단정하지 않는다.
                failure(MessagingErrorCode.INVALID_ARGUMENT),
                failure(MessagingErrorCode.SENDER_ID_MISMATCH),
                // 일시 오류는 토큰 문제가 아니다.
                failure(MessagingErrorCode.UNAVAILABLE),
                failure(MessagingErrorCode.INTERNAL)
        ));
        when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(response);

        PushSendResult result = adapter.send(
                List.of("ok", "unregistered", "invalid", "mismatch", "unavailable", "internal"),
                PushMessage.of("제목", "본문"));

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(5);
        assertThat(result.invalidTokens()).containsExactly("unregistered", "mismatch");
    }

    @Test
    @DisplayName("토큰이 500개를 넘으면 배치를 나눠 보낸다")
    void splitsIntoBatchesOf500() throws Exception {
        List<String> tokens = IntStream.range(0, 501).mapToObj(i -> "tok-" + i).toList();
        BatchResponse response = batch(List.of(success()));
        // MulticastMessage는 토큰을 노출하지 않으므로 호출 횟수로 분할을 검증한다.
        when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(response);

        adapter.send(tokens, PushMessage.of("제목", "본문"));

        verify(firebaseMessaging, times(2)).sendEachForMulticast(any(MulticastMessage.class));
    }

    @Test
    @DisplayName("배치 호출 자체가 실패하면 실패로 세되 토큰은 지우지 않는다")
    void batchFailureDoesNotInvalidateTokens() throws Exception {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNAVAILABLE);
        when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenThrow(exception);

        PushSendResult result = adapter.send(List.of("tok-1", "tok-2"), PushMessage.of("제목", "본문"));

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(2);
        assertThat(result.invalidTokens()).isEmpty();
    }
}
