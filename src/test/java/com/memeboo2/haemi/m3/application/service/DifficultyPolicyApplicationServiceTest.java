package com.memeboo2.haemi.m3.application.service;

import com.memeboo2.haemi.m3.application.command.UpdateDifficultyPolicyCommand;
import com.memeboo2.haemi.m3.domain.model.training.DifficultyPolicy;
import com.memeboo2.haemi.m3.domain.model.training.QuestionType;
import com.memeboo2.haemi.m3.domain.repository.DifficultyPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DifficultyPolicyApplicationServiceTest {

    @Mock DifficultyPolicyRepository repository;

    private DifficultyPolicyApplicationService service;

    @BeforeEach
    void setUp() {
        service = new DifficultyPolicyApplicationService(repository);
    }

    @Test
    void returnsDefaultsForMissingPolicyLevels() {
        when(repository.findAll()).thenReturn(List.of(DifficultyPolicy.defaultFor(2)));

        assertThat(service.getPolicies())
                .extracting(policy -> policy.level())
                .containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void updatesAndPersistsExpertPolicy() {
        DifficultyPolicy policy = DifficultyPolicy.defaultFor(2);
        when(repository.findByLevel(2)).thenReturn(Optional.of(policy));
        when(repository.save(policy)).thenReturn(policy);

        var result = service.updatePolicy(new UpdateDifficultyPolicyCommand(
                2,
                27.0,
                0.85,
                0.35,
                EnumSet.of(QuestionType.PERSON_RECALL, QuestionType.PLACE_MATCH),
                "expert@haemi.kr",
                LocalDate.of(2026, 7, 6)
        ));

        assertThat(result.maxAverageResponseSeconds()).isEqualTo(27.0);
        assertThat(result.reviewedBy()).isEqualTo("expert@haemi.kr");
        verify(repository).save(policy);
    }
}
