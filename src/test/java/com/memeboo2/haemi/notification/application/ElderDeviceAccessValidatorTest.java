package com.memeboo2.haemi.notification.application;

import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.FamilyGroup;
import com.memeboo2.haemi.m0.domain.model.FamilyRelation;
import com.memeboo2.haemi.m0.domain.model.Gender;
import com.memeboo2.haemi.m0.domain.model.NotificationPreference;
import com.memeboo2.haemi.m0.domain.model.ResidenceType;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import com.memeboo2.haemi.m0.domain.repository.FamilyGroupRepository;
import com.memeboo2.haemi.auth.domain.repository.MemberRepository;
import com.memeboo2.haemi.m4.domain.repository.AlertRecipientSettingRepository;
import com.memeboo2.haemi.auth.domain.model.Member;
import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.m4.domain.model.dashboard.AlertRecipientSetting;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ElderDeviceAccessValidatorTest {

    private final ElderRepository elders = mock(ElderRepository.class);
    private final FamilyGroupRepository groups = mock(FamilyGroupRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final AlertRecipientSettingRepository alertRecipients = mock(AlertRecipientSettingRepository.class);
    private final ElderDeviceAccessValidator validator = new ElderDeviceAccessValidator(
            elders, groups, members, alertRecipients);

    @Test
    void allowsFamilyMemberToBindCaregiverSessionOnElderPhone() {
        UUID caregiverId = UUID.randomUUID();
        FamilyGroup group = FamilyGroup.create(caregiverId, FamilyRelation.DAUGHTER, NotificationPreference.ALL);
        Elder elder = Elder.create(group.getId(), null, "김해미", 1940, Gender.FEMALE, ResidenceType.HOME_ALONE);
        when(elders.findById(elder.getId())).thenReturn(Optional.of(elder));
        when(groups.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatCode(() -> validator.requireCanBind(caregiverId, elder.getId())).doesNotThrowAnyException();
    }

    @Test
    void rejectsUnrelatedAccountFromBindingAnotherElderDevice() {
        UUID caregiverId = UUID.randomUUID();
        FamilyGroup group = FamilyGroup.create(caregiverId, FamilyRelation.DAUGHTER, NotificationPreference.ALL);
        Elder elder = Elder.create(group.getId(), null, "김해미", 1940, Gender.FEMALE, ResidenceType.HOME_ALONE);
        when(elders.findById(elder.getId())).thenReturn(Optional.of(elder));
        when(groups.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> validator.requireCanBind(UUID.randomUUID(), elder.getId()))
                .isInstanceOf(DeviceTokenAccessDeniedException.class);
    }

    @Test
    void allowsAssignedInstitutionManagerToBindCaregiverSessionOnElderPhone() {
        UUID caregiverId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        FamilyGroup group = FamilyGroup.create(caregiverId, FamilyRelation.DAUGHTER, NotificationPreference.ALL);
        Elder elder = Elder.create(group.getId(), null, "김해미", 1940, Gender.FEMALE, ResidenceType.HOME_ALONE);
        Member manager = Member.create("manager@haemi.test", "encoded", "기관담당", MemberRole.INSTITUTION_ADMIN);
        AlertRecipientSetting setting = AlertRecipientSetting.createOrUpdate(
                null, elder.getId().toString(), caregiverId.toString(), Set.of(managerId.toString()));
        when(elders.findById(elder.getId())).thenReturn(Optional.of(elder));
        when(groups.findById(group.getId())).thenReturn(Optional.of(group));
        when(members.findById(managerId)).thenReturn(Optional.of(manager));
        when(alertRecipients.findByElderId(elder.getId().toString())).thenReturn(Optional.of(setting));

        assertThatCode(() -> validator.requireCanBind(managerId, elder.getId())).doesNotThrowAnyException();
    }
}
