package com.memeboo2.haemi.m1.application.service;

public class ReminiscenceContentNotFoundException extends RuntimeException {
    public ReminiscenceContentNotFoundException(String id) {
        super("회상 콘텐츠를 찾을 수 없습니다. id=" + id);
    }
}
