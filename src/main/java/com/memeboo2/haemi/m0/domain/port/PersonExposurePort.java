package com.memeboo2.haemi.m0.domain.port;

import com.memeboo2.haemi.m0.domain.model.PersonContentTense;

import java.util.List;
import java.util.UUID;

/** A의 콘텐츠 생성과 B의 선다운로드 무효화가 공유하는 인물 노출 계약. */
public interface PersonExposurePort {
    List<PhotoPersonExposure> findByPhotoId(UUID photoId);

    record PhotoPersonExposure(UUID personId, String name, String nickname,
                               PersonContentTense tense, boolean nameUsable) {
        public boolean isPhotoEligible() {
            return tense != PersonContentTense.EXCLUDED;
        }
    }
}
