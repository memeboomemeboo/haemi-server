package com.memeboo2.haemi.m1.application.service;

import com.memeboo2.haemi.m1.application.command.AcceptInviteCommand;
import com.memeboo2.haemi.m1.application.command.InviteMemberCommand;
import com.memeboo2.haemi.m1.application.dto.AlbumResult;
import com.memeboo2.haemi.m1.application.dto.TimelineResult;
import com.memeboo2.haemi.m1.application.query.GetAlbumQuery;
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
                new GetTimelineQuery(album.getAlbumId().toString(), "owner", null, null, null, null));

        assertThat(result.belowMinimumPhotoThreshold()).isTrue();
        assertThat(result.guideMessage()).isEqualTo("사진을 더 추가하면 타임라인이 만들어집니다");
    }

    @Test
    @DisplayName("가족 관리용 타임라인은 충분한 사진이 있으면 안내 없이 반환한다")
    void getTimeline_returnsFamilyManagementViewWithoutViewerRoleFlag() {
        addPhoto("hash-1");
        addPhoto("hash-2");
        addPhoto("hash-3");

        TimelineResult familyResult = service.getTimeline(
                new GetTimelineQuery(album.getAlbumId().toString(), "owner", null, null, null, null));

        assertThat(familyResult.belowMinimumPhotoThreshold()).isFalse();
    }

    @Test
    @DisplayName("앨범 구성원도 어르신도 아니면 앨범과 타임라인을 조회할 수 없다")
    void getAlbumAndTimeline_rejectNonMemberNonElderViewer() {
        assertThatThrownBy(() -> service.getAlbum(new GetAlbumQuery(album.getAlbumId().toString(), "stranger")))
                .isInstanceOf(AlbumAccessDeniedException.class);
        assertThatThrownBy(() -> service.getTimeline(
                new GetTimelineQuery(album.getAlbumId().toString(), "stranger", null, null, null, null)))
                .isInstanceOf(AlbumAccessDeniedException.class);
    }

    @Test
    @DisplayName("어르신 본인은 구성원이 아니어도 앨범을 조회할 수 있다")
    void getAlbum_allowsElderViewer() {
        AlbumResult result = service.getAlbum(new GetAlbumQuery(album.getAlbumId().toString(), "elder-1"));

        assertThat(result.albumId()).isEqualTo(album.getAlbumId().toString());
    }

    private void addPhoto(String hash) {
        album.addPhoto(
                PhotoFile.of("key-" + hash, hash + ".jpg", "image/jpeg", 1024),
                PhotoMetadata.of(LocalDateTime.now(), null, null),
                hash, "owner");
    }
}
