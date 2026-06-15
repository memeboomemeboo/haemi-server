package com.memeboo2.haemi.m1.domain.model.album;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "photos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Photo {

    private static final int MAX_PERSON_TAGS = 10;

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Embedded
    private PhotoFile file;

    @Embedded
    private PhotoMetadata metadata;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "photo_person_tags",
            joinColumns = @JoinColumn(name = "photo_id")
    )
    private List<PersonTag> personTags = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false)
    private AnalysisStatus analysisStatus;

    @Column(name = "sha256_hash", nullable = false)
    private String hash;

    @Column(name = "uploaded_by", nullable = false)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id", nullable = false)
    private Album album;

    static Photo create(PhotoFile file, PhotoMetadata metadata, String hash, String uploadedBy, Album album) {
        Photo photo = new Photo();
        photo.id = UUID.randomUUID();
        photo.file = file;
        photo.metadata = metadata;
        photo.hash = hash;
        photo.uploadedBy = uploadedBy;
        photo.uploadedAt = LocalDateTime.now();
        photo.analysisStatus = AnalysisStatus.PENDING;
        photo.album = album;
        return photo;
    }

    public PhotoId getPhotoId() {
        return PhotoId.of(id);
    }

    void updateMemo(String timePeriod, String locationText, String memo) {
        this.metadata = this.metadata.withMemo(timePeriod, locationText, memo);
    }

    void tagPersons(List<PersonTag> tags) {
        if (tags.size() > MAX_PERSON_TAGS) {
            throw new PersonTagLimitExceededException(tags.size());
        }
        this.personTags.clear();
        this.personTags.addAll(tags);
    }

    void markAnalysisInProgress() {
        this.analysisStatus = AnalysisStatus.IN_PROGRESS;
    }

    void markAnalysisCompleted() {
        this.analysisStatus = AnalysisStatus.COMPLETED;
    }

    void markAnalysisFailed() {
        this.analysisStatus = AnalysisStatus.FAILED;
    }

    public List<PersonTag> getPersonTags() {
        return Collections.unmodifiableList(personTags);
    }
}
