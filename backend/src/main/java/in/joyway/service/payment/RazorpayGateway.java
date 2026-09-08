package in.joyway.service.payment;

import java.math.BigDecimal;
import java.util.Optional;

public interface RazorpayGateway {

    RazorpayOrderResult createOrder(String receiptReference, BigDecimal amountInr, String currency);

    boolean verifyPaymentSignature(String orderId, String paymentId, String signature);

    boolean verifyWebhookSignature(String payload, String signature);

    /**
     * Fetches payment details from Razorpay for server-side amount/order verification.
     */
    Optional<RazorpayPaymentDetails> fetchPayment(String paymentId);

    RazorpayRefundResult createRefund(String paymentId, int amountPaise, String receiptReference);
}
