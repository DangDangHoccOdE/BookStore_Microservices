package com.bookstore.analytics.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderAnalyticsRepository extends JpaRepository<OrderAnalyticsEntity, Long> {

    Optional<OrderAnalyticsEntity> findFirstByOrderByIdAsc();
}
