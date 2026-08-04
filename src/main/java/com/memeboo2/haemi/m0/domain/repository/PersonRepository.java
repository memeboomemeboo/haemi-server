package com.memeboo2.haemi.m0.domain.repository;

import com.memeboo2.haemi.m0.domain.model.Person;
import com.memeboo2.haemi.m0.domain.model.PersonVisibility;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonRepository {
    Person save(Person person);
    Optional<Person> findById(UUID personId);
    List<Person> findAllByGroupIdAndVisibility(UUID groupId, PersonVisibility visibility);
}
