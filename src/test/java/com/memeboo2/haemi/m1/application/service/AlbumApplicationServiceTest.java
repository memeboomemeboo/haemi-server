package com.memeboo2.haemi.m1.application.service;

import com.memeboo2.haemi.m1.application.command.AcceptInviteCommand;
import com.memeboo2.haemi.m1.application.command.InviteMemberCommand;
import com.memeboo2.haemi.m1.application.dto.TimelineResult;
import com.memeboo2.haemi.m1.application.query.GetTimelineQuery;
import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.AlbumAccessDeniedException;
import com.memeboo2.haemi.m1.domain.model.album.PhotoFile;
import com.memeboo2.haemi.m1.domain.model.album.PhotoMetadata;
import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlbumApplicationServiceTest {

    @Mock AlbumRepository albumRepository;
    @Mock NotificationPort notificationPort;

    AlbumApplicationService service;
    Album album;

    @BeforeEach
    void setUp() {
        service = new AlbumApplicationService(albumRepository, notificationPort);
        album = Album.create("elder-1", "group-1", "owner");
        when(albumRepository.findById(album.getAlbumId())).thenReturn(Optional.of(album));
    }

    @Test
    @DisplayName("앨범 구성원이 아닌 사람은 초대를 보낼 수 없다")
    void inviteMember_rejectsInviterOutsideAlbum() {
        InviteMemberCommand command = new InviteMemberCommand(album.getAlbumId().toString(), "stranger", "family-2");

        assertThatThrownBy(() -> service.inviteMember(command))
                .isInstanceOf(AlbumAccessDeniedException.class);
        assertThat(album.isMember("family-2")).isFalse();
    }

    @Test
    @DisplayName("앨범 구성원은 새 멤버를 초대할 수 있고, 수락 전까지는 PENDING 상태다")
    void inviteMember_allowsExistingMemberToInvite() {
        InviteMemberCommand command = new InviteMemberCommand(album.getAlbumId().toString(), "owner", "family-2");

        service.inviteMember(command);

        assertThat(album.isMember("family-2")).isFalse();

        service.acceptInvite(new AcceptInviteCommand(album.getAlbumId().toString(), "family-2"));

        assertThat(album.isMember("family-2")).isTrue();
    }

    @Test
    @DisplayName("시기 메타데이터가 있는 사진이 3장 미만이면 안내 메시지를 반환한다")
    void getTimeline_returnsGuideMessageBelowThreshold() {
        addPhoto("hash-1");

        TimelineResult result = service.getTimeline(
                new GetTimelineQuery(album.getAlbumId().toString(), null, null, null, null, "FAMILY"));

        assertThat(result.belowMinimumPhotoThreshold()).isTrue();
        assertThat(result.guideMessage()).isEqualTo("사진을 더 추가하면 타임라인이 만들어집니다");
    }

    @Test
    @DisplayName("FAMILY 역할만 타임라인을 편집할 수 있다")
    void getTimeline_setsEditableByRole() {
        addPhoto("hash-1");
        addPhoto("hash-2");
        addPhoto("hash-3");

        TimelineResult familyResult = service.getTimeline(
                new GetTimelineQuery(album.getAlbumId().toString(), null, null, null, null, "FAMILY"));
        TimelineResult elderResult = service.getTimeline(
                new GetTimelineQuery(album.getAlbumId().toString(), null, null, null, null, "ELDER"));

        assertThat(familyResult.editable()).isTrue();
        assertThat(familyResult.belowMinimumPhotoThreshold()).isFalse();
        assertThat(elderResult.editable()).isFalse();
    }

    private void addPhoto(String hash) {
        album.addPhoto(
                PhotoFile.of("key-" + hash, hash + ".jpg", "image/jpeg", 1024),
                PhotoMetadata.of(LocalDateTime.now(), null, null),
                hash, "owner");
    }
}
