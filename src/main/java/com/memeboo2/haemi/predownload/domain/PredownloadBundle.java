package com.memeboo2.haemi.predownload.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 선다운로드 번들 (#48). 09:00 세션 전에 어르신 단말로 미리 전송할 오늘의 자산 묶음.
 * 카드·사진·힌트 키를 담아 힌트가 0초에 재생되도록 한다.
 */
public record PredownloadBundle(
        String elderId,
        LocalDate date,
        List<String> cardKeys,
        List<String> photoKeys,
        List<String> hintKeys,
        LocalDateTime assembledAt
) {
    public static PredownloadBundle of(String elderId, LocalDate date,
                                       List<String> cardKeys, List<String> photoKeys,
                                       List<String> hintKeys) {
        return new PredownloadBundle(
                elderId, date,
                List.copyOf(cardKeys), List.copyOf(photoKeys), List.copyOf(hintKeys),
                LocalDateTime.now());
    }

    public int totalAssets() {
        return cardKeys.size() + photoKeys.size() + hintKeys.size();
    }

    public boolean isEmpty() {
        return totalAssets() == 0;
    }
}
