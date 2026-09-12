package in.joyway.dto.admin;

import in.joyway.dto.booking.TravellerSummaryDto;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Builder
public record AdminBookingDetailDto(
        Long bookingId,
        String bookingReference,
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
        String bookingStatus,
        String paymentStatus,
        String razorpayOrderId,
        String razorpayPaymentId,
        String razorpayRefundId,
        String paymentRecordStatus,
        BigDecimal refundedAmount,
        boolean signatureVerified,
        Instant paymentDate,
        Instant createdAt,
        List<TravellerSummaryDto> travellers,
        String pickupPoint,
        boolean createdByAdmin,
        String adminNotes,
        String offlinePaymentReference
) {}
