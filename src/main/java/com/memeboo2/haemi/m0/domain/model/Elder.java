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

    /**
     * Mode A에서 어르신 본인이 로그인할 때 사용하는 계정이다.
     * Mode B에서는 보호자 계정으로 대행할 수 있으므로 기기 토큰의 소유자와는 별개다.
     */
    @Column(name = "member_id", unique = true, columnDefinition = "uuid")
    private UUID memberId;

    @Column(name = "org_id", length = 100)
    private String orgId;

    /**
     * 어르신 참여 로그인(F0-01-E)에서 입력받은 전화번호의 해시.
     * OTP 없이 식별·중복 등록 감지에만 쓰므로 원본은 보관하지 않는다.
     */
    @Column(name = "phone_hash", length = 64)
    private String phoneHash;

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

    // 사별 생명주기 (F0-05)
    @Column(name = "bereavement_requested_at")
    private LocalDateTime bereavementRequestedAt;

    @Column(name = "bereaved_at")
    private LocalDateTime bereavedAt;

    @Column(name = "silent_until")
    private LocalDateTime silentUntil;

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

    /** 참여 로그인에서 확인한 전화번호 해시를 기록한다. */
    public void registerPhoneHash(String phoneHash) {
        if (phoneHash == null || phoneHash.isBlank()) {
            throw new M0ValidationException("전화번호를 입력해주세요.");
        }
        this.phoneHash = phoneHash;
        touch();
    }

    /** 초대 코드가 이 어르신 본인에게 쓰였는지 교차 검증한다 (EX-F001E-02). */
    public boolean matchesName(String candidate) {
        return candidate != null && name.equals(candidate.trim());
    }

    /** Mode A 어르신 계정 연결. 계정 자체의 역할 검증은 애플리케이션 계층에서 수행한다. */
    public void linkMember(UUID memberId) {
        if (memberId == null) {
            throw new M0ValidationException("연결할 어르신 계정은 필수예요.");
        }
        this.memberId = memberId;
        touch();
    }

    // ── 일반 상태 전이 (생존 상태 간) ──
    public void transitionTo(ElderStatus target) {
        if (target == null) {
            throw new M0ValidationException("어르신 상태는 필수예요.");
        }
        if (target == ElderStatus.DECEASED) {
            throw new M0ValidationException("사별 처리는 2단계 확인(requestBereavement→confirmBereavement)으로만 가능해요.");
        }
        if (!status.canTransitionTo(target)) {
            throw new M0ValidationException("허용되지 않은 상태 전이예요: " + status + " → " + target);
        }
        this.status = target;
        touch();
    }

    // ── 사별 처리 1단계: 요청(2단계 확인 대기) ──
    public void requestBereavement(LocalDateTime now) {
        if (status == ElderStatus.DECEASED || status == ElderStatus.MEMORIAL) {
            throw new M0ValidationException("이미 사별 처리된 어르신이에요.");
        }
        this.bereavementRequestedAt = now;
        touch();
    }

    // ── 사별 처리 2단계: 확정 → DECEASED, 무음기간 시작 ──
    public void confirmBereavement(LocalDateTime now, int silentDays) {
        if (bereavementRequestedAt == null) {
            throw new M0ValidationException("사별 확정 전에 요청(1단계)이 필요해요.");
        }
        if (!status.canTransitionTo(ElderStatus.DECEASED)) {
            throw new M0ValidationException("현재 상태에서 사별 처리할 수 없어요: " + status);
        }
        this.status = ElderStatus.DECEASED;
        this.bereavedAt = now;
        this.silentUntil = now.plusDays(silentDays);
        this.bereavementRequestedAt = null;
        touch();
    }

    // ── 48시간 내 사별 오등록 복구 → ACTIVE ──
    public void recoverFromBereavement(LocalDateTime now, int recoveryWindowHours) {
        if (status != ElderStatus.DECEASED) {
            throw new M0ValidationException("사별 상태에서만 복구할 수 있어요.");
        }
        if (bereavedAt == null || now.isAfter(bereavedAt.plusHours(recoveryWindowHours))) {
            throw new M0ValidationException("사별 오등록 복구 기간(" + recoveryWindowHours + "시간)이 지났어요.");
        }
        this.status = ElderStatus.ACTIVE;
        this.bereavedAt = null;
        this.silentUntil = null;
        touch();
    }

    // ── 무음기간 경과 후 memorial 기억 보관함으로 봉인 ──
    public void enshrineMemorial(LocalDateTime now) {
        if (status != ElderStatus.DECEASED) {
            throw new M0ValidationException("사별 상태에서만 memorial로 전환할 수 있어요.");
        }
        if (isInSilentPeriod(now)) {
            throw new M0ValidationException("무음기간이 끝난 뒤에 memorial로 전환할 수 있어요.");
        }
        this.status = ElderStatus.MEMORIAL;
        touch();
    }

    // ── 발송 파이프라인 최종 상태 검증 (EX-F005-01): 생존·비무음일 때만 발송 ──
    public boolean isDispatchable(LocalDateTime now) {
        return (status == ElderStatus.ACTIVE || status == ElderStatus.DECLINING)
                && !isInSilentPeriod(now);
    }

    public boolean isInSilentPeriod(LocalDateTime now) {
        return silentUntil != null && now.isBefore(silentUntil);
    }

    public boolean isMemorialArchiveOnly() {
        return status == ElderStatus.MEMORIAL;
    }

    public boolean isBereavementPending() {
        return bereavementRequestedAt != null;
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
