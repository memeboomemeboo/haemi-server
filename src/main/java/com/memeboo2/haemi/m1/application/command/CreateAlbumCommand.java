package com.memeboo2.haemi.m1.application.command;

public record CreateAlbumCommand(
        String elderProfileId,
        String groupId,
        String ownerMemberId
) {}
