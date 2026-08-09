package com.memeboo2.haemi.eventlog.infrastructure;

import com.memeboo2.haemi.eventlog.domain.EventTypeCount;
import com.memeboo2.haemi.eventlog.domain.LoggedEvent;
import com.memeboo2.haemi.eventlog.domain.repository.LoggedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class LoggedEventRepositoryAdapter implements LoggedEventRepository {

    private final JpaLoggedEventRepository jpa;

    @Override
    public LoggedEvent save(LoggedEvent event) {
        return jpa.save(event);
    }

    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return jpa.existsById(idempotencyKey);
    }

    @Override
    public int pseudonymizeByElderId(String elderId) {
        return jpa.pseudonymizeByElderId(elderId);
    }

    @Override
    public List<EventTypeCount> countByTypeBetween(LocalDateTime from, LocalDateTime to) {
        return jpa.countByTypeBetween(from, to);
    }
}
