package in.joyway.dto.admin;

import lombok.Builder;

import java.time.Instant;

@Builder
public record AdminEmailLogDto(
        Long id,
        String recipient,
        String subject,
        String template,
        String status,
        int attemptCount,
        Instant sentAt,
        Instant lastAttemptedAt,
        Instant createdAt,
        String failureReason
) {}
