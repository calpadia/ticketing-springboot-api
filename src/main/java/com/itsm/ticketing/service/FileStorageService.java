package com.itsm.ticketing.service;

import com.itsm.ticketing.dto.AttachmentResponse;
import com.itsm.ticketing.entity.Ticket;
import com.itsm.ticketing.entity.TicketAttachment;
import com.itsm.ticketing.exception.ResourceNotFoundException;
import com.itsm.ticketing.repository.TicketAttachmentRepository;
import com.itsm.ticketing.security.FileValidationUtil;
import com.itsm.ticketing.security.InputSanitizer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for file storage operations.
 * Handles uploading files to local filesystem and retrieving them.
 *
 * Security hardened with:
 * - Path traversal prevention (CWE-22)
 * - File type validation (CWE-434)
 * - Filename sanitization (OWASP File Upload)
 * - Secure file storage with UUID naming
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final TicketAttachmentRepository attachmentRepository;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private Path uploadPath;

    /**
     * Initialize the upload directory on startup.
     */
    @PostConstruct
    public void init() {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
            log.info("Upload directory initialized: {}", uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadPath, e);
        }
    }

    /**
     * Store uploaded files and link them to a ticket.
     * Validates each file before storage (CWE-434).
     *
     * @param ticket the ticket entity to link attachments to
     * @param files  the uploaded files
     * @return list of saved attachment responses
     */
    public List<AttachmentResponse> storeFiles(Ticket ticket, List<MultipartFile> files) {
        return files.stream()
                .filter(file -> !file.isEmpty())
                .map(file -> storeFile(ticket, file))
                .collect(Collectors.toList());
    }

    /**
     * Store a single file and create an attachment record.
     * Includes file validation and path traversal prevention.
     */
    private AttachmentResponse storeFile(Ticket ticket, MultipartFile file) {
        // Validate file (CWE-434: Unrestricted Upload)
        String validationError = FileValidationUtil.validateFile(file);
        if (validationError != null) {
            log.warn("SECURITY_AUDIT: File upload rejected for ticket {}: {}",
                    ticket.getTicketNumber(), validationError);
            throw new IllegalArgumentException(validationError);
        }

        // Sanitize filename (CWE-22: Path Traversal)
        String originalFileName = InputSanitizer.sanitizeFilename(file.getOriginalFilename());
        // Use UUID prefix to prevent filename collisions and predictability
        String storedFileName = UUID.randomUUID().toString() + "_" + originalFileName;

        try {
            // Resolve and validate target path (CWE-22 defense-in-depth)
            Path targetLocation = uploadPath.resolve(storedFileName).normalize();

            // Ensure the resolved path is still within the upload directory
            if (!targetLocation.startsWith(uploadPath)) {
                log.error("SECURITY_AUDIT: Path traversal attempt detected! " +
                        "Resolved path: {} is outside upload dir: {}", targetLocation, uploadPath);
                throw new SecurityException("Invalid file path - potential path traversal attack");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            TicketAttachment attachment = TicketAttachment.builder()
                    .ticket(ticket)
                    .fileName(originalFileName)
                    .storedFileName(storedFileName)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .build();

            TicketAttachment saved = attachmentRepository.save(attachment);
            log.info("File stored securely: {} -> {} (ticket: {})",
                    originalFileName, storedFileName, ticket.getTicketNumber());

            return mapToResponse(saved);

        } catch (IOException e) {
            throw new RuntimeException("Could not store file: " + originalFileName, e);
        }
    }

    /**
     * Get all attachments for a ticket.
     */
    public List<AttachmentResponse> getAttachmentsByTicketId(Long ticketId) {
        return attachmentRepository.findByTicketId(ticketId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Load a file as a Resource for downloading.
     * Includes path traversal protection (CWE-22).
     *
     * @param attachmentId the attachment ID
     * @return the file resource
     */
    public Resource loadFileAsResource(Long attachmentId) {
        TicketAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attachment not found with ID: " + attachmentId));

        try {
            Path filePath = uploadPath.resolve(attachment.getStoredFileName()).normalize();

            // Path traversal protection (CWE-22)
            if (!filePath.startsWith(uploadPath)) {
                log.error("SECURITY_AUDIT: Path traversal attempt in file download! " +
                        "Resolved: {} Upload dir: {}", filePath, uploadPath);
                throw new SecurityException("Invalid file path");
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new ResourceNotFoundException(
                        "File not found: " + attachment.getFileName());
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException(
                    "File not found: " + attachment.getFileName());
        }
    }

    /**
     * Get the original filename for an attachment.
     */
    public String getOriginalFileName(Long attachmentId) {
        TicketAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attachment not found with ID: " + attachmentId));
        return attachment.getFileName();
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    private AttachmentResponse mapToResponse(TicketAttachment attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .fileType(attachment.getFileType())
                .fileSize(attachment.getFileSize())
                .downloadUrl("/api/v1/attachments/" + attachment.getId() + "/download")
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }
}
