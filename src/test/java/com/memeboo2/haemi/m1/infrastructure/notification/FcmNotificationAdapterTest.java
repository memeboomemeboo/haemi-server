package com.memeboo2.haemi.m1.infrastructure.notification;

import com.memeboo2.haemi.notification.application.PushDispatchService;
import com.memeboo2.haemi.notification.domain.PushMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FcmNotificationAdapterTest {

    @Mock PushDispatchService pushDispatch;

    FcmNotificationAdapter adapter;

    @BeforeEach
    void setUp() {
        // 위임 내용 자체를 보기 위해 호출 스레드에서 바로 실행하는 executor를 쓴다.
        adapter = new FcmNotificationAdapter(pushDispatch, Runnable::run);
    }

    @Test
    @DisplayName("단일 수신자 알림은 제목·본문 그대로 위임된다")
    void sendToMember() {
        adapter.sendToMember("member-1", "새 사진이 왔어요", "손녀가 사진을 올렸어요");

        ArgumentCaptor<PushMessage> message = ArgumentCaptor.captor();
        verify(pushDispatch).dispatchToMember(org.mockito.ArgumentMatchers.eq("member-1"), message.capture());
        assertThat(message.getValue().title()).isEqualTo("새 사진이 왔어요");
        assertThat(message.getValue().body()).isEqualTo("손녀가 사진을 올렸어요");
    }

    @Test
    @DisplayName("그룹 알림은 수신자 전원으로 위임된다")
    void sendToGroup() {
        adapter.sendToGroup(Set.of("member-1", "member-2"), "제목", "본문");

        ArgumentCaptor<Collection<String>> members = ArgumentCaptor.captor();
        verify(pushDispatch).dispatchToMembers(members.capture(), org.mockito.ArgumentMatchers.any());
        assertThat(members.getValue()).containsExactlyInAnyOrder("member-1", "member-2");
    }
}
