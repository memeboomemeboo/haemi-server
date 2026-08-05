package com.memeboo2.haemi.offline.domain.port;

import com.memeboo2.haemi.offline.domain.OfflineSessionResult;

/**
 * 최초 수신된 오프라인 세션 결과를 다운스트림(난이도 프로필·리포트 등)에 반영하는 포트.
 * 운영에서는 m3 반영 구현으로 교체한다.
 */
public interface OfflineResultApplyPort {

    void apply(OfflineSessionResult result);
}
