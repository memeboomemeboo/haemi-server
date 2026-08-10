package com.memeboo2.haemi.m2.infrastructure.scheduler;

import com.memeboo2.haemi.m0.application.service.ElderRecipientResolver;
import com.memeboo2.haemi.m1.application.service.AlbumBatchScanner;
import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import com.memeboo2.haemi.m2.domain.repository.MemoryPostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EveningNotificationSchedulerTest {

    private final NotificationPort notificationPort = mock(NotificationPort.class);
    private final AlbumRepository albums = mock(AlbumRepository.class);
    private final MemoryPostRepository posts = mock(MemoryPostRepository.class);
    private final ElderRecipientResolver elderRecipients = mock(ElderRecipientResolver.class);
    private final EveningNotificationScheduler scheduler = new EveningNotificationScheduler(
            notificationPort, new AlbumBatchScanner(albums), posts, elderRecipients);

    @Test
    @DisplayName("미열람 글이 있는 앨범의 어르신에게만 요약을 보낸다")
    void sendsOnlyToEldersWithUnreadPosts() {
        Album withUnread = album();
        Album withoutUnread = album();
        when(albums.findPage(0, AlbumBatchScanner.BATCH_SIZE)).thenReturn(List.of(withUnread, withoutUnread));
        when(posts.existsUnreadPublishedByAlbumIdSince(eq(withUnread.getId()), any(LocalDateTime.class)))
                .thenReturn(true);
        when(posts.existsUnreadPublishedByAlbumIdSince(eq(withoutUnread.getId()), any(LocalDateTime.class)))
                .thenReturn(false);
        when(elderRecipients.resolveByGroupId(withUnread.getGroupId())).thenReturn(Optional.of("elder-1"));

        scheduler.sendEveningSummary();

        verify(notificationPort).sendToElder(eq("elder-1"), anyString(), anyString());
        verify(notificationPort, times(1)).sendToElder(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("같은 어르신의 앨범이 여럿이어도 요약은 한 번만 보낸다")
    void deduplicatesRecipientsAcrossAlbums() {
        Album first = album();
        Album second = album();
        when(albums.findPage(0, AlbumBatchScanner.BATCH_SIZE)).thenReturn(List.of(first, second));
        when(posts.existsUnreadPublishedByAlbumIdSince(any(UUID.class), any(LocalDateTime.class))).thenReturn(true);
        when(elderRecipients.resolveByGroupId(anyString())).thenReturn(Optional.of("elder-1"));

        scheduler.sendEveningSummary();

        verify(notificationPort, times(1)).sendToElder(eq("elder-1"), anyString(), anyString());
    }

    @Test
    @DisplayName("연결된 어르신 프로필이 없으면 발송하지 않는다")
    void skipsAlbumsWithoutLinkedElder() {
        Album album = album();
        when(albums.findPage(0, AlbumBatchScanner.BATCH_SIZE)).thenReturn(List.of(album));
        when(posts.existsUnreadPublishedByAlbumIdSince(any(UUID.class), any(LocalDateTime.class))).thenReturn(true);
        when(elderRecipients.resolveByGroupId(anyString())).thenReturn(Optional.empty());

        scheduler.sendEveningSummary();

        verifyNoInteractions(notificationPort);
    }

    private static Album album() {
        return Album.create(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                UUID.randomUUID().toString());
    }
}
