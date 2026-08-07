package com.shophub.service;

import com.shophub.dto.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishOrderCreated(OrderEvent event) {
        log.info("Publishing local order.created event for orderId={}", event.getOrderId());
        eventPublisher.publishEvent(event);
    }
}
