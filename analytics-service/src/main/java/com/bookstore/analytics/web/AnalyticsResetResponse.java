package com.bookstore.analytics.web;

import java.time.LocalDateTime;

public record AnalyticsResetResponse(String message, int deletedProcessedEvents, LocalDateTime resetAt) {}
