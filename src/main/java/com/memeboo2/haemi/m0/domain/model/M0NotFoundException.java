package com.memeboo2.haemi.m0.domain.model;

public class M0NotFoundException extends RuntimeException {
    public M0NotFoundException(String resource) {
        super(resource + "을(를) 찾을 수 없습니다.");
    }
}
