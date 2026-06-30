package com.memeboo2.haemi.m5.domain.model.care;

public class WalkRoutineNotFoundException extends RuntimeException {
    public WalkRoutineNotFoundException(String routineId) {
        super("산책 루틴을 찾을 수 없습니다: " + routineId);
    }
}
