package com.bookstore.order.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderEventRepository extends JpaRepository<OrderEventEntity, Long> {
    @Query(
            value =
                    """
        select * from order_events
        where status = 'PENDING'
        order by created_at
        for update skip locked
        limit :limit
        """,
            nativeQuery = true)
    List<OrderEventEntity> lockPendingEvents(@Param("limit") int limit);

    @Modifying
    @Query(
            value =
                    """
    UPDATE order_events
    SET status = 'PENDING',
        locked_at = NULL,
        locked_by = NULL
    WHERE status = 'PROCESSING'
    AND locked_at < now() - interval '5 minutes'
    """,
            nativeQuery = true)
    int recoverStuckEvents();
}
