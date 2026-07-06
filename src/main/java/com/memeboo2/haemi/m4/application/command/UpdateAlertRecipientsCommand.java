package com.memeboo2.haemi.m4.application.command;

import java.util.Set;

public record UpdateAlertRecipientsCommand(
        String elderId,
        String primaryCaregiverMemberId,
        Set<String> institutionManagerMemberIds
) {}
