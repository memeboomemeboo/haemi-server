package com.memeboo2.haemi.notification.infrastructure;

import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.FamilyGroup;
import com.memeboo2.haemi.m0.domain.model.FamilyRelation;
import com.memeboo2.haemi.m0.domain.model.Gender;
import com.memeboo2.haemi.m0.domain.model.NotificationPreference;
import com.memeboo2.haemi.m0.domain.model.ResidenceType;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import com.memeboo2.haemi.m0.domain.repository.FamilyGroupRepository;
import com.memeboo2.haemi.notification.domain.DevicePlatform;
import com.memeboo2.haemi.notification.domain.DeviceToken;
import com.memeboo2.haemi.notification.domain.repository.DeviceTokenRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OnlineFamilyMemberQueryAdapterTest {

    @Test
    void selectsOnlyTheMostRecentlyActiveFamilyApp() {
        ElderRepository elders = mock(ElderRepository.class);
        FamilyGroupRepository groups = mock(FamilyGroupRepository.class);
        DeviceTokenRepository tokens = mock(DeviceTokenRepository.class);
        UUID memberId = UUID.randomUUID();
        FamilyGroup group = FamilyGroup.create(memberId, FamilyRelation.SON, NotificationPreference.ALL);
        Elder elder = Elder.create(group.getId(), null, "김어르신", 1945, Gender.FEMALE, ResidenceType.HOME_WITH_FAMILY);
        DeviceToken current = DeviceToken.register("current", memberId, DevicePlatform.ANDROID, LocalDateTime.now());
        DeviceToken stale = DeviceToken.register("stale", UUID.randomUUID(), DevicePlatform.IOS,
                LocalDateTime.now().minusMinutes(3));
        when(elders.findById(elder.getId())).thenReturn(Optional.of(elder));
        when(groups.findById(group.getId())).thenReturn(Optional.of(group));
        when(tokens.findByMemberIds(List.of(memberId))).thenReturn(List.of(stale, current));

        var result = new OnlineFamilyMemberQueryAdapter(elders, groups, tokens)
                .findOneOnlineMemberId(elder.getId().toString(), LocalDateTime.now().minusMinutes(2));

        assertThat(result).contains(memberId);
    }
}
