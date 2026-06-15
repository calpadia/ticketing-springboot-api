package com.itsm.ticketing.controller;

import com.itsm.ticketing.dto.AttachmentResponse;
import com.itsm.ticketing.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing ticket attachments.
 * Provides endpoints for listing and downloading attachments.
 */
@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
@Slf4j
public class AttachmentController {

    private final FileStorageService fileStorageService;

    /**
     * Get all attachments for a ticket.
     *
     * @param ticketId the ticket ID
     * @return list of attachment info
     */
    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<List<AttachmentResponse>> getAttachmentsByTicketId(
            @PathVariable Long ticketId) {
        log.info("GET /api/v1/attachments/ticket/{} - Fetching attachments", ticketId);
        List<AttachmentResponse> attachments = fileStorageService.getAttachmentsByTicketId(ticketId);
        return ResponseEntity.ok(attachments);
    }

    /**
     * Download an attachment by its ID.
     *
     * @param id the attachment ID
     * @return the file resource
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id) {
        log.info("GET /api/v1/attachments/{}/download - Downloading attachment", id);

        Resource resource = fileStorageService.loadFileAsResource(id);
        String fileName = fileStorageService.getOriginalFileName(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
