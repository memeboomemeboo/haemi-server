package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.Person;
import com.memeboo2.haemi.m0.domain.model.PersonVisibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPersonRepository extends JpaRepository<Person, UUID> {
    List<Person> findAllByGroupIdAndVisibilityAndActiveTrue(UUID groupId, PersonVisibility visibility);
    List<Person> findAllByGroupIdAndLinkedMemberId(UUID groupId, UUID linkedMemberId);
    List<Person> findAllByGroupIdAndName(UUID groupId, String name);
}
