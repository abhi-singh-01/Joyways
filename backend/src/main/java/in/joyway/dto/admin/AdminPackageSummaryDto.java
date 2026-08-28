package in.joyway.dto.admin;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record AdminPackageSummaryDto(
        Long id,
        String title,
        String slug,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        String duration,
        BigDecimal pricePerPerson,
        Integer totalSeats,
        Integer availableSeats,
        String status,
        String heroImageUrl,
        int imageCount
) {}
