package com.bookstore.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_analytics")
public class OrderAnalyticsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_analytics_id_generator")
    @SequenceGenerator(name = "order_analytics_id_generator", sequenceName = "order_analytics_id_seq")
    private Long id;

    @Column(name = "total_orders", nullable = false)
    private Long totalOrders = 0L;

    @Column(name = "total_items", nullable = false)
    private Long totalItems = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public Long getTotalOrders() {
        return totalOrders;
    }

    public Long getTotalItems() {
        return totalItems;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTotalOrders(Long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public void setTotalItems(Long totalItems) {
        this.totalItems = totalItems;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
