package com.itsm.ticketing.repository;

import com.itsm.ticketing.entity.ChatAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, Long> {

    /**
     * Find attachment by ID and sender ID (ownership validation).
     */
    Optional<ChatAttachment> findByIdAndSenderId(Long id, Long senderId);

    /**
     * Find all attachments linked to a specific chat message.
     */
    List<ChatAttachment> findByChatMessageId(Long chatMessageId);

    /**
     * Find all attachments by their IDs (batch lookup).
     */
    List<ChatAttachment> findAllByIdIn(List<Long> ids);
}
