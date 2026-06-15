package com.memeboo2.haemi.m2.domain.model.post;

public class EmptyPostContentException extends RuntimeException {
    public EmptyPostContentException() {
        super("추억 내용을 입력해주세요. 텍스트, 사진, 음성 중 하나 이상이 필요합니다.");
    }
}
