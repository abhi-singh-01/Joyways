package in.joyway.dto.booking;

import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;

@Builder
public record CustomerTripInfoDto(
        String tripReference,
        String status,
        LocalDate travelDate,
        Instant startAt,
        Instant endAt,
        String pickupLocation,
        String dropLocation,
        String vehicleType,
        String driverContactName,
        String driverContactPhone
) {}
