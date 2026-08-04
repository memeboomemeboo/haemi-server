package com.memeboo2.haemi.m0.domain.repository;

import com.memeboo2.haemi.m0.domain.model.PhotoPerson;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhotoPersonRepository {
    PhotoPerson save(PhotoPerson photoPerson);
    Optional<PhotoPerson> findByPhotoIdAndPersonId(UUID photoId, UUID personId);
    List<PhotoPerson> findAllByPhotoId(UUID photoId);
}
