package com.memeboo2.haemi.m2.domain.port;

import java.io.InputStream;

public interface SttPort {

    /**
     * 음성 파일을 텍스트로 변환한다. (STT)
     * @param audioStream 음성 파일 스트림
     * @param contentType audio/mp3, audio/m4a, audio/aac 등. 일부 모바일 클라이언트는
     *                    application/octet-stream을 보내므로 파일명과 함께 해석한다.
     * @param originalFilename 업로드 원본 파일명. contentType 폴백에만 사용한다.
     * @return 변환된 텍스트
     */
    String transcribe(InputStream audioStream, String contentType, String originalFilename);
}
