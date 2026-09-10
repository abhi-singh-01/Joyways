package in.joyway.dto.package_;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
public record PackageDetailDto(
        Long id,
        String title,
        String slug,
        String destination,
        String shortDescription,
        String fullDescription,
        String departureCity,
        String pickupPoints,
        LocalDate startDate,
        LocalDate endDate,
        String duration,
        BigDecimal pricePerPerson,
        Integer totalSeats,
        Integer availableSeats,
        String hotelName,
        String hotelDetails,
        String vehicleType,
        String itinerary,
        String inclusions,
        String exclusions,
        String cancellationPolicy,
        String status,
        List<PackageImageDto> images,
        boolean bookable,
        boolean waitlistable
) {}
