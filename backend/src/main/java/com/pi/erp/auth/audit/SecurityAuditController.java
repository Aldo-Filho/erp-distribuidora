package com.pi.erp.auth.audit;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityAuditController {

    @Autowired
    private JdbcTemplate jdbc;

    public record EventView(
        Long id,
        Long actorId,
        String action,
        String target,
        String outcome,
        Instant occurredAt,
        UUID correlationId,
        String changes,
        String actorName,
        String actorType,
        String targetName,
        String targetType
    ) {}

    public static Pageable pageable(int page, int size) {
        if (page < 0 || size < 1 || size > 100 || page > 100000) {
            throw new IllegalArgumentException("Invalid pagination parameters.");
        }
        return PageRequest.of(page, size);
    }

    @GetMapping("/security/audit/search")
    public ResponseEntity<Page<EventView>> search(
        @RequestParam(required = false) Instant from,
        @RequestParam(required = false) Instant to,
        @RequestParam(required = false) Long actorId,
        @RequestParam(required = false) String actorName,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String outcome,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        Pageable pageable = pageable(page, size);
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        if (from != null) {
            where.append(" AND occurred_at >= ?");
            parameters.add(Timestamp.from(from));
        }
        if (to != null) {
            where.append(" AND occurred_at <= ?");
            parameters.add(Timestamp.from(to));
        }
        if (actorId != null) {
            where.append(" AND actor_id = ?");
            parameters.add(actorId);
        }
        if (actorName != null && !actorName.isBlank()) {
            where.append(" AND position(lower(?) in lower(coalesce(actor_name, ''))) > 0");
            parameters.add(actorName.trim());
        }
        if (action != null && !action.isBlank()) {
            where.append(" AND action = ?");
            parameters.add(action);
        }
        if (outcome != null && !outcome.isBlank()) {
            where.append(" AND outcome = ?");
            parameters.add(outcome);
        }
        Long total = jdbc.queryForObject(
            "SELECT count(*) FROM security_audit_events" + where,
            Long.class,
            parameters.toArray()
        );
        parameters.add(size);
        parameters.add(pageable.getOffset());
        List<EventView> events = jdbc.query(
            "SELECT * FROM security_audit_events" +
                where +
                " ORDER BY occurred_at DESC, id DESC LIMIT ? OFFSET ?",
            (row, index) ->
                new EventView(
                    row.getLong("id"),
                    row.getObject("actor_id", Long.class),
                    row.getString("action"),
                    row.getString("target"),
                    row.getString("outcome"),
                    row.getTimestamp("occurred_at").toInstant(),
                    row.getObject("correlation_id", UUID.class),
                    row.getString("changes"),
                    row.getString("actor_name"),
                    row.getString("actor_type"),
                    row.getString("target_name"),
                    row.getString("target_type")
                ),
            parameters.toArray()
        );
        return ResponseEntity.ok(new PageImpl<>(events, pageable, total));
    }
}
