package com.memeboo2.haemi.m3.application.service;

import com.memeboo2.haemi.common.exception.DomainValidationException;

import com.memeboo2.haemi.m3.application.command.UpdateDifficultyPolicyCommand;
import com.memeboo2.haemi.m3.application.dto.DifficultyPolicyResult;
import com.memeboo2.haemi.m3.domain.model.training.DifficultyPolicy;
import com.memeboo2.haemi.m3.domain.repository.DifficultyPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional
public class DifficultyPolicyApplicationService {

    private final DifficultyPolicyRepository repository;

    @Transactional(readOnly = true)
    public List<DifficultyPolicyResult> getPolicies() {
        List<DifficultyPolicy> policies = repository.findAll();
        if (policies.size() == 5) {
            return policies.stream().map(DifficultyPolicyResult::from).toList();
        }
        return IntStream.rangeClosed(1, 5)
                .mapToObj(level -> policies.stream()
                        .filter(policy -> policy.getLevel() == level)
                        .findFirst()
                        .orElseGet(() -> DifficultyPolicy.defaultFor(level)))
                .map(DifficultyPolicyResult::from)
                .toList();
    }

    public DifficultyPolicyResult updatePolicy(UpdateDifficultyPolicyCommand command) {
        if (command.level() < 1 || command.level() > 5) {
            throw new DomainValidationException("난이도 레벨은 1~5 범위여야 합니다.");
        }
        DifficultyPolicy policy = repository.findByLevel(command.level())
                .orElseGet(() -> DifficultyPolicy.defaultFor(command.level()));
        policy.update(
                command.maxAverageResponseSeconds(),
                command.increaseAccuracyThreshold(),
                command.decreaseAccuracyThreshold(),
                command.questionTypes(),
                command.reviewedBy(),
                command.reviewedDate()
        );
        return DifficultyPolicyResult.from(repository.save(policy));
    }
}
