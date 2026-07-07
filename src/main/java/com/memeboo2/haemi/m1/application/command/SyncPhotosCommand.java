package com.memeboo2.haemi.m1.application.command;

import com.memeboo2.haemi.m1.domain.model.album.NetworkType;

import java.util.List;

public record SyncPhotosCommand(
        String albumId,
        String uploadedBy,
        List<SavePhotoCommand> photos,
        boolean wifiOnly,
        NetworkType networkType,
        Integer batteryLevel,
        boolean backgroundSync
) {}
