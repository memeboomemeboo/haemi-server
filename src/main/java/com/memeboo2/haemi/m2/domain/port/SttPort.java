package com.memeboo2.haemi.m2.domain.port;

import java.io.InputStream;

public interface SttPort {

    /**
     * 음성 파일을 텍스트로 변환한다. (STT)
     * @param audioStream 음성 파일 스트림
     * @param contentType audio/mp3, audio/m4a, audio/aac 등
     * @return 변환된 텍스트
     */
    String transcribe(InputStream audioStream, String contentType);
}
