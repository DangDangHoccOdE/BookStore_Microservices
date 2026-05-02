package com.bookstore.notifications.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderEventRepository extends JpaRepository<OrderEventEntity, Long> {

    @Modifying
    @Query(
            value =
                    """
        insert into order_events(event_id, status, created_at, updated_at, retry_count)
        values (:eventId, 'PROCESSING', now(), now(), 0)
        on conflict (event_id) do nothing
    """,
            nativeQuery = true)
    int insertIfNotExists(@Param("eventId") String eventId);

    @Modifying
    @Query(
            value =
                    """
        update order_events
        set status = 'PROCESSING',
            updated_at = now()
        where event_id = :eventId
          and status = 'FAILED'
    """,
            nativeQuery = true)
    int retryIfFailed(@Param("eventId") String eventId);

    @Modifying
    @Query(
            value =
                    """
            update order_events
               set status = 'SENT',
                   processed_at = now(),
                   updated_at = now(),
                   last_error = null
             where event_id = :eventId
            """,
            nativeQuery = true)
    int markSent(@Param("eventId") String eventId);

    @Modifying
    @Query(
            value =
                    """
            update order_events
               set status = 'FAILED',
                   retry_count = retry_count + 1,
                   last_error = :error,
                   updated_at = now()
             where event_id = :eventId
            """,
            nativeQuery = true)
    int markFailed(@Param("eventId") String eventId, @Param("error") String error);
}
