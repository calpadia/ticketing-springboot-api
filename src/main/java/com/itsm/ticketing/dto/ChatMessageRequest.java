package com.itsm.ticketing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for sending a chat message via WebSocket.
 * Content can be blank if attachments are provided.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageRequest {

    @NotNull(message = "Ticket ID is required")
    private Long ticketId;

    /**
     * Text content of the message. Can be empty/null if only sending attachments.
     */
    private String content;

    /**
     * List of attachment IDs (from prior upload via REST) to link to this message.
     * Optional — can be null or empty for text-only messages.
     */
    private List<Long> attachmentIds;
}

