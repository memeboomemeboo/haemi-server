package com.memeboo2.haemi.m1.domain.port;

import java.io.InputStream;

public interface PhotoStoragePort {

    // 파일 저장 후 storageKey 반환
    String store(InputStream inputStream, String originalFilename, String contentType) throws Exception;

    // storageKey로 접근 가능한 URL 반환
    String getAccessUrl(String storageKey);

    void delete(String storageKey);
}
