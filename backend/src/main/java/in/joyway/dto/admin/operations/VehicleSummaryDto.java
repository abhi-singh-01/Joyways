package in.joyway.dto.admin.operations;

import lombok.Builder;

import java.time.Instant;

@Builder
public record VehicleSummaryDto(
        Long id,
        String registrationNumber,
        String vehicleType,
        String brandModel,
        Integer seatingCapacity,
        String ownershipType,
        String status,
        String currentTripReference,
        Instant createdAt
) {}
