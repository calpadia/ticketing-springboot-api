package com.itsm.ticketing.entity;

/**
 * Enum representing the roles a user can have in the ITSM system.
 * <p>
 * Hierarchy:
 * <ul>
 *   <li>ADMIN — full access to all system features, can assign/reassign tickets to anyone</li>
 *   <li>SUPPORT — Tier 1 support (customer-facing). Receives tickets, can escalate
 *       (assign) tickets to TECHNICAL_SUPPORT for technical work.</li>
 *   <li>TECHNICAL_SUPPORT — Tier 2/3 technical engineers. Handle technical issues
 *       escalated from SUPPORT. Can view and work on tickets assigned to them.</li>
 *   <li>USER — Client user. Limited access (can create and view their client's tickets).</li>
 * </ul>
 */
public enum Role {
    ADMIN,
    SUPPORT,
    TECHNICAL_SUPPORT,
    USER
}
