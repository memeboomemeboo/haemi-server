package com.memeboo2.haemi.m0.infrastructure.event;

import com.memeboo2.haemi.auth.domain.event.MemberWithdrawnEvent;
import com.memeboo2.haemi.m0.domain.repository.FamilyGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** F0-01 EX-F001-04: 대표 보호자 탈퇴 시 그룹 데이터 고아화를 막는다. */
@Component
@RequiredArgsConstructor
public class OwnerWithdrawalListener {

    private final FamilyGroupRepository groups;

    @EventListener
    @Transactional
    public void transferOrHold(MemberWithdrawnEvent event) {
        groups.findByOwnerMemberId(event.memberId()).ifPresent(group -> {
            group.handleOwnerWithdrawal(event.memberId());
            groups.save(group);
        });
    }
}
