package com.memeboo2.haemi.m2.infrastructure.event;

import com.memeboo2.haemi.m0.application.service.ElderRecipientResolver;
import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import com.memeboo2.haemi.m2.application.service.MemoryPostApplicationService;
import com.memeboo2.haemi.m2.domain.event.ElderRepliedEvent;
import com.memeboo2.haemi.m2.domain.event.MemoryPostPublishedEvent;
import com.memeboo2.haemi.m2.domain.model.post.AuthorInfo;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPostId;
import com.memeboo2.haemi.m2.domain.model.post.ReplyType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemoryPostEventListenerTest {

    private final NotificationPort notifications = mock(NotificationPort.class);
    private final MemoryPostApplicationService posts = mock(MemoryPostApplicationService.class);
    private final AlbumRepository albums = mock(AlbumRepository.class);
    private final ElderRecipientResolver elderRecipients = mock(ElderRecipientResolver.class);
    private final MemoryPostEventListener listener = new MemoryPostEventListener(
            notifications, posts, albums, elderRecipients);

    @Test
    void routesPublishedMemoryToCanonicalElderProfileInsteadOfLegacyAlbumIdentifier() {
        Album album = Album.create("legacy-account-id", UUID.randomUUID().toString(), "family-1");
        UUID postId = UUID.randomUUID();
        String elderId = UUID.randomUUID().toString();
        when(albums.findById(AlbumId.of(album.getId()))).thenReturn(Optional.of(album));
        when(elderRecipients.resolveByGroupId(album.getGroupId())).thenReturn(Optional.of(elderId));

        listener.onPostPublished(new MemoryPostPublishedEvent(
                MemoryPostId.of(postId), album.getId(), AuthorInfo.of("family-1", "딸", "딸"), LocalDateTime.now()));

        verify(posts).handleElderNotification(postId.toString(), album.getId(), elderId);
    }

    @Test
    void routesElderReplyToActualAcceptedAlbumMembers() {
        Album album = Album.create("legacy-account-id", UUID.randomUUID().toString(), "family-1");
        album.inviteMember("family-2");
        album.acceptInvite("family-2");
        UUID postId = UUID.randomUUID();
        when(albums.findById(AlbumId.of(album.getId()))).thenReturn(Optional.of(album));

        listener.onElderReplied(new ElderRepliedEvent(
                MemoryPostId.of(postId), album.getId(), ReplyType.EMOJI, LocalDateTime.now()));

        verify(notifications).sendToGroup(eq(album.getMemberIds()), anyString(), anyString());
    }
}
