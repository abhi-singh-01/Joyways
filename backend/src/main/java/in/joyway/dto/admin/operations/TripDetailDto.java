package in.joyway.dto.admin.operations;

import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Builder
public record TripDetailDto(
        Long id,
        String tripReference,
        Long packageId,
        String packageTitle,
        String destination,
        LocalDate travelDate,
        Instant startAt,
        Instant endAt,
        String pickupLocation,
        String dropLocation,
        String status,
        String notes,
        int totalTravellers,
        Integer vehicleCapacity,
        Integer remainingCapacity,
        VehicleSummaryDto vehicle,
        DriverSummaryDto driver,
        List<TripBookingSummaryDto> bookings,
        Instant createdAt,
        Instant updatedAt
) {}
