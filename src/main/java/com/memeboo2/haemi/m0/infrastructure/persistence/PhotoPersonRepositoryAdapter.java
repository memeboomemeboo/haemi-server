package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.PhotoPerson;
import com.memeboo2.haemi.m0.domain.repository.PhotoPersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PhotoPersonRepositoryAdapter implements PhotoPersonRepository {

    private final JpaPhotoPersonRepository photoPersons;

    @Override
    public PhotoPerson save(PhotoPerson photoPerson) {
        return photoPersons.save(photoPerson);
    }

    @Override
    public Optional<PhotoPerson> findByPhotoIdAndPersonId(UUID photoId, UUID personId) {
        return photoPersons.findByPhotoIdAndPerson_Id(photoId, personId);
    }

    @Override
    public List<PhotoPerson> findAllByPhotoId(UUID photoId) {
        return photoPersons.findAllByPhotoId(photoId);
    }
}
