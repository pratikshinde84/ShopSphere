package com.shophub.service;

import com.shophub.dto.event.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventListener {

    @Async
    @EventListener
    public void handleOrderCreated(OrderEvent event) {
        log.info("[LOCAL EVENT LISTENER] Received order.created event for Order #{} (Buyer: {}, Total: ${})",
                event.getOrderId(), event.getBuyerEmail(), event.getTotalPrice());
        // Custom background processing (e.g. notifications, analytics) can be performed here asynchronously
    }
}
