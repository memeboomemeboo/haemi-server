package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.Person;
import com.memeboo2.haemi.m0.domain.model.PersonVisibility;
import com.memeboo2.haemi.m0.domain.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PersonRepositoryAdapter implements PersonRepository {

    private final JpaPersonRepository persons;

    @Override
    public Person save(Person person) {
        return persons.save(person);
    }

    @Override
    public Optional<Person> findById(UUID personId) {
        return persons.findById(personId);
    }

    @Override
    public List<Person> findAllByGroupIdAndVisibility(UUID groupId, PersonVisibility visibility) {
        return persons.findAllByGroupIdAndVisibilityAndActiveTrue(groupId, visibility);
    }

    @Override
    public List<Person> findAllByGroupIdAndLinkedMemberId(UUID groupId, UUID linkedMemberId) {
        return persons.findAllByGroupIdAndLinkedMemberId(groupId, linkedMemberId);
    }

    @Override
    public List<Person> findAllByGroupIdAndName(UUID groupId, String name) {
        return persons.findAllByGroupIdAndName(groupId, name);
    }
}
