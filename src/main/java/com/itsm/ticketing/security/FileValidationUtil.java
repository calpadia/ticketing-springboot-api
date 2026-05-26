package com.itsm.ticketing.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * Utility class for file upload validation.
 * Prevents unrestricted file upload attacks.
 * References:
 * - CWE-434 (Unrestricted Upload of File with Dangerous Type)
 * - OWASP File Upload Cheat Sheet
 * - NIST SP 800-53 SI-3 (Malicious Code Protection)
 * - BSSN: Standar Keamanan Aplikasi - Validasi File Upload
 */
@Slf4j
public final class FileValidationUtil {

    private FileValidationUtil() {
        // Utility class
    }

    // Allowed MIME types for file uploads
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            // Images
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/svg+xml",
            // Documents
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            // Text
            "text/plain",
            "text/csv",
            // Archives
            "application/zip",
            "application/x-rar-compressed",
            "application/x-7z-compressed"
    );

    // Allowed file extensions
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "svg",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "csv", "zip", "rar", "7z", "log"
    );

    // Dangerous file extensions that should NEVER be allowed
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "sh", "ps1", "vbs", "js", "jar",
            "msi", "dll", "com", "scr", "pif", "hta", "cpl",
            "php", "asp", "aspx", "jsp", "py", "rb", "pl"
    );

    // Maximum file size: 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * Validate an uploaded file for security.
     *
     * @param file the uploaded file
     * @return validation result message, null if valid
     */
    public static String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "File is empty or null";
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("SECURITY_AUDIT: File upload rejected - size exceeds limit: {} bytes",
                    file.getSize());
            return "File size exceeds maximum allowed size (10MB)";
        }

        // Check filename
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return "File must have a valid filename";
        }

        // Check for path traversal in filename
        if (originalFilename.contains("..") || originalFilename.contains("/")
                || originalFilename.contains("\\")) {
            log.warn("SECURITY_AUDIT: File upload rejected - path traversal attempt: {}",
                    originalFilename);
            return "Invalid filename - contains illegal characters";
        }

        // Check file extension
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (BLOCKED_EXTENSIONS.contains(extension)) {
            log.warn("SECURITY_AUDIT: File upload rejected - blocked extension: {}",
                    originalFilename);
            return "File type not allowed: ." + extension;
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("SECURITY_AUDIT: File upload rejected - unknown extension: {}",
                    originalFilename);
            return "File type not supported: ." + extension;
        }

        // Check MIME type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            log.warn("SECURITY_AUDIT: File upload rejected - invalid MIME type: {} for file: {}",
                    contentType, originalFilename);
            return "File content type not allowed: " + contentType;
        }

        // Check for double extensions (e.g., file.php.jpg)
        if (hasDoubleExtension(originalFilename)) {
            String firstExt = getFirstExtension(originalFilename);
            if (BLOCKED_EXTENSIONS.contains(firstExt.toLowerCase())) {
                log.warn("SECURITY_AUDIT: File upload rejected - double extension attack: {}",
                        originalFilename);
                return "Invalid filename - suspicious double extension detected";
            }
        }

        return null; // Valid
    }

    /**
     * Get file extension from filename.
     */
    public static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    /**
     * Check if filename has double extension.
     */
    private static boolean hasDoubleExtension(String filename) {
        long dotCount = filename.chars().filter(ch -> ch == '.').count();
        return dotCount > 1;
    }

    /**
     * Get the first extension in a double-extension filename.
     */
    private static String getFirstExtension(String filename) {
        int firstDot = filename.indexOf('.');
        int lastDot = filename.lastIndexOf('.');
        if (firstDot != lastDot) {
            return filename.substring(firstDot + 1, lastDot);
        }
        return "";
    }
}
