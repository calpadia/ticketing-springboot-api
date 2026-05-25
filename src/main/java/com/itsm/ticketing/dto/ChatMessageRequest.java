package com.itsm.ticketing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sending a chat message via WebSocket.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageRequest {

    @NotNull(message = "Ticket ID is required")
    private Long ticketId;

    @NotBlank(message = "Message content is required")
    private String content;
}
