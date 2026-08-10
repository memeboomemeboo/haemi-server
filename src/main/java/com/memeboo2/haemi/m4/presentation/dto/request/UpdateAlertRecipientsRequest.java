package com.memeboo2.haemi.m4.presentation.dto.request;

import java.util.Set;

public record UpdateAlertRecipientsRequest(
        Set<String> institutionManagerMemberIds
) {}
