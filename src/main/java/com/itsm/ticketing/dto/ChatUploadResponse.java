package com.itsm.ticketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned after uploading a file for chat.
 * Contains the attachment ID that should be referenced when sending the chat message.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatUploadResponse {

    private Long id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String downloadUrl;
}
