package in.joyway.dto.review;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record PackageReviewsResponse(
        BigDecimal averageRating,
        long reviewCount,
        List<PackageReviewItemDto> reviews
) {
}
