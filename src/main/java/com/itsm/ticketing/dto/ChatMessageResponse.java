package com.itsm.ticketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for chat messages.
 * Used both in WebSocket broadcast and REST API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {

    private Long id;
    private Long ticketId;
    private String ticketNumber;
    private Long senderId;
    private String senderName;
    private String senderRole;
    private String content;
    private LocalDateTime sentAt;
}
