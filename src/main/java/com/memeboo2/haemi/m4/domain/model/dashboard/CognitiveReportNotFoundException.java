package com.memeboo2.haemi.m4.domain.model.dashboard;

public class CognitiveReportNotFoundException extends RuntimeException {

    public CognitiveReportNotFoundException(String reportId) {
        super("인지 리포트를 찾을 수 없습니다. reportId=" + reportId);
    }
}
