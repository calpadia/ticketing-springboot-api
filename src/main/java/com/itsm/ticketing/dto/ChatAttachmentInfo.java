package com.itsm.ticketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Compact DTO for attachment info embedded in chat message responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatAttachmentInfo {

    private Long id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String downloadUrl;
}
