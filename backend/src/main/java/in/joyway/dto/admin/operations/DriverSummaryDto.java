package in.joyway.dto.admin.operations;

import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;

@Builder
public record DriverSummaryDto(
        Long id,
        String name,
        String phone,
        String licenseNumber,
        LocalDate licenseExpiryDate,
        String licenseStatus,
        String status,
        String currentTripReference,
        Instant createdAt
) {}
