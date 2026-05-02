package com.bookstore.notifications.domain.models;

public record EmailMessage(String to, String subject, String content) {}
