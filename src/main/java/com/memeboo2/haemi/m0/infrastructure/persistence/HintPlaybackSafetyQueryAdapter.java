package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.Person;
import com.memeboo2.haemi.m0.domain.model.PersonContentTense;
import com.memeboo2.haemi.m0.domain.model.PersonLifeStatus;
import com.memeboo2.haemi.m3.domain.port.HintPlaybackSafetyQuery;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import com.memeboo2.haemi.m0.domain.repository.PersonRepository;
import com.memeboo2.haemi.m0.domain.repository.PhotoPersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** 사진 태그와 가족 인물 마스터를 이용해 힌트 재생 안전성을 최종 검증한다. */
@Component
@RequiredArgsConstructor
public class HintPlaybackSafetyQueryAdapter implements HintPlaybackSafetyQuery {

    private final ElderRepository elders;
    private final PersonRepository persons;
    private final PhotoPersonRepository photoPersons;

    @Override
    @Transactional(readOnly = true)
    public boolean isPlayable(String elderId, UUID photoId, String authorMemberId, String mentionedPersonName) {
        UUID parsedElderId;
        try {
            parsedElderId = UUID.fromString(elderId);
        } catch (IllegalArgumentException | NullPointerException invalidId) {
            return false;
        }
        return elders.findById(parsedElderId)
                .map(elder -> isPlayableForGroup(elder.getGroupId(), photoId, authorMemberId, mentionedPersonName))
                .orElse(false);
    }

    private boolean isPlayableForGroup(UUID groupId, UUID photoId, String authorMemberId, String mentionedPersonName) {
        if (photoId != null && photoPersons.findAllByPhotoId(photoId).stream()
                .map(tag -> tag.getPerson())
                .anyMatch(this::isExcluded)) {
            return false;
        }
        if (authorMemberId != null && !authorMemberId.isBlank()) {
            try {
                if (persons.findAllByGroupIdAndLinkedMemberId(groupId, UUID.fromString(authorMemberId)).stream()
                        .anyMatch(this::isUnsafeAuthor)) {
                    return false;
                }
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }
        return mentionedPersonName == null || mentionedPersonName.isBlank()
                || persons.findAllByGroupIdAndName(groupId, mentionedPersonName.trim()).stream()
                .noneMatch(this::isExcluded);
    }

    private boolean isUnsafeAuthor(Person person) {
        return isExcluded(person) || person.getLifeStatus() == PersonLifeStatus.DECEASED;
    }

    private boolean isExcluded(Person person) {
        return person.contentTense() == PersonContentTense.EXCLUDED;
    }
}
