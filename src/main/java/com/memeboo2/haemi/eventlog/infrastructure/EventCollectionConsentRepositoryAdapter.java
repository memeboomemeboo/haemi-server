package com.memeboo2.haemi.eventlog.infrastructure;

import com.memeboo2.haemi.eventlog.domain.EventCollectionConsent;
import com.memeboo2.haemi.eventlog.domain.repository.EventCollectionConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EventCollectionConsentRepositoryAdapter implements EventCollectionConsentRepository {

    private final JpaEventCollectionConsentRepository jpa;

    @Override
    public EventCollectionConsent save(EventCollectionConsent consent) {
        return jpa.save(consent);
    }

    @Override
    public Optional<EventCollectionConsent> findByElderId(String elderId) {
        return jpa.findById(elderId);
    }
}
