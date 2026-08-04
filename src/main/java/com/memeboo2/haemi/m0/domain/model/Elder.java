package com.memeboo2.haemi.m0.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

@Entity
@Table(name = "elders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Elder {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "group_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID groupId;

    @Column(name = "org_id", length = 100)
    private String orgId;

    @Column(nullable = false, length = 10)
    private String name;

    @Column(name = "birth_year", nullable = false)
    private int birthYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "residence_type", nullable = false, length = 30)
    private ResidenceType residenceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_mode", nullable = false, length = 20)
    private ElderAccessMode accessMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ElderStatus status;

    @Column(name = "personalization_level", nullable = false)
    private int personalizationLevel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Elder create(UUID groupId, String orgId, String name, int birthYear,
                               Gender gender, ResidenceType residenceType) {
        validate(name, birthYear, gender, residenceType);
        Elder elder = new Elder();
        elder.id = UUID.randomUUID();
        elder.groupId = groupId;
        elder.orgId = blankToNull(orgId);
        elder.name = name.trim();
        elder.birthYear = birthYear;
        elder.gender = gender;
        elder.residenceType = residenceType;
        elder.accessMode = ElderAccessMode.UNSET;
        elder.status = ElderStatus.ACTIVE;
        elder.personalizationLevel = 2;
        elder.createdAt = LocalDateTime.now();
        elder.updatedAt = elder.createdAt;
        return elder;
    }

    public void updateProfile(String name, Integer birthYear, Gender gender, ResidenceType residenceType,
                              String orgId) {
        String effectiveName = name != null ? name : this.name;
        int effectiveBirthYear = birthYear != null ? birthYear : this.birthYear;
        Gender effectiveGender = gender != null ? gender : this.gender;
        ResidenceType effectiveResidenceType = residenceType != null ? residenceType : this.residenceType;
        validate(effectiveName, effectiveBirthYear, effectiveGender, effectiveResidenceType);
        this.name = effectiveName.trim();
        this.birthYear = effectiveBirthYear;
        this.gender = effectiveGender;
        this.residenceType = effectiveResidenceType;
        if (orgId != null) {
            this.orgId = blankToNull(orgId);
        }
        touch();
    }

    /** M0 B 담당의 접근 모드 서비스가 호출하는 상태 없는 계약. */
    public void changeAccessMode(ElderAccessMode accessMode) {
        if (accessMode == null || accessMode == ElderAccessMode.UNSET) {
            throw new M0ValidationException("접근 모드는 A 또는 B로 설정해야 해요.");
        }
        this.accessMode = accessMode;
        touch();
    }

    public void changeStatus(ElderStatus status) {
        if (status == null) {
            throw new M0ValidationException("어르신 상태는 필수예요.");
        }
        this.status = status;
        touch();
    }

    public double calculateCompleteness(Collection<LifeStory> lifeStories) {
        long recommendedCategoryCount = lifeStories.stream()
                .map(LifeStory::getCategory)
                .distinct()
                .count();
        double maximum = 4.0 + LifeStoryCategory.values().length * 0.6;
        return Math.min(1.0, (4.0 + recommendedCategoryCount * 0.6) / maximum);
    }

    private static void validate(String name, int birthYear, Gender gender, ResidenceType residenceType) {
        if (name == null || name.trim().length() < 2 || name.trim().length() > 10) {
            throw new M0ValidationException("어르신 성함은 2~10자로 입력해주세요.");
        }
        if (birthYear < 1920 || birthYear > 1970) {
            throw new M0ValidationException("출생연도는 1920~1970년 사이여야 해요.");
        }
        if (gender == null || residenceType == null) {
            throw new M0ValidationException("성별과 거주 형태는 필수예요.");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void touch() {
        updatedAt = LocalDateTime.now();
    }
}
