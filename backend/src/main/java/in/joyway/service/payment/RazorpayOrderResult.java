package in.joyway.service.payment;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record RazorpayOrderResult(
        String orderId,
        BigDecimal amount,
        String currency
) {}
