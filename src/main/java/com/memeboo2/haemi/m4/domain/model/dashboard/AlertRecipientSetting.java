package com.memeboo2.haemi.m4.domain.model.dashboard;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "cognitive_alert_recipient_settings",
        uniqueConstraints = @UniqueConstraint(columnNames = "elder_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlertRecipientSetting {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "elder_id", nullable = false)
    private String elderId;

    @Column(name = "primary_caregiver_member_id", nullable = false)
    private String primaryCaregiverMemberId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cognitive_alert_institution_managers",
            joinColumns = @JoinColumn(name = "setting_id"))
    @Column(name = "member_id", nullable = false)
    private Set<String> institutionManagerMemberIds = new LinkedHashSet<>();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static AlertRecipientSetting createOrUpdate(
            AlertRecipientSetting existing,
            String elderId,
            String primaryCaregiverMemberId,
            Set<String> institutionManagerMemberIds
    ) {
        if (elderId == null || elderId.isBlank()) {
            throw new IllegalArgumentException("어르신 ID는 필수입니다.");
        }
        if (primaryCaregiverMemberId == null || primaryCaregiverMemberId.isBlank()) {
            throw new IllegalArgumentException("주 보호자 회원 ID는 필수입니다.");
        }
        AlertRecipientSetting setting = existing != null ? existing : new AlertRecipientSetting();
        if (setting.id == null) {
            setting.id = UUID.randomUUID();
            setting.elderId = elderId.trim();
        }
        setting.primaryCaregiverMemberId = primaryCaregiverMemberId.trim();
        setting.institutionManagerMemberIds.clear();
        if (institutionManagerMemberIds != null) {
            institutionManagerMemberIds.stream()
                    .filter(id -> id != null && !id.isBlank())
                    .map(String::trim)
                    .filter(id -> !id.equals(setting.primaryCaregiverMemberId))
                    .forEach(setting.institutionManagerMemberIds::add);
        }
        setting.updatedAt = LocalDateTime.now();
        return setting;
    }

    public Set<String> recipientMemberIds() {
        Set<String> recipients = new LinkedHashSet<>();
        recipients.add(primaryCaregiverMemberId);
        recipients.addAll(institutionManagerMemberIds);
        return Collections.unmodifiableSet(recipients);
    }

    public Set<String> getInstitutionManagerMemberIds() {
        return Collections.unmodifiableSet(institutionManagerMemberIds);
    }
}
