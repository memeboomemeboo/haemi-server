package com.memeboo2.haemi.m0.application.service;

import com.memeboo2.haemi.m0.application.dto.ElderStatusResult;
import com.memeboo2.haemi.m0.domain.event.ElderBereavedEvent;
import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.ElderStatus;
import com.memeboo2.haemi.m0.domain.model.FamilyGroup;
import com.memeboo2.haemi.m0.domain.model.Gender;
import com.memeboo2.haemi.m0.domain.model.ResidenceType;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import com.memeboo2.haemi.m0.domain.repository.FamilyGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElderStatusApplicationServiceTest {

    @Mock ElderRepository elders;
    @Mock FamilyGroupRepository groups;
    @Mock ApplicationEventPublisher eventPublisher;

    ElderStatusApplicationService service;

    private final UUID actorId = UUID.randomUUID();
    private Elder elder;

    @BeforeEach
    void setUp() {
        service = new ElderStatusApplicationService(elders, groups, eventPublisher);
        ReflectionTestUtils.setField(service, "silentDays", 7);
        ReflectionTestUtils.setField(service, "recoveryWindowHours", 48);

        elder = Elder.create(UUID.randomUUID(), null, "김어르신", 1945,
                Gender.FEMALE, ResidenceType.HOME_WITH_FAMILY);
        when(elders.findById(elder.getId())).thenReturn(Optional.of(elder));
        when(groups.findById(elder.getGroupId())).thenReturn(Optional.of(mock(FamilyGroup.class)));
        when(elders.save(any(Elder.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("사별 확정 시 DECEASED로 전이하고 사별 이벤트를 발행한다")
    void confirmBereavement_publishesEvent() {
        service.requestBereavement(actorId, elder.getId());

        ElderStatusResult result = service.confirmBereavement(actorId, elder.getId());

        assertThat(result.status()).isEqualTo(ElderStatus.DECEASED);
        assertThat(result.dispatchable()).isFalse();
        verify(eventPublisher).publishEvent(any(ElderBereavedEvent.class));
    }

    @Test
    @DisplayName("생존 상태 전이는 이벤트 없이 상태만 바꾼다")
    void changeStatus_noEvent() {
        ElderStatusResult result = service.changeStatus(actorId, elder.getId(), ElderStatus.HOSPITALIZED);

        assertThat(result.status()).isEqualTo(ElderStatus.HOSPITALIZED);
        verify(eventPublisher, never()).publishEvent(any());
    }
}
