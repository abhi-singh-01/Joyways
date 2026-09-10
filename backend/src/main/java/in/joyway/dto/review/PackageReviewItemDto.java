package in.joyway.dto.review;

import lombok.Builder;

import java.time.Instant;

@Builder
public record PackageReviewItemDto(
        String customerName,
        int rating,
        String comment,
        Instant createdAt
) {
}
