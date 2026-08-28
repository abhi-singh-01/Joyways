package in.joyway.service.payment;

import java.math.BigDecimal;

public interface RazorpayGateway {

    RazorpayOrderResult createOrder(String receiptReference, BigDecimal amountInr, String currency);

    boolean verifyPaymentSignature(String orderId, String paymentId, String signature);

    boolean verifyWebhookSignature(String payload, String signature);
}
