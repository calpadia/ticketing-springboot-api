package com.itsm.ticketing.repository;

import com.itsm.ticketing.entity.TicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TicketAttachment entity.
 */
@Repository
public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {

    /**
     * Find all attachments for a given ticket.
     */
    List<TicketAttachment> findByTicketId(Long ticketId);
}
