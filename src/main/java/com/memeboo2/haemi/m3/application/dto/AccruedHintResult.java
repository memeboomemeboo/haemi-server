package com.memeboo2.haemi.m3.application.dto;

import com.memeboo2.haemi.m3.domain.model.hint.AccrualSource;
import com.memeboo2.haemi.m3.domain.model.hint.AccruedHint;
import com.memeboo2.haemi.m3.domain.model.hint.HintTier;

public record AccruedHintResult(
        String id,
        String elderId,
        String photoId,
        AccrualSource source,
        HintTier tier,
        String authorName,
        String text
) {
    public static AccruedHintResult from(AccruedHint hint) {
        return new AccruedHintResult(
                hint.getId().toString(),
                hint.getElderId(),
                hint.getPhotoId() == null ? null : hint.getPhotoId().toString(),
                hint.getSource(),
                hint.tier(),
                hint.getAuthorName(),
                hint.getText()
        );
    }
}
