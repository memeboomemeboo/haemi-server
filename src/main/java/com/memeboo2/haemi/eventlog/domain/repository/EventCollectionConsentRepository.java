package com.memeboo2.haemi.eventlog.domain.repository;

import com.memeboo2.haemi.eventlog.domain.EventCollectionConsent;

import java.util.Optional;

public interface EventCollectionConsentRepository {

    EventCollectionConsent save(EventCollectionConsent consent);

    Optional<EventCollectionConsent> findByElderId(String elderId);
}
