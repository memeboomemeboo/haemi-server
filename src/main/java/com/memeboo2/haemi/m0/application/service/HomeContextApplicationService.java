package com.memeboo2.haemi.m0.application.service;

import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.m0.application.dto.HomeContextResult;
import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.M0AccessDeniedException;
import com.memeboo2.haemi.m0.domain.model.M0NotFoundException;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import com.memeboo2.haemi.m0.domain.repository.FamilyGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 홈 API가 토큰 주체에서 현재 가족 그룹과 어르신 프로필을 해석한다.
 * 클라이언트가 elderId·groupId를 보낼 필요가 없도록 연결 관계는 서버에서만 조회한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeContextApplicationService {

    private final ElderRepository elders;
    private final FamilyGroupRepository groups;

    public HomeContextResult resolve(UUID memberId, MemberRole role) {
        Elder elder = switch (role) {
            case ELDER -> elders.findByMemberId(memberId)
                    .orElseThrow(() -> new M0NotFoundException("연결된 어르신 프로필"));
            case FAMILY -> groups.findActiveByMemberId(memberId)
                    .flatMap(group -> elders.findByGroupId(group.getId()))
                    .orElseThrow(() -> new M0NotFoundException("가족 그룹에 연결된 어르신 프로필"));
            case INSTITUTION_ADMIN -> throw new M0AccessDeniedException("기관 관리자 계정은 가족 홈을 사용할 수 없어요.");
        };
        return HomeContextResult.from(memberId, role, elder);
    }
}
