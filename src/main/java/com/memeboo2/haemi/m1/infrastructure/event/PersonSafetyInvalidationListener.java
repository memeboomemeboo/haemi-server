package com.memeboo2.haemi.m1.infrastructure.event;

import com.memeboo2.haemi.m0.domain.event.PersonSafetyChangedEvent;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import com.memeboo2.haemi.m1.domain.repository.ReminiscenceContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** EX-F004-04: 숨김·사별 상태가 바뀌면 이미 생성된 카드도 즉시 폐기한다. */
@Component
@RequiredArgsConstructor
public class PersonSafetyInvalidationListener {

    private final AlbumRepository albums;
    private final ReminiscenceContentRepository contents;

    @EventListener
    @Transactional
    public void invalidate(PersonSafetyChangedEvent event) {
        albums.findByGroupId(event.groupId().toString())
                .ifPresent(album -> contents.invalidateByAlbumId(album.getAlbumId()));
    }
}
