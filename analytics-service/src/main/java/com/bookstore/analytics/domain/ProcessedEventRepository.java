package com.bookstore.analytics.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, Long> {

    @Modifying
    @Query(
            value =
                    """
        insert into processed_events(event_id, event_type, processed_at, created_at)
        values (:eventId, :eventType, now(), now())
        on conflict (event_id) do nothing
        """,
            nativeQuery = true)
    int insertIfNotExists(@Param("eventId") String eventId, @Param("eventType") String eventType);

    @Modifying
    @Query(value = "delete from processed_events", nativeQuery = true)
    int deleteAllProcessedEvents();
}
