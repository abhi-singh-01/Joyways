package in.joyway.dto.payment;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentOrderResponse(
        String razorpayOrderId,
        String razorpayKeyId,
        BigDecimal amount,
        String currency,
        String bookingReference,
        Long bookingId
) {}
