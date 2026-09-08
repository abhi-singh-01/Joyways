package in.joyway.service.payment;

public record RazorpayRefundResult(String refundId, long amountPaise, String status) {
}
