package com.memeboo2.haemi.m0.domain.model;

import com.memeboo2.haemi.m0.domain.event.PersonSafetyChangedEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "persons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Person extends AbstractAggregateRoot<Person> {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "group_id", nullable = false, columnDefinition = "uuid")
    private UUID groupId;

    @Column(nullable = false, length = 10)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FamilyRelation relation;

    @Enumerated(EnumType.STRING)
    @Column(name = "life_status", nullable = false, length = 20)
    private PersonLifeStatus lifeStatus;

    @Column(name = "deceased_at")
    private LocalDate deceasedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PersonVisibility visibility;

    @Column(length = 30)
    private String nickname;

    @Column(name = "profile_photo_id", columnDefinition = "uuid")
    private UUID profilePhotoId;

    @Column(name = "linked_member_id", columnDefinition = "uuid")
    private UUID linkedMemberId;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Person create(UUID groupId, String name, FamilyRelation relation,
                                PersonLifeStatus lifeStatus, LocalDate deceasedAt,
                                PersonVisibility visibility, String nickname,
                                UUID profilePhotoId, UUID linkedMemberId) {
        PersonVisibility effectiveVisibility = visibility == null ? PersonVisibility.SHOWN : visibility;
        validate(name, relation, lifeStatus, deceasedAt, effectiveVisibility, nickname);
        Person person = new Person();
        person.id = UUID.randomUUID();
        person.groupId = groupId;
        person.name = name.trim();
        person.relation = relation;
        person.lifeStatus = lifeStatus;
        person.deceasedAt = deceasedAt;
        person.visibility = effectiveVisibility;
        person.nickname = normalizeNickname(nickname);
        person.profilePhotoId = profilePhotoId;
        person.linkedMemberId = linkedMemberId;
        person.active = true;
        person.createdAt = LocalDateTime.now();
        person.updatedAt = person.createdAt;
        return person;
    }

    public void update(FamilyRelation relation, PersonLifeStatus lifeStatus, LocalDate deceasedAt,
                       PersonVisibility visibility, String nickname, UUID profilePhotoId) {
        FamilyRelation effectiveRelation = relation == null ? this.relation : relation;
        PersonLifeStatus effectiveLifeStatus = lifeStatus == null ? this.lifeStatus : lifeStatus;
        LocalDate effectiveDeceasedAt = lifeStatus == null ? this.deceasedAt : deceasedAt;
        PersonVisibility effectiveVisibility = visibility == null ? this.visibility : visibility;
        String effectiveNickname = nickname == null ? this.nickname : nickname;
        validate(this.name, effectiveRelation, effectiveLifeStatus, effectiveDeceasedAt, effectiveVisibility,
                effectiveNickname);

        boolean affectsContent = this.lifeStatus != effectiveLifeStatus
                || this.visibility != effectiveVisibility
                || this.active != true;
        this.relation = effectiveRelation;
        this.lifeStatus = effectiveLifeStatus;
        this.deceasedAt = effectiveDeceasedAt;
        this.visibility = effectiveVisibility;
        this.nickname = normalizeNickname(effectiveNickname);
        if (profilePhotoId != null) {
            this.profilePhotoId = profilePhotoId;
        }
        touch();
        if (affectsContent) {
            registerEvent(new PersonSafetyChangedEvent(groupId, id));
        }
    }

    /** 태그는 유지하고 어르신 노출만 영구 차단한다. */
    public void deactivate() {
        if (!active) {
            return;
        }
        active = false;
        visibility = PersonVisibility.HIDDEN;
        touch();
        registerEvent(new PersonSafetyChangedEvent(groupId, id));
    }

    public PersonContentTense contentTense() {
        if (!active || visibility == PersonVisibility.HIDDEN) {
            return PersonContentTense.EXCLUDED;
        }
        return switch (lifeStatus) {
            case ALIVE -> PersonContentTense.CURRENT_ALLOWED;
            case DECEASED -> PersonContentTense.PAST_ONLY;
            case UNKNOWN -> PersonContentTense.NEUTRAL_ONLY;
        };
    }

    private static void validate(String name, FamilyRelation relation, PersonLifeStatus lifeStatus,
                                 LocalDate deceasedAt, PersonVisibility visibility, String nickname) {
        if (name == null || name.trim().length() < 2 || name.trim().length() > 10) {
            throw new M0ValidationException("인물명은 2~10자로 입력해주세요.");
        }
        if (relation == null || lifeStatus == null || visibility == null) {
            throw new M0ValidationException("관계, 생존 여부, 노출 설정은 필수예요.");
        }
        if (lifeStatus == PersonLifeStatus.DECEASED && deceasedAt == null) {
            throw new M0ValidationException("작고한 분은 작고 시기를 입력해주세요.");
        }
        if (lifeStatus != PersonLifeStatus.DECEASED && deceasedAt != null) {
            throw new M0ValidationException("작고 시기는 작고한 경우에만 입력할 수 있어요.");
        }
        if (nickname != null && nickname.trim().length() > 30) {
            throw new M0ValidationException("호칭은 30자를 넘을 수 없어요.");
        }
    }

    private static String normalizeNickname(String nickname) {
        return nickname == null || nickname.isBlank() ? null : nickname.trim();
    }

    private void touch() {
        updatedAt = LocalDateTime.now();
    }
}
