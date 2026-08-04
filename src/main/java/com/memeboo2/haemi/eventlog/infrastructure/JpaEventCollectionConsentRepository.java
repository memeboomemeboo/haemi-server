package com.memeboo2.haemi.eventlog.infrastructure;

import com.memeboo2.haemi.eventlog.domain.EventCollectionConsent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaEventCollectionConsentRepository
        extends JpaRepository<EventCollectionConsent, String> {
}
