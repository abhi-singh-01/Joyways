package in.joyway.dto.admin.operations;

import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;

@Builder
public record TripSummaryDto(
        Long id,
        String tripReference,
        String packageTitle,
        String destination,
        LocalDate travelDate,
        Instant startAt,
        Instant endAt,
        int bookingCount,
        int totalTravellers,
        Integer vehicleCapacity,
        Integer remainingCapacity,
        String vehicleRegistration,
        String driverName,
        String status
) {}
