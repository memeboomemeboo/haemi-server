package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.PhotoPerson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPhotoPersonRepository extends JpaRepository<PhotoPerson, UUID> {
    Optional<PhotoPerson> findByPhotoIdAndPerson_Id(UUID photoId, UUID personId);
    List<PhotoPerson> findAllByPhotoId(UUID photoId);
}
