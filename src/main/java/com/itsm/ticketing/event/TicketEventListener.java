package com.itsm.ticketing.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Broadcasts ticket state changes over WebSocket AFTER the DB transaction commits.
 *
 * <p>Using {@code AFTER_COMMIT} guarantees:</p>
 * <ul>
 *     <li>Clients never receive a broadcast for data that was rolled back.</li>
 *     <li>When a client RE-FETCHES after the push notification the data is
 *         already visible.</li>
 * </ul>
 *
 * <h3>Topic layout</h3>
 * <pre>
 * /topic/tickets/new           → broadcast when a ticket is created
 * /topic/tickets/{id}/status   → broadcast when ticket status changes
 * /topic/tickets/{id}/assigned → broadcast when assignment changes
 * </pre>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TicketEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketEvent(TicketEvent event) {
        String destination;
        switch (event.getType()) {
            case CREATED -> destination = "/topic/tickets/new";
            case STATUS_CHANGED -> destination = "/topic/tickets/" + event.getPayload().getId() + "/status";
            case ASSIGNED -> destination = "/topic/tickets/" + event.getPayload().getId() + "/assigned";
            default -> {
                log.warn("Unknown TicketEvent type: {}", event.getType());
                return;
            }
        }

        messagingTemplate.convertAndSend(destination, event.getPayload());
        log.debug("WebSocket broadcast: {} → {}", event.getType(), destination);
    }
}
