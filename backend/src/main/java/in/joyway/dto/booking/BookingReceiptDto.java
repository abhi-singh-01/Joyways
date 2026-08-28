package in.joyway.dto.booking;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Builder
public record BookingReceiptDto(
        String brandName,
        String title,
        String bookingReference,
        String customerName,
        String packageTitle,
        String destination,
        LocalDate travelDate,
        LocalDate endDate,
        int numberOfTravellers,
        BigDecimal packageAmount,
        String currency,
        String paymentStatus,
        String razorpayPaymentId,
        Instant bookingDate,
        Instant paymentDate
) {}
