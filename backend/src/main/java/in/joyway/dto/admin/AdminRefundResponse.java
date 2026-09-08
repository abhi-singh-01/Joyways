package in.joyway.dto.admin;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AdminRefundResponse(
        Long bookingId,
        String bookingReference,
        String bookingStatus,
        String paymentStatus,
        String paymentRecordStatus,
        String razorpayRefundId,
        String refundState,
        BigDecimal refundedAmount,
        BigDecimal totalAmount
) {}
