package com.memeboo2.haemi.m1.application.service;

import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * 야간 배치가 앨범을 페이지 단위로 훑도록 순회를 한곳에 모은 컴포넌트 (#85).
 *
 * <p>스케줄러마다 페이지 루프를 복붙하지 않고, 배치 크기도 여기서만 관리한다.
 */
@Component
@RequiredArgsConstructor
public class AlbumBatchScanner {

    public static final int BATCH_SIZE = 100;

    private final AlbumRepository albums;

    /** 모든 앨범을 페이지 단위로 순회한다. */
    public void forEachAlbum(Consumer<Album> action) {
        scan(page -> albums.findPage(page, BATCH_SIZE), action);
    }

    /** 사진이 {@code minPhotos}장 이상인 앨범만 순회한다. */
    public void forEachAlbumWithAtLeastPhotos(int minPhotos, Consumer<Album> action) {
        scan(page -> albums.findPageWithAtLeastPhotos(minPhotos, page, BATCH_SIZE), action);
    }

    private void scan(PageLoader loader, Consumer<Album> action) {
        for (int page = 0; ; page++) {
            List<Album> batch = loader.load(page);
            if (batch.isEmpty()) {
                return;
            }
            batch.forEach(action);
            // 마지막 페이지가 정확히 가득 찬 경우까지 고려해 한 번 더 확인한다.
            if (batch.size() < BATCH_SIZE) {
                return;
            }
        }
    }

    @FunctionalInterface
    private interface PageLoader {
        List<Album> load(int page);
    }
}
