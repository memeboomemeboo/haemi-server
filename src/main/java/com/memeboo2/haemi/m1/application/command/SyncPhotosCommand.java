package com.memeboo2.haemi.m1.application.command;

import java.util.List;

public record SyncPhotosCommand(
        String albumId,
        String uploadedBy,
        List<SavePhotoCommand> photos
) {}
