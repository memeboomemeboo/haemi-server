package com.memeboo2.haemi.m2.domain.port;

import java.io.InputStream;

/**
 * M2 전용 미디어(사진·음성) 저장 포트.
 * 실 서비스에서는 S3 등 오브젝트 스토리지로 교체한다.
 */
public interface MediaStoragePort {

    String store(InputStream inputStream, String originalFilename, String contentType) throws Exception;

    String getAccessUrl(String storageKey);

    void delete(String storageKey);
}
