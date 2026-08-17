package com.bookstore.analytics.domain;

import org.springframework.data.jpa.repository.JpaRepository;

interface FailedKafkaEventRepository extends JpaRepository<FailedKafkaEventEntity, Long> {}
