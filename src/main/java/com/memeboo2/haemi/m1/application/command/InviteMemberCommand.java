package com.memeboo2.haemi.m1.application.command;

public record InviteMemberCommand(
        String albumId,
        String inviterId,
        String inviteeId
) {}
