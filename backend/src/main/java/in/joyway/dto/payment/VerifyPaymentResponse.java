package in.joyway.dto.payment;

import in.joyway.dto.booking.BookingReceiptDto;
import lombok.Builder;

@Builder
public record VerifyPaymentResponse(
        String bookingReference,
        String status,
        String paymentStatus,
        BookingReceiptDto receipt
) {}
