package in.joyway.dto.booking;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record BookingReviewDto(
        Long packageId,
        String packageTitle,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        String duration,
        int numberOfTravellers,
        BigDecimal pricePerPerson,
        BigDecimal subtotal,
        BigDecimal taxesAndFees,
        BigDecimal totalAmount,
        String currency,
        int availableSeats,
        boolean bookable
) {}
