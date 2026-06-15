package com.itsm.ticketing.repository;

import com.itsm.ticketing.entity.Ticket;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reusable JPA {@link Specification}s for {@link Ticket}.
 *
 * <p>Specifications are composed at runtime so only the predicates that are
 * actually requested end up in the generated SQL. This avoids the PostgreSQL
 * "could not determine data type of parameter" error that happens when
 * binding a {@code null} parameter to a JPQL {@code IS NULL} check.</p>
 */
public final class TicketSpecifications {

    private TicketSpecifications() {}

    public static Specification<Ticket> withClientId(Long clientId) {
        if (clientId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("client").get("id"), clientId);
    }

    public static Specification<Ticket> withIdIn(Collection<Long> ids) {
        if (ids == null) return null;
        if (ids.isEmpty()) {
            // Caller passed an empty whitelist — match nothing.
            return (root, query, cb) -> cb.disjunction();
        }
        return (root, query, cb) -> root.get("id").in(ids);
    }

    public static Specification<Ticket> createdAtFrom(LocalDateTime from) {
        if (from == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Ticket> createdAtTo(LocalDateTime to) {
        if (to == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    /**
     * Combine the given specs, skipping any that are {@code null}.
     * Returns {@code null} (i.e. no predicate) if every spec is null.
     */
    @SafeVarargs
    public static Specification<Ticket> allOf(Specification<Ticket>... specs) {
        List<Specification<Ticket>> nonNull = new ArrayList<>();
        for (Specification<Ticket> s : specs) {
            if (s != null) nonNull.add(s);
        }
        if (nonNull.isEmpty()) return null;
        Specification<Ticket> result = nonNull.get(0);
        for (int i = 1; i < nonNull.size(); i++) {
            result = result.and(nonNull.get(i));
        }
        return result;
    }
}
