package in.joyway.dto.admin.operations;

import lombok.Builder;

@Builder
public record TripBookingSummaryDto(
        Long bookingId,
        String bookingReference,
        String customerName,
        int numberOfTravellers,
        String bookingStatus,
        String paymentStatus
) {}
