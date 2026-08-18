package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ElderRepositoryAdapter implements ElderRepository {

    private final JpaElderRepository elders;

    @Override
    public Elder save(Elder elder) {
        return elders.save(elder);
    }

    @Override
    public Optional<Elder> findById(UUID elderId) {
        return elders.findById(elderId);
    }

    @Override
    public Optional<Elder> findByGroupId(UUID groupId) {
        return elders.findByGroupId(groupId);
    }

    @Override
    public Optional<Elder> findByMemberId(UUID memberId) {
        return elders.findByMemberId(memberId);
    }

    @Override
    public Optional<Elder> findByPhoneHash(String phoneHash) {
        return elders.findByPhoneHash(phoneHash);
    }

    @Override
    public boolean existsByGroupId(UUID groupId) {
        return elders.existsByGroupId(groupId);
    }
}
