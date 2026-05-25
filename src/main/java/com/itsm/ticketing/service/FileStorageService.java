package com.itsm.ticketing.service;

import com.itsm.ticketing.dto.AttachmentResponse;
import com.itsm.ticketing.entity.Ticket;
import com.itsm.ticketing.entity.TicketAttachment;
import com.itsm.ticketing.exception.ResourceNotFoundException;
import com.itsm.ticketing.repository.TicketAttachmentRepository;
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
     */
    private AttachmentResponse storeFile(Ticket ticket, MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        String storedFileName = UUID.randomUUID().toString() + "_" + originalFileName;

        try {
            Path targetLocation = uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            TicketAttachment attachment = TicketAttachment.builder()
                    .ticket(ticket)
                    .fileName(originalFileName)
                    .storedFileName(storedFileName)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .build();

            TicketAttachment saved = attachmentRepository.save(attachment);
            log.info("File stored: {} -> {} (ticket: {})",
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
