package in.joyway.service.payment;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import in.joyway.config.RazorpayProperties;
import in.joyway.exception.RazorpayApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayGatewayImpl implements RazorpayGateway {

    private static final int MIN_AMOUNT_PAISE = 100;

    private final RazorpayProperties razorpayProperties;

    @Override
    public RazorpayOrderResult createOrder(String receiptReference, BigDecimal amountInr, String currency) {
        if (!razorpayProperties.isConfigured()) {
            throw new IllegalStateException("Razorpay is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }

        int amountPaise = toPaise(amountInr);
        if (amountPaise < MIN_AMOUNT_PAISE) {
            throw new IllegalArgumentException(
                    "Payment amount must be at least " + MIN_AMOUNT_PAISE + " paise (₹1.00).");
        }

        try {
            RazorpayClient client = new RazorpayClient(
                    razorpayProperties.getKeyId(),
                    razorpayProperties.getKeySecret()
            );

            JSONObject options = new JSONObject();
            options.put("amount", amountPaise);
            options.put("currency", currency);
            options.put("receipt", receiptReference);
            options.put("payment_capture", 1);

            Order order = client.orders.create(options);
            return RazorpayOrderResult.builder()
                    .orderId(order.get("id"))
                    .amount(amountInr)
                    .currency(currency)
                    .build();
        } catch (RazorpayException ex) {
            log.error("Razorpay order creation failed for receipt={}", receiptReference);
            if (isAuthFailure(ex)) {
                throw new RazorpayApiException("Razorpay authentication failed.", 401);
            }
            throw new RazorpayApiException("Unable to create payment order. Please try again.", 500);
        }
    }

    @Override
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        if (!razorpayProperties.isConfigured()) {
            return false;
        }
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);
            return Utils.verifyPaymentSignature(attributes, razorpayProperties.getKeySecret());
        } catch (Exception ex) {
            log.warn("Payment signature verification failed for orderId={}", orderId);
            return false;
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        if (!razorpayProperties.isWebhookConfigured()) {
            log.warn("Webhook received but RAZORPAY_WEBHOOK_SECRET is not configured.");
            return false;
        }
        try {
            return Utils.verifyWebhookSignature(payload, signature, razorpayProperties.getWebhookSecret());
        } catch (Exception ex) {
            log.warn("Webhook signature verification failed");
            return false;
        }
    }

    public static int toPaise(BigDecimal amountInr) {
        return amountInr.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
    }

    private static boolean isAuthFailure(RazorpayException ex) {
        String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        return message.contains("authentication") || message.contains("unauthorized") || message.contains("401");
    }
}
