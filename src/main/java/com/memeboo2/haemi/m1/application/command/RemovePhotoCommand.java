package com.memeboo2.haemi.m1.application.command;

public record RemovePhotoCommand(
        String albumId,
        String photoId,
        String requestingMemberId
) {}
