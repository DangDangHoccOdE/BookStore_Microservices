package com.bookstore.order.jobs;

import com.bookstore.order.domain.OrderEventRepository;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxScheduler {
    private static final Logger log = LoggerFactory.getLogger(OutboxScheduler.class);

    private final OrderEventRepository orderEventRepository;

    public OutboxScheduler(OrderEventRepository orderEventRepository) {
        this.orderEventRepository = orderEventRepository;
    }

    @Scheduled(fixedDelayString = "${orders.new-orders-job-delay}")
    @Transactional
    @SchedulerLock(name = "recoverStuckEvents")
    public void recoverStuckEvents() {
        LockAssert.assertLocked();
        int updated = orderEventRepository.recoverStuckEvents();
        log.info("Recovered {} stuck events", updated);
    }
}
