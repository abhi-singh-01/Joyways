package in.joyway.service.payment;

/**
 * Authoritative payment facts fetched from Razorpay (not client-supplied).
 */
public record RazorpayPaymentDetails(
        String paymentId,
        String orderId,
        long amountPaise,
        String status
) {
}
