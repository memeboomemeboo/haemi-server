package com.memeboo2.haemi.m1.application.command;

import java.util.List;

public record UpdatePhotoMemoCommand(
        String albumId,
        String photoId,
        String timePeriod,
        String locationText,
        String memo,
        List<PersonTagItem> personTags
) {
    public record PersonTagItem(String memberId, String memberName) {}
}
