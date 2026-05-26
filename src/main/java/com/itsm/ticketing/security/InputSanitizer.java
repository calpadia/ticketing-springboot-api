package com.itsm.ticketing.security;

import java.util.regex.Pattern;

/**
 * Utility class for input sanitization and validation.
 * Implements defense against common injection attacks.
 * References:
 * - CWE-79 (Cross-site Scripting)
 * - CWE-89 (SQL Injection)
 * - OWASP Input Validation Cheat Sheet
 * - BSSN: Standar Keamanan Aplikasi - Validasi Input
 */
public final class InputSanitizer {

    private InputSanitizer() {
        // Utility class
    }

    // Password policy: min 8 chars, at least 1 uppercase, 1 lowercase, 1 digit, 1 special char
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#+\\-_])[A-Za-z\\d@$!%*?&#+\\-_]{8,128}$"
    );

    // Email pattern (RFC 5322 simplified)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    );

    // Phone pattern (international format)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[+]?[0-9\\-\\s()]{7,20}$"
    );

    // Filename pattern (safe characters only)
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._\\-\\s()]{1,255}$"
    );

    // XSS dangerous patterns
    private static final Pattern XSS_PATTERN = Pattern.compile(
            "<script|javascript:|on\\w+\\s*=|<iframe|<object|<embed|<form|data:text/html",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Validate password meets security policy.
     * NIST SP 800-63B: minimum 8 characters with complexity requirements.
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 128) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Validate email format.
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.length() > 254) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate phone number format.
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return true; // Phone is optional
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Sanitize text input by removing potential XSS payloads.
     * Strips HTML tags and dangerous patterns.
     */
    public static String sanitizeText(String input) {
        if (input == null) {
            return null;
        }
        // Remove null bytes
        String sanitized = input.replace("\0", "");
        // Encode HTML special characters
        sanitized = sanitized
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
        return sanitized.trim();
    }

    /**
     * Check if input contains potential XSS payload.
     */
    public static boolean containsXss(String input) {
        if (input == null) {
            return false;
        }
        return XSS_PATTERN.matcher(input).find();
    }

    /**
     * Validate filename is safe (no path traversal, no dangerous chars).
     * CWE-22: Path Traversal prevention.
     */
    public static boolean isSafeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return false;
        }
        // Check for path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return false;
        }
        return SAFE_FILENAME_PATTERN.matcher(filename).matches();
    }

    /**
     * Sanitize filename by removing dangerous characters.
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unnamed_file";
        }
        // Remove path separators and traversal
        String sanitized = filename
                .replace("..", "")
                .replace("/", "")
                .replace("\\", "")
                .replace("\0", "");
        // Keep only safe characters
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._\\-\\s()]", "_");
        if (sanitized.isBlank()) {
            return "unnamed_file";
        }
        // Limit length
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }
        return sanitized;
    }

    /**
     * Get password policy description for error messages.
     */
    public static String getPasswordPolicyMessage() {
        return "Password must be 8-128 characters with at least 1 uppercase letter, "
                + "1 lowercase letter, 1 digit, and 1 special character (@$!%*?&#+\\-_)";
    }
}
