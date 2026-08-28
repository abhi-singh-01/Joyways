package in.joyway.dto.booking;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Builder
public record BookingDetailResponse(
        String bookingReference,
        Long bookingId,
        String customerName,
        String customerEmail,
        String customerMobile,
        String packageTitle,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        int numberOfTravellers,
        BigDecimal totalAmount,
        String currency,
        String status,
        String paymentStatus,
        String razorpayOrderId,
        String razorpayPaymentId,
        Instant paymentDate,
        Instant createdAt,
        List<TravellerSummaryDto> travellers,
        CustomerTripInfoDto tripInfo
) {}
