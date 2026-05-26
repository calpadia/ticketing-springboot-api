package com.itsm.ticketing.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Security audit logger for tracking security-relevant events.
 * References:
 * - NIST SP 800-53 AU-2 (Audit Events)
 * - NIST SP 800-53 AU-3 (Content of Audit Records)
 * - BSSN: Standar Keamanan Aplikasi - Audit Logging
 * - CWE-778 (Insufficient Logging)
 */
@Component
@Slf4j
public class SecurityAuditLogger {

    private static final String AUDIT_PREFIX = "SECURITY_AUDIT";

    /**
     * Log successful authentication event.
     */
    public void logAuthSuccess(String email, String ipAddress) {
        log.info("{}: AUTH_SUCCESS | user={} | ip={}", AUDIT_PREFIX, email, ipAddress);
    }

    /**
     * Log failed authentication event.
     */
    public void logAuthFailure(String email, String ipAddress, String reason) {
        log.warn("{}: AUTH_FAILURE | user={} | ip={} | reason={}",
                AUDIT_PREFIX, email, ipAddress, reason);
    }

    /**
     * Log user registration event.
     */
    public void logRegistration(String email, String role, String ipAddress) {
        log.info("{}: USER_REGISTERED | user={} | role={} | ip={}",
                AUDIT_PREFIX, email, role, ipAddress);
    }

    /**
     * Log access denied event.
     */
    public void logAccessDenied(String email, String resource, String ipAddress) {
        log.warn("{}: ACCESS_DENIED | user={} | resource={} | ip={}",
                AUDIT_PREFIX, email, resource, ipAddress);
    }

    /**
     * Log suspicious activity (potential attack).
     */
    public void logSuspiciousActivity(String description, String ipAddress, String details) {
        log.warn("{}: SUSPICIOUS_ACTIVITY | description={} | ip={} | details={}",
                AUDIT_PREFIX, description, ipAddress, details);
    }

    /**
     * Log file upload event.
     */
    public void logFileUpload(String email, String filename, long fileSize, String ipAddress) {
        log.info("{}: FILE_UPLOAD | user={} | file={} | size={} | ip={}",
                AUDIT_PREFIX, email, filename, fileSize, ipAddress);
    }

    /**
     * Log file upload rejection.
     */
    public void logFileUploadRejected(String email, String filename, String reason, String ipAddress) {
        log.warn("{}: FILE_UPLOAD_REJECTED | user={} | file={} | reason={} | ip={}",
                AUDIT_PREFIX, email, filename, reason, ipAddress);
    }

    /**
     * Log privilege escalation attempt.
     */
    public void logPrivilegeEscalation(String email, String attemptedAction, String ipAddress) {
        log.error("{}: PRIVILEGE_ESCALATION_ATTEMPT | user={} | action={} | ip={}",
                AUDIT_PREFIX, email, attemptedAction, ipAddress);
    }

    /**
     * Log data access event (sensitive operations).
     */
    public void logDataAccess(String email, String resource, String action, String ipAddress) {
        log.info("{}: DATA_ACCESS | user={} | resource={} | action={} | ip={}",
                AUDIT_PREFIX, email, resource, action, ipAddress);
    }

    /**
     * Extract client IP from request.
     */
    public String extractIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
