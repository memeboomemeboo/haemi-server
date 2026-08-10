package com.memeboo2.haemi.m1.application.service;

import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AlbumBatchScannerTest {

    private final AlbumRepository albums = mock(AlbumRepository.class);
    private final AlbumBatchScanner scanner = new AlbumBatchScanner(albums);

    @Test
    @DisplayName("앨범이 없으면 첫 페이지만 조회하고 끝난다")
    void stopsAtEmptyFirstPage() {
        when(albums.findPage(0, AlbumBatchScanner.BATCH_SIZE)).thenReturn(List.of());

        scanner.forEachAlbum(album -> {
            throw new AssertionError("호출되면 안 된다");
        });

        verify(albums).findPage(0, AlbumBatchScanner.BATCH_SIZE);
        verifyNoMoreInteractions(albums);
    }

    @Test
    @DisplayName("페이지가 가득 차지 않으면 다음 페이지를 요청하지 않는다")
    void stopsAtPartialPage() {
        when(albums.findPage(0, AlbumBatchScanner.BATCH_SIZE)).thenReturn(List.of(album(), album()));

        List<Album> visited = new ArrayList<>();
        scanner.forEachAlbum(visited::add);

        assertThat(visited).hasSize(2);
        verify(albums).findPage(0, AlbumBatchScanner.BATCH_SIZE);
        verifyNoMoreInteractions(albums);
    }

    @Test
    @DisplayName("가득 찬 페이지가 이어지면 마지막 페이지까지 순회한다")
    void walksEveryPage() {
        when(albums.findPage(0, AlbumBatchScanner.BATCH_SIZE)).thenReturn(fullPage());
        when(albums.findPage(1, AlbumBatchScanner.BATCH_SIZE)).thenReturn(fullPage());
        when(albums.findPage(2, AlbumBatchScanner.BATCH_SIZE)).thenReturn(List.of(album()));

        List<Album> visited = new ArrayList<>();
        scanner.forEachAlbum(visited::add);

        assertThat(visited).hasSize(AlbumBatchScanner.BATCH_SIZE * 2 + 1);
        verify(albums).findPage(2, AlbumBatchScanner.BATCH_SIZE);
    }

    @Test
    @DisplayName("가득 찬 마지막 페이지 뒤의 빈 페이지에서도 멈춘다")
    void stopsAtEmptyPageAfterExactlyFullPage() {
        when(albums.findPage(0, AlbumBatchScanner.BATCH_SIZE)).thenReturn(fullPage());
        when(albums.findPage(1, AlbumBatchScanner.BATCH_SIZE)).thenReturn(List.of());

        List<Album> visited = new ArrayList<>();
        scanner.forEachAlbum(visited::add);

        assertThat(visited).hasSize(AlbumBatchScanner.BATCH_SIZE);
        verify(albums).findPage(0, AlbumBatchScanner.BATCH_SIZE);
        verify(albums).findPage(1, AlbumBatchScanner.BATCH_SIZE);
        verifyNoMoreInteractions(albums);
    }

    @Test
    @DisplayName("사진 수 조건은 저장소 쿼리로 내려간다")
    void photoCountFilterIsPushedToRepository() {
        when(albums.findPageWithAtLeastPhotos(5, 0, AlbumBatchScanner.BATCH_SIZE)).thenReturn(List.of(album()));

        List<Album> visited = new ArrayList<>();
        scanner.forEachAlbumWithAtLeastPhotos(5, visited::add);

        assertThat(visited).hasSize(1);
        verify(albums).findPageWithAtLeastPhotos(5, 0, AlbumBatchScanner.BATCH_SIZE);
        verifyNoMoreInteractions(albums);
    }

    private static List<Album> fullPage() {
        return IntStream.range(0, AlbumBatchScanner.BATCH_SIZE).mapToObj(index -> album()).toList();
    }

    private static Album album() {
        return Album.create(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                UUID.randomUUID().toString());
    }
}
