package com.memeboo2.haemi.m1.domain.model.album;

public class PhotoFileSizeExceededException extends RuntimeException {
    public PhotoFileSizeExceededException(long sizeBytes) {
        super("20MB 이하 사진만 저장 가능합니다. size=" + sizeBytes + " bytes");
    }
}
