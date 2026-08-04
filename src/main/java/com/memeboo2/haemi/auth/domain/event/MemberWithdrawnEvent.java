package com.memeboo2.haemi.auth.domain.event;

import java.util.UUID;

public record MemberWithdrawnEvent(UUID memberId) {
}
