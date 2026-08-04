package com.memeboo2.haemi.m0.application.service;

import com.memeboo2.haemi.m0.application.command.CreatePersonCommand;
import com.memeboo2.haemi.m0.application.command.TagPhotoPersonCommand;
import com.memeboo2.haemi.m0.application.command.UpdatePersonCommand;
import com.memeboo2.haemi.m0.application.dto.PersonResult;
import com.memeboo2.haemi.m0.domain.model.*;
import com.memeboo2.haemi.m0.domain.port.PersonExposurePort;
import com.memeboo2.haemi.m0.domain.port.PhotoOwnershipPort;
import com.memeboo2.haemi.m0.domain.repository.FamilyGroupRepository;
import com.memeboo2.haemi.m0.domain.repository.PersonRepository;
import com.memeboo2.haemi.m0.domain.repository.PhotoPersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonApplicationService implements PersonExposurePort {

    private final FamilyGroupRepository groups;
    private final PersonRepository persons;
    private final PhotoPersonRepository photoPersons;
    private final PhotoOwnershipPort photoOwnership;

    public PersonResult create(UUID actorId, UUID groupId, CreatePersonCommand command) {
        FamilyGroup group = loadGroup(groupId);
        group.requireActiveMember(actorId);
        Person person = Person.create(groupId, command.name(), command.relation(), command.lifeStatus(),
                command.deceasedAt(), command.visibility(), command.nickname(), command.profilePhotoId(),
                command.linkedMemberId());
        return PersonResult.from(persons.save(person));
    }

    public PersonResult update(UUID actorId, UUID personId, UpdatePersonCommand command) {
        Person person = loadPerson(personId);
        loadGroup(person.getGroupId()).requireOwner(actorId);
        person.update(command.lifeStatus(), command.deceasedAt(), command.visibility(), command.nickname(),
                command.profilePhotoId());
        return PersonResult.from(persons.save(person));
    }

    public void deactivate(UUID actorId, UUID personId) {
        Person person = loadPerson(personId);
        loadGroup(person.getGroupId()).requireOwner(actorId);
        person.deactivate();
        persons.save(person);
    }

    public void tagPhoto(UUID actorId, UUID photoId, TagPhotoPersonCommand command) {
        Person person = loadPerson(command.personId());
        loadGroup(person.getGroupId()).requireActiveMember(actorId);
        photoOwnership.requireBelongsToGroup(photoId, person.getGroupId());
        UUID confirmedBy = command.confirmed() ? actorId : null;
        PhotoPerson tag = photoPersons.findByPhotoIdAndPersonId(photoId, person.getId())
                .orElseGet(() -> PhotoPerson.create(photoId, person, command.confidence(), confirmedBy));
        if (tag.getId() != null) {
            tag.update(command.confidence(), confirmedBy);
        }
        photoPersons.save(tag);
    }

    @Transactional(readOnly = true)
    public List<PersonResult> findShown(UUID actorId, UUID groupId) {
        loadGroup(groupId).requireActiveMember(actorId);
        return persons.findAllByGroupIdAndVisibility(groupId, PersonVisibility.SHOWN).stream()
                .map(PersonResult::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhotoPersonExposure> findByPhotoId(UUID photoId) {
        return photoPersons.findAllByPhotoId(photoId).stream()
                .map(tag -> new PhotoPersonExposure(tag.getPerson().getId(), tag.getPerson().getName(),
                        tag.getPerson().getNickname(), tag.getPerson().contentTense(), tag.canUsePersonName()))
                .toList();
    }

    private Person loadPerson(UUID personId) {
        return persons.findById(personId).orElseThrow(() -> new M0NotFoundException("인물"));
    }

    private FamilyGroup loadGroup(UUID groupId) {
        return groups.findById(groupId).orElseThrow(() -> new M0NotFoundException("가족 그룹"));
    }
}
