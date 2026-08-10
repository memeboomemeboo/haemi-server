package com.memeboo2.haemi.m1.infrastructure.ai;

import com.memeboo2.haemi.m1.domain.port.ReminiscenceCardGeneratorPort.CardGenerationRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 외부 AI 호출 타임아웃 (#98).
 *
 * <p>확인해야 할 것은 "실패하는가"가 아니라 <b>"끝나는가"</b>다. 어댑터는 원래 모든 예외를
 * 안전한 기본 카드로 받아 주므로, 타임아웃이 없을 때의 증상은 오류가 아니라 무한 대기다.
 */
class GeminiReminiscenceCardGeneratorTest {

    private ServerSocket silentServer;
    private Thread acceptor;

    @AfterEach
    void tearDown() throws IOException {
        if (acceptor != null) {
            acceptor.interrupt();
        }
        if (silentServer != null && !silentServer.isClosed()) {
            silentServer.close();
        }
    }

    @Test
    @DisplayName("API 키가 없으면 외부 호출 없이 기본 카드로 폴백한다")
    void fallsBackWithoutApiKey() {
        GeminiReminiscenceCardGenerator generator = new GeminiReminiscenceCardGenerator(
                "", "gemini-3.6-flash", Duration.ofSeconds(5), Duration.ofSeconds(20));

        assertThat(generator.generate(request())).isPresent();
    }

    @Test
    @DisplayName("업스트림이 응답하지 않아도 유한 시간 안에 폴백으로 끝난다")
    void doesNotHangWhenUpstreamNeverResponds() throws Exception {
        int port = startSilentServer();
        GeminiReminiscenceCardGenerator generator = new GeminiReminiscenceCardGenerator(
                "test-key", "gemini-3.6-flash", Duration.ofMillis(300), Duration.ofMillis(300));
        // 실제 엔드포인트 대신 응답하지 않는 로컬 서버를 보게 한다.
        setBaseUrl(generator, "http://127.0.0.1:" + port);

        long startedAt = System.nanoTime();
        var card = generator.generate(request());
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

        assertThat(card).as("타임아웃은 폴백으로 흡수된다").isPresent();
        assertThat(elapsed)
                .as("타임아웃이 없으면 여기서 영원히 멈춘다")
                .isLessThan(Duration.ofSeconds(10));
    }

    /** 연결은 받아 주지만 응답을 보내지 않는 서버. 리드 타임아웃이 걸리는 상황을 만든다. */
    private int startSilentServer() throws IOException {
        silentServer = new ServerSocket(0);
        CountDownLatch ready = new CountDownLatch(1);
        acceptor = new Thread(() -> {
            ready.countDown();
            while (!Thread.currentThread().isInterrupted() && !silentServer.isClosed()) {
                try (Socket ignored = silentServer.accept()) {
                    TimeUnit.SECONDS.sleep(30);
                } catch (IOException | InterruptedException stop) {
                    return;
                }
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();
        return silentServer.getLocalPort();
    }

    private void setBaseUrl(GeminiReminiscenceCardGenerator generator, String baseUrl) throws Exception {
        var field = GeminiReminiscenceCardGenerator.class.getDeclaredField("restClient");
        field.setAccessible(true);
        var existing = (org.springframework.web.client.RestClient) field.get(generator);
        field.set(generator, existing.mutate().baseUrl(baseUrl).build());
    }

    private CardGenerationRequest request() {
        return new CardGenerationRequest(UUID.randomUUID(), "1978년 여름, 고향집", "", 2, List.of());
    }
}
