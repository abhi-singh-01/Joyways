package in.joyway.dto.package_;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record PackageSummaryDto(
        Long id,
        String title,
        String slug,
        String destination,
        String shortDescription,
        String duration,
        String departureCity,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal pricePerPerson,
        Integer availableSeats,
        String heroImageUrl,
        String heroImageAlt
) {}
