package com.itsm.ticketing.repository;

import com.itsm.ticketing.entity.TicketUserRead;
import com.itsm.ticketing.entity.TicketUserReadId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link TicketUserRead} read receipts.
 *
 * <p>Provides queries to compute unread ticket and message counts
 * for a given user, scoped to the set of tickets they can access.</p>
 */
@Repository
public interface TicketUserReadRepository extends JpaRepository<TicketUserRead, TicketUserReadId> {

    /**
     * Count unread tickets from a given set of accessible ticket IDs for a user.
     *
     * <p>A ticket is considered unread when:</p>
     * <ul>
     *   <li>The user has never opened it (no row in ticket_user_reads), OR</li>
     *   <li>The ticket was created after the user last read it (new ticket since last visit)</li>
     * </ul>
     *
     * @param userId              the user's ID
     * @param accessibleTicketIds the list of ticket IDs this user is allowed to see
     * @return count of unread tickets
     */
    @Query("""
            SELECT COUNT(DISTINCT t.id) FROM Ticket t
            WHERE t.id IN :accessibleTicketIds
              AND (
                NOT EXISTS (
                  SELECT r FROM TicketUserRead r
                  WHERE r.id.ticketId = t.id AND r.id.userId = :userId
                )
                OR t.createdAt > (
                  SELECT r.lastReadAt FROM TicketUserRead r
                  WHERE r.id.ticketId = t.id AND r.id.userId = :userId
                )
              )
            """)
    long countUnreadTickets(@Param("userId") Long userId,
                            @Param("accessibleTicketIds") List<Long> accessibleTicketIds);

    /**
     * Count unread chat messages across a given set of accessible ticket IDs for a user.
     *
     * <p>A message is considered unread when:</p>
     * <ul>
     *   <li>It was sent by someone else (not the current user), AND</li>
     *   <li>The user has never opened that ticket (no row in ticket_user_reads), OR</li>
     *   <li>The message was sent after the user last read the ticket</li>
     * </ul>
     *
     * @param userId              the user's ID
     * @param accessibleTicketIds the list of ticket IDs this user is allowed to see
     * @return count of unread messages
     */
    @Query("""
            SELECT COUNT(m) FROM ChatMessage m
            WHERE m.ticket.id IN :accessibleTicketIds
              AND m.sender.id != :userId
              AND (
                NOT EXISTS (
                  SELECT r FROM TicketUserRead r
                  WHERE r.id.ticketId = m.ticket.id AND r.id.userId = :userId
                )
                OR m.sentAt > (
                  SELECT r.lastReadAt FROM TicketUserRead r
                  WHERE r.id.ticketId = m.ticket.id AND r.id.userId = :userId
                )
              )
            """)
    long countUnreadMessages(@Param("userId") Long userId,
                             @Param("accessibleTicketIds") List<Long> accessibleTicketIds);

    /**
     * Check whether the user has ever opened (read) a specific ticket.
     * Used to populate the {@code isRead} flag on each {@link com.itsm.ticketing.dto.TicketResponse}
     * so the frontend can highlight unread ticket rows with a "NEW" badge.
     *
     * <p>Returns {@code true} if a {@link TicketUserRead} record exists for the given
     * (ticketId, userId) pair — i.e. the user has opened the ticket detail page at least once.</p>
     *
     * @param ticketId the ticket ID
     * @param userId   the user ID
     * @return true if a read receipt exists, false otherwise
     */
    boolean existsByIdTicketIdAndIdUserId(Long ticketId, Long userId);

    /**
     * Count unread chat messages for a single ticket for the given user.
     *
     * <p>A message is considered unread when:</p>
     * <ul>
     *   <li>It was sent by someone else (not the current user), AND</li>
     *   <li>The user has never opened that ticket (no row in ticket_user_reads), OR</li>
     *   <li>The message was sent after the user last read the ticket</li>
     * </ul>
     *
     * @param ticketId the specific ticket ID
     * @param userId   the user ID
     * @return count of unread messages for this ticket
     */
    @Query("""
            SELECT COUNT(m) FROM ChatMessage m
            WHERE m.ticket.id = :ticketId
              AND m.sender.id != :userId
              AND (
                NOT EXISTS (
                  SELECT r FROM TicketUserRead r
                  WHERE r.id.ticketId = :ticketId AND r.id.userId = :userId
                )
                OR m.sentAt > (
                  SELECT r.lastReadAt FROM TicketUserRead r
                  WHERE r.id.ticketId = :ticketId AND r.id.userId = :userId
                )
              )
            """)
    long countUnreadMessagesByTicketAndUser(@Param("ticketId") Long ticketId,
                                            @Param("userId") Long userId);
    /**
     * Fetch per-ticket detail rows for all tickets that have at least one unread message.
     *
     * <p>Returns one row per unread message (not per ticket), ordered by {@code sentAt DESC}.
     * The caller (service layer) should group by ticketId and take the first row per group
     * to get the latest unread message per ticket.</p>
     *
     * <p>Each {@code Object[]} row contains:
     * <ol>
     *   <li>[0] {@code Long ticketId}</li>
     *   <li>[1] {@code String ticketNumber}</li>
     *   <li>[2] {@code String clientName} — the client that owns the ticket</li>
     *   <li>[3] {@code String content} — raw message text (may be null for attachment-only messages)</li>
     *   <li>[4] {@code LocalDateTime sentAt}</li>
     * </ol>
     * </p>
     *
     * @param userId              the current user's ID (messages sent by this user are excluded)
     * @param accessibleTicketIds the ticket IDs this user is allowed to see
     * @return raw rows ordered by sentAt DESC
     */
    @Query("""
            SELECT t.id, t.ticketNumber, t.client.companyName, m.content, m.sentAt
            FROM ChatMessage m
            JOIN m.ticket t
            WHERE t.id IN :accessibleTicketIds
              AND m.sender.id != :userId
              AND (
                NOT EXISTS (
                  SELECT r FROM TicketUserRead r
                  WHERE r.id.ticketId = t.id AND r.id.userId = :userId
                )
                OR m.sentAt > (
                  SELECT r.lastReadAt FROM TicketUserRead r
                  WHERE r.id.ticketId = t.id AND r.id.userId = :userId
                )
              )
            ORDER BY m.sentAt DESC
            """)
    List<Object[]> findUnreadMessageDetails(@Param("userId") Long userId,
                                            @Param("accessibleTicketIds") List<Long> accessibleTicketIds);
}

