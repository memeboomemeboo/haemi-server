package com.memeboo2.haemi.m1.infrastructure.event;

import com.memeboo2.haemi.m0.domain.event.FamilyMemberJoinedEvent;
import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlbumMemberAutoInviteListener {

    private final AlbumRepository albumRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFamilyMemberJoined(FamilyMemberJoinedEvent event) {
        albumRepository.findByGroupId(event.groupId().toString()).ifPresentOrElse(
                album -> autoInvite(album, event.memberId().toString()),
                () -> log.debug("[EVENT] 앨범 없음, 자동 초대 생략. groupId={}", event.groupId())
        );
    }

    private void autoInvite(Album album, String memberId) {
        boolean newlyInvited = album.inviteMember(memberId);
        albumRepository.save(album);
        if (newlyInvited) {
            log.info("[EVENT] 앨범 자동 초대: albumId={}, memberId={}", album.getAlbumId(), memberId);
        }
    }
}
