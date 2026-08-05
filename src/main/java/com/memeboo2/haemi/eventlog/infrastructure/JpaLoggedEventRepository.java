package com.memeboo2.haemi.eventlog.infrastructure;

import com.memeboo2.haemi.eventlog.domain.EventTypeCount;
import com.memeboo2.haemi.eventlog.domain.LoggedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface JpaLoggedEventRepository extends JpaRepository<LoggedEvent, String> {

    @Modifying
    @Query("""
            UPDATE LoggedEvent e SET e.elderId = null, e.pseudonymized = true
            WHERE e.elderId = :elderId
            """)
    int pseudonymizeByElderId(String elderId);

    @Query("""
            SELECT new com.memeboo2.haemi.eventlog.domain.EventTypeCount(e.eventType, COUNT(e))
            FROM LoggedEvent e
            WHERE e.occurredAt BETWEEN :from AND :to
            GROUP BY e.eventType
            """)
    List<EventTypeCount> countByTypeBetween(LocalDateTime from, LocalDateTime to);
}
