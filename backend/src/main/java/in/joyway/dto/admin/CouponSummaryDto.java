package in.joyway.dto.admin;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Builder
public record CouponSummaryDto(
        Long id,
        String code,
        Long packageId,
        String packageTitle,
        String destination,
        String discountType,
        BigDecimal discountValue,
        LocalDate expiresOn,
        boolean active,
        boolean expired,
        long redemptionCount,
        Instant createdAt
) {}
