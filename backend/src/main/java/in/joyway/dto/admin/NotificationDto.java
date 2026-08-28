package in.joyway.dto.admin;

import lombok.Builder;

import java.time.Instant;

@Builder
public record NotificationDto(
        Long id,
        String type,
        String title,
        String message,
        String relatedEntityType,
        Long relatedEntityId,
        boolean read,
        Instant createdAt,
        Instant readAt
) {}
