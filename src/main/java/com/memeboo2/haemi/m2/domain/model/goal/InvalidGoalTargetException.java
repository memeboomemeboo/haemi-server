package com.memeboo2.haemi.m2.domain.model.goal;

public class InvalidGoalTargetException extends RuntimeException {
    public InvalidGoalTargetException(int target) {
        super("그룹 협력 목표 값은 1 이상이어야 합니다: " + target);
    }
}
