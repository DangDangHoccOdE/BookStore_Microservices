package com.bookstore.analytics.web;

import com.bookstore.analytics.domain.OrderAnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final OrderAnalyticsService orderAnalyticsService;

    AnalyticsController(OrderAnalyticsService orderAnalyticsService) {
        this.orderAnalyticsService = orderAnalyticsService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    AnalyticsSummaryResponse getSummary() {
        log.info("Retrieving order analytics summary");
        return orderAnalyticsService.getSummary();
    }

    @PostMapping("/rebuild/reset")
    @PreAuthorize("hasRole('ADMIN')")
    AnalyticsResetResponse resetForReplay() {
        log.warn("Resetting analytics rebuild state");
        return orderAnalyticsService.resetForReplay();
    }
}
