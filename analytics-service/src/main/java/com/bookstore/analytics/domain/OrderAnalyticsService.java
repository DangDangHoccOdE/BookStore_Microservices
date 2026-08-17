package com.bookstore.analytics.domain;

import com.bookstore.analytics.web.AnalyticsResetResponse;
import com.bookstore.analytics.web.AnalyticsSummaryResponse;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderAnalyticsService {
    private static final Logger log = LoggerFactory.getLogger(OrderAnalyticsService.class);

    private final OrderAnalyticsRepository orderAnalyticsRepository;
    private final ProcessedEventRepository processedEventRepository;

    OrderAnalyticsService(
            OrderAnalyticsRepository orderAnalyticsRepository, ProcessedEventRepository processedEventRepository) {
        this.orderAnalyticsRepository = orderAnalyticsRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary() {
        OrderAnalyticsEntity analytics = getCurrentAnalytics();

        return new AnalyticsSummaryResponse(
                analytics.getTotalOrders(), analytics.getTotalItems(), analytics.getUpdatedAt());
    }

    public boolean recordOrderCreated(String eventId, long itemCount) {
        int inserted = processedEventRepository.insertIfNotExists(eventId, "OrderCreatedEvent");

        if (inserted == 0) {
            log.warn("Duplicate OrderCreatedEvent skipped, eventId={}", eventId);
            return false;
        }

        OrderAnalyticsEntity analytics = getCurrentAnalytics();
        analytics.setTotalOrders(analytics.getTotalOrders() + 1);
        analytics.setTotalItems(analytics.getTotalItems() + itemCount);
        analytics.setUpdatedAt(LocalDateTime.now());

        log.info("Order analytics updated for eventId={}, itemCount={}", eventId, itemCount);

        return true;
    }

    private OrderAnalyticsEntity getCurrentAnalytics() {
        return orderAnalyticsRepository
                .findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("Order analytics row was not initialized"));
    }

    public AnalyticsResetResponse resetForReplay() {
        int deletedEvents = processedEventRepository.deleteAllProcessedEvents();

        OrderAnalyticsEntity analytics = getCurrentAnalytics();
        analytics.setTotalOrders(0L);
        analytics.setTotalItems(0L);
        analytics.setUpdatedAt(LocalDateTime.now());

        log.warn("Order analytics reset for replay. Deleted {} processed events.", deletedEvents);

        return new AnalyticsResetResponse("Analytics rebuild state reset", deletedEvents, analytics.getUpdatedAt());
    }
}
