package com.memeboo2.haemi.m1.infrastructure.storage;

import com.memeboo2.haemi.m1.domain.port.PhotoStoragePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@Component
public class LocalPhotoStorageAdapter implements PhotoStoragePort {

    private final Path uploadRoot;

    public LocalPhotoStorageAdapter(@Value("${haemi.storage.upload-path}") String uploadPath) {
        this.uploadRoot = Paths.get(uploadPath);
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException e) {
            throw new IllegalStateException("업로드 디렉토리 생성 실패: " + uploadPath, e);
        }
    }

    @Override
    public String store(InputStream inputStream, String originalFilename, String contentType) throws Exception {
        String extension = extractExtension(originalFilename);
        String storageKey = UUID.randomUUID() + extension;
        Path target = uploadRoot.resolve(storageKey);

        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        log.debug("사진 저장: storageKey={}", storageKey);
        return storageKey;
    }

    @Override
    public String getAccessUrl(String storageKey) {
        return "/api/v1/photos/files/" + storageKey;
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(uploadRoot.resolve(storageKey));
        } catch (IOException e) {
            log.warn("파일 삭제 실패: storageKey={}", storageKey, e);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
