package com.itsm.ticketing.entity;

/**
 * Enum representing the roles a user can have in the ITSM system.
 * ADMIN   - Full access to all system features, can assign tickets
 * SUPPORT - Technical support staff, can be assigned to tickets and work on them
 * USER    - Client user, limited access (can create and view tickets)
 */
public enum Role {
    ADMIN,
    SUPPORT,
    USER
}
