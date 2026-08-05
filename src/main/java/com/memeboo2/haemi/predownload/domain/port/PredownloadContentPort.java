package com.memeboo2.haemi.predownload.domain.port;

import com.memeboo2.haemi.predownload.domain.PredownloadBundle;

import java.time.LocalDate;
import java.util.List;

/**
 * 선다운로드 대상 콘텐츠 조회 포트. 08:00에 생성된 오늘의 카드·사진·힌트를 어르신별로 조립한다.
 */
public interface PredownloadContentPort {

    List<String> eligibleElderIds(LocalDate date);

    PredownloadBundle assemble(String elderId, LocalDate date);
}
