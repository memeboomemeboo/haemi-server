package com.memeboo2.haemi.predownload.domain.port;

import com.memeboo2.haemi.predownload.domain.PredownloadBundle;

/**
 * 선다운로드 번들을 어르신 단말로 미리 전송(prefetch)하는 포트.
 * 운영에서는 CDN 프리페치 / 디바이스 캐시 푸시 구현으로 교체한다.
 */
public interface PredownloadDispatchPort {

    void dispatch(PredownloadBundle bundle);
}
