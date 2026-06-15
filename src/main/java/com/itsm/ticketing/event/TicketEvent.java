package com.itsm.ticketing.event;

import com.itsm.ticketing.dto.TicketResponse;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Application event fired when something significant happens to a ticket.
 * Published inside a {@code @Transactional} method; the matching listener
 * is registered with {@code phase = AFTER_COMMIT} so the WebSocket broadcast
 * only fires once the data is durably persisted.
 */
@Getter
public class TicketEvent extends ApplicationEvent {

    public enum Type {
        CREATED,
        STATUS_CHANGED,
        ASSIGNED
    }

    private final Type type;
    private final TicketResponse payload;

    public TicketEvent(Object source, Type type, TicketResponse payload) {
        super(source);
        this.type = type;
        this.payload = payload;
    }
}
