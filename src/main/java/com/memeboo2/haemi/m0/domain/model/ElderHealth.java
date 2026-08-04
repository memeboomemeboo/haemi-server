package com.memeboo2.haemi.m0.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** 민감 진단 정보는 elder 테이블과 분리하고 암호문으로만 저장한다. */
@Entity
@Table(name = "elder_health")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ElderHealth {

    @Id
    @Column(name = "elder_id", columnDefinition = "uuid")
    private UUID elderId;

    @Column(name = "diagnosis_encrypted", nullable = false, columnDefinition = "text")
    private String diagnosisEncrypted;

    @Column(name = "consent_id", nullable = false, length = 100)
    private String consentId;

    @Column(name = "diagnosed_at")
    private LocalDate diagnosedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static ElderHealth create(UUID elderId, String encryptedDiagnosis, String consentId, LocalDate diagnosedAt) {
        if (consentId == null || consentId.isBlank()) {
            throw new M0ValidationException("건강 정보 저장에는 별도 동의가 필요해요.");
        }
        ElderHealth health = new ElderHealth();
        health.elderId = elderId;
        health.diagnosisEncrypted = encryptedDiagnosis;
        health.consentId = consentId.trim();
        health.diagnosedAt = diagnosedAt;
        health.updatedAt = LocalDateTime.now();
        return health;
    }

    public void update(String encryptedDiagnosis, String consentId, LocalDate diagnosedAt) {
        if (consentId == null || consentId.isBlank()) {
            throw new M0ValidationException("건강 정보 저장에는 별도 동의가 필요해요.");
        }
        this.diagnosisEncrypted = encryptedDiagnosis;
        this.consentId = consentId.trim();
        this.diagnosedAt = diagnosedAt;
        this.updatedAt = LocalDateTime.now();
    }
}
