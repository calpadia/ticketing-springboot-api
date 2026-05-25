package com.itsm.ticketing.service;

import com.itsm.ticketing.dto.*;
import com.itsm.ticketing.entity.*;
import com.itsm.ticketing.exception.ResourceNotFoundException;
import com.itsm.ticketing.repository.ChatAttachmentRepository;
import com.itsm.ticketing.repository.ChatMessageRepository;
import com.itsm.ticketing.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing chat messages.
 * Handles sending messages, retrieving chat history, and file attachments.
 * Enforces access control (client-scoped) and ticket status rules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatAttachmentRepository chatAttachmentRepository;
    private final TicketRepository ticketRepository;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    // ========================================================================
    // FILE UPLOAD
    // ========================================================================

    /**
     * Upload a file for use in a chat message.
     * The file is stored on disk and a ChatAttachment record is created.
     * The attachment is NOT linked to a message yet — it will be linked
     * when the user sends the message via WebSocket.
     *
     * @param ticketId the ticket this file belongs to
     * @param file     the uploaded file
     * @param sender   the authenticated user uploading the file
     * @return upload response with attachment ID for later reference
     */
    @Transactional
    public ChatUploadResponse uploadChatFile(Long ticketId, MultipartFile file, User sender) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with id: " + ticketId));

        // Access control
        validateTicketAccess(ticket, sender);

        // Block upload if ticket is RESOLVED or CLOSED
        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED) {
            throw new IllegalStateException(
                    "Tidak bisa upload file. Ticket sudah berstatus: " + ticket.getStatus());
        }

        // Store the file
        String originalFileName = file.getOriginalFilename();
        String storedFileName = UUID.randomUUID().toString() + "_" + originalFileName;

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            Path targetLocation = uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            ChatAttachment attachment = ChatAttachment.builder()
                    .ticket(ticket)
                    .sender(sender)
                    .fileName(originalFileName)
                    .storedFileName(storedFileName)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .build();

            ChatAttachment saved = chatAttachmentRepository.save(attachment);
            log.info("Chat file uploaded: {} -> {} (ticket: {}, user: {})",
                    originalFileName, storedFileName, ticket.getTicketNumber(), sender.getEmail());

            return ChatUploadResponse.builder()
                    .id(saved.getId())
                    .fileName(saved.getFileName())
                    .fileType(saved.getFileType())
                    .fileSize(saved.getFileSize())
                    .downloadUrl("/api/v1/chat/attachments/" + saved.getId() + "/download")
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Could not store file: " + originalFileName, e);
        }
    }

    /**
     * Load a chat attachment file as a Resource for downloading.
     *
     * @param attachmentId the chat attachment ID
     * @param currentUser  the authenticated user requesting the download
     * @return the file resource
     */
    @Transactional(readOnly = true)
    public Resource loadChatAttachmentAsResource(Long attachmentId, User currentUser) {
        ChatAttachment attachment = chatAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chat attachment not found with ID: " + attachmentId));

        // Access control: validate user can access this ticket's chat
        validateTicketAccess(attachment.getTicket(), currentUser);

        try {
            Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize()
                    .resolve(attachment.getStoredFileName()).normalize();
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
     * Get the original filename for a chat attachment.
     */
    @Transactional(readOnly = true)
    public String getChatAttachmentFileName(Long attachmentId) {
        ChatAttachment attachment = chatAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chat attachment not found with ID: " + attachmentId));
        return attachment.getFileName();
    }

    // ========================================================================
    // MESSAGING
    // ========================================================================

    /**
     * Send a new chat message linked to a ticket.
     * Rules:
     * - USER can only send messages on tickets belonging to their client
     * - Chat is blocked if ticket status is RESOLVED or CLOSED
     * - Message must have content or attachments (or both)
     *
     * @param request the chat message request containing ticketId, content, and optional attachmentIds
     * @param sender  the authenticated user sending the message
     * @return the saved chat message as a response DTO
     */
    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request, User sender) {
        Ticket ticket = ticketRepository.findById(request.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with id: " + request.getTicketId()));

        // Access control: USER can only chat on their client's tickets
        validateTicketAccess(ticket, sender);

        // Block chat if ticket is RESOLVED or CLOSED
        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED) {
            throw new IllegalStateException(
                    "Chat tidak bisa dilanjutkan. Ticket sudah berstatus: " + ticket.getStatus());
        }

        // Validate: message must have content or attachments
        boolean hasContent = request.getContent() != null && !request.getContent().isBlank();
        boolean hasAttachments = request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty();

        if (!hasContent && !hasAttachments) {
            throw new IllegalArgumentException(
                    "Pesan harus memiliki konten teks atau lampiran file (atau keduanya)");
        }

        ChatMessage chatMessage = ChatMessage.builder()
                .ticket(ticket)
                .sender(sender)
                .content(hasContent ? request.getContent() : null)
                .senderRole(sender.getRole())
                .build();

        ChatMessage saved = chatMessageRepository.save(chatMessage);

        // Link attachments to this message if provided
        List<ChatAttachmentInfo> attachmentInfos = new ArrayList<>();
        if (hasAttachments) {
            attachmentInfos = linkAttachmentsToMessage(saved, request.getAttachmentIds(), sender, ticket);
        }

        log.info("Chat message saved: id={}, ticketId={}, sender={}, attachments={}",
                saved.getId(), ticket.getId(), sender.getEmail(), attachmentInfos.size());

        ChatMessageResponse response = mapToResponse(saved);
        response.setAttachments(attachmentInfos);
        return response;
    }

    // ========================================================================
    // CHAT HISTORY
    // ========================================================================

    /**
     * Get all chat messages for a ticket by ticket ID.
     * Access controlled per user's client.
     *
     * @param ticketId    the ticket ID
     * @param currentUser the authenticated user
     * @return list of chat messages ordered by sent time
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatHistory(Long ticketId, User currentUser) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with id: " + ticketId));

        validateTicketAccess(ticket, currentUser);

        return chatMessageRepository.findByTicketIdOrderBySentAtAsc(ticketId)
                .stream()
                .map(msg -> {
                    ChatMessageResponse response = mapToResponse(msg);
                    response.setAttachments(getAttachmentInfosForMessage(msg.getId()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all chat messages for a ticket by ticket number.
     * Access controlled per user's client.
     *
     * @param ticketNumber the unique ticket number
     * @param currentUser  the authenticated user
     * @return list of chat messages ordered by sent time
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatHistoryByTicketNumber(String ticketNumber, User currentUser) {
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with number: " + ticketNumber));

        validateTicketAccess(ticket, currentUser);

        return chatMessageRepository.findByTicketTicketNumberOrderBySentAtAsc(ticketNumber)
                .stream()
                .map(msg -> {
                    ChatMessageResponse response = mapToResponse(msg);
                    response.setAttachments(getAttachmentInfosForMessage(msg.getId()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    /**
     * Link previously uploaded attachments to a chat message.
     * Validates ownership (sender must match) and ticket scope.
     */
    private List<ChatAttachmentInfo> linkAttachmentsToMessage(
            ChatMessage message, List<Long> attachmentIds, User sender, Ticket ticket) {

        List<ChatAttachment> attachments = chatAttachmentRepository.findAllByIdIn(attachmentIds);

        if (attachments.size() != attachmentIds.size()) {
            throw new ResourceNotFoundException(
                    "Beberapa attachment ID tidak ditemukan. Dikirim: " + attachmentIds.size()
                            + ", ditemukan: " + attachments.size());
        }

        List<ChatAttachmentInfo> infos = new ArrayList<>();

        for (ChatAttachment attachment : attachments) {
            // Validate: attachment must belong to the same sender
            if (!attachment.getSender().getId().equals(sender.getId())) {
                throw new AccessDeniedException(
                        "Attachment ID " + attachment.getId() + " bukan milik Anda");
            }
            // Validate: attachment must be for the same ticket
            if (!attachment.getTicket().getId().equals(ticket.getId())) {
                throw new IllegalArgumentException(
                        "Attachment ID " + attachment.getId() + " bukan untuk ticket ini");
            }
            // Validate: attachment must not already be linked to another message
            if (attachment.getChatMessage() != null) {
                throw new IllegalArgumentException(
                        "Attachment ID " + attachment.getId() + " sudah digunakan di pesan lain");
            }

            attachment.setChatMessage(message);
            chatAttachmentRepository.save(attachment);

            infos.add(mapAttachmentToInfo(attachment));
        }

        return infos;
    }

    /**
     * Get attachment info list for a specific message.
     */
    private List<ChatAttachmentInfo> getAttachmentInfosForMessage(Long messageId) {
        return chatAttachmentRepository.findByChatMessageId(messageId)
                .stream()
                .map(this::mapAttachmentToInfo)
                .collect(Collectors.toList());
    }

    /**
     * Validates that the current user has access to this ticket's chat.
     * ADMIN: always allowed. USER: only if ticket belongs to their client.
     */
    private void validateTicketAccess(Ticket ticket, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return; // Admin can access all ticket chats
        }
        if (currentUser.getClient() == null ||
                !currentUser.getClient().getId().equals(ticket.getClient().getId())) {
            throw new AccessDeniedException(
                    "Anda tidak memiliki akses ke chat ticket ini");
        }
    }

    private ChatMessageResponse mapToResponse(ChatMessage chatMessage) {
        return ChatMessageResponse.builder()
                .id(chatMessage.getId())
                .ticketId(chatMessage.getTicket().getId())
                .ticketNumber(chatMessage.getTicket().getTicketNumber())
                .senderId(chatMessage.getSender().getId())
                .senderName(chatMessage.getSender().getName())
                .senderRole(chatMessage.getSenderRole().name())
                .content(chatMessage.getContent())
                .sentAt(chatMessage.getSentAt())
                .build();
    }

    private ChatAttachmentInfo mapAttachmentToInfo(ChatAttachment attachment) {
        return ChatAttachmentInfo.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .fileType(attachment.getFileType())
                .fileSize(attachment.getFileSize())
                .downloadUrl("/api/v1/chat/attachments/" + attachment.getId() + "/download")
                .build();
    }
}
