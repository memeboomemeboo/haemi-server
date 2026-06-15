package com.memeboo2.haemi.m2.infrastructure.storage;

import com.memeboo2.haemi.m2.domain.port.MediaStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 로컬 파일시스템 기반 미디어 저장 어댑터 (개발/테스트 환경 전용).
 * 운영 환경에서는 S3 등 오브젝트 스토리지 구현체로 교체한다.
 */
@Component
public class LocalMediaStorageAdapter implements MediaStoragePort {

    private final Path basePath;

    public LocalMediaStorageAdapter(
            @Value("${haemi.storage.upload-path}") String uploadPath) {
        this.basePath = Paths.get(uploadPath, "media");
    }

    @Override
    public String store(InputStream inputStream, String originalFilename, String contentType) throws Exception {
        Files.createDirectories(basePath);
        String ext = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                : "";
        String key = UUID.randomUUID() + ext;
        Path dest = basePath.resolve(key);
        try {
            Files.copy(inputStream, dest);
        } catch (IOException e) {
            throw new RuntimeException("미디어 파일 저장 실패: " + e.getMessage(), e);
        }
        return key;
    }

    @Override
    public String getAccessUrl(String storageKey) {
        return "/media/" + storageKey;
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(basePath.resolve(storageKey));
        } catch (IOException ignored) {
            // 삭제 실패 시 로그만 남기고 무시
        }
    }
}
