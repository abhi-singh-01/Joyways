package in.joyway.dto.admin;

import lombok.Builder;

import java.time.Instant;

@Builder
public record AdminAuditLogDto(
        Long id,
        String actorName,
        String actorEmail,
        String action,
        String entityType,
        Long entityId,
        String summary,
        Instant createdAt
) {}
