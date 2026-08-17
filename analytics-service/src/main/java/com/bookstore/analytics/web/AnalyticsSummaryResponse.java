package com.bookstore.analytics.web;

import java.time.LocalDateTime;

public record AnalyticsSummaryResponse(Long totalOrders, Long totalItems, LocalDateTime updatedAt) {}
