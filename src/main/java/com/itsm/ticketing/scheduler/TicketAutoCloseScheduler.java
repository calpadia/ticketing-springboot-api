package com.itsm.ticketing.scheduler;

import com.itsm.ticketing.service.TicketAutoCloseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that triggers the auto-close logic for stale tickets every midnight.
 *
 * <p>Runs at 00:00 server time every day. Delegates all business logic to
 * {@link TicketAutoCloseService#processAutoClose()}.</p>
 *
 * <p>Requires {@code @EnableScheduling} on the application or a {@code @Configuration} class.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TicketAutoCloseScheduler {

    private final TicketAutoCloseService ticketAutoCloseService;

    /**
     * Runs at midnight every day (server time).
     *
     * <p>Cron expression: {@code "0 0 0 * * *"} — second=0, minute=0, hour=0.</p>
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void runAutoClose() {
        log.info("=== [Scheduler] Auto-close job started ===");
        try {
            int closed = ticketAutoCloseService.processAutoClose();
            log.info("=== [Scheduler] Auto-close job finished: {} ticket(s) closed ===", closed);
        } catch (Exception e) {
            log.error("=== [Scheduler] Auto-close job encountered an error: {} ===", e.getMessage(), e);
        }
    }
}
