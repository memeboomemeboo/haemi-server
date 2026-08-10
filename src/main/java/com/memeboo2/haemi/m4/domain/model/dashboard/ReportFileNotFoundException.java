package com.memeboo2.haemi.m4.domain.model.dashboard;

/**
 * 리포트 행은 있는데 PDF 파일이 없을 때 (#93).
 *
 * <p>재배포로 파일이 사라졌던 시절에 만들어진 행이 대표적이다. 응답 본문을 쓰다 깨지는 대신
 * 원인을 알 수 있는 404를 주기 위해 구분한다.
 */
public class ReportFileNotFoundException extends RuntimeException {

    public ReportFileNotFoundException(String reportId) {
        super("리포트 파일을 찾을 수 없어요. 리포트를 다시 만들어 주세요. reportId=" + reportId);
    }
}
