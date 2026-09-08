package in.joyway.dto.admin;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record AdminBookingSummaryDto(
        Long bookingId,
        String bookingReference,
        String customerName,
        String customerEmail,
        String packageTitle,
        String destination,
        int numberOfTravellers,
        BigDecimal totalAmount,
        String currency,
        String bookingStatus,
        String paymentStatus,
        String razorpayOrderId,
        String razorpayPaymentId,
        Instant createdAt
) {}
