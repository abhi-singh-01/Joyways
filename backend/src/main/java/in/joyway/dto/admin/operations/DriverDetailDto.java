package in.joyway.dto.admin.operations;

import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;

@Builder
public record DriverDetailDto(
        Long id,
        String name,
        String phone,
        String email,
        String licenseNumber,
        LocalDate licenseExpiryDate,
        String licenseStatus,
        String status,
        String notes,
        String currentTripReference,
        Instant createdAt,
        Instant updatedAt
) {}
