package in.joyway.dto.booking;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Builder
public record BookingResponse(
        String bookingReference,
        Long bookingId,
        String packageTitle,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        int numberOfTravellers,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        String couponCode,
        String currency,
        String status,
        String paymentStatus,
        Instant createdAt,
        Instant paymentExpiresAt,
        String pickupPoint,
        List<TravellerSummaryDto> travellers
) {}
