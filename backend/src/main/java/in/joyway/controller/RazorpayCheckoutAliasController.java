package in.joyway.controller;

import in.joyway.dto.ApiResponse;
import in.joyway.dto.payment.CreatePaymentOrderRequest;
import in.joyway.dto.payment.PaymentOrderResponse;
import in.joyway.dto.payment.VerifyPaymentRequest;
import in.joyway.dto.payment.VerifyPaymentResponse;
import in.joyway.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Standard Razorpay Web Checkout aliases.
 * Delegates to the same booking-integrated payment flow as {@link PaymentController}.
 */
@RestController
@RequiredArgsConstructor
public class RazorpayCheckoutAliasController {

    private final PaymentService paymentService;

    @PostMapping("/api/create-order")
    public ApiResponse<PaymentOrderResponse> createOrder(
            @Valid @RequestBody CreatePaymentOrderRequest request
    ) {
        PaymentOrderResponse response = paymentService.createOrder(request);
        return ApiResponse.success(response, "Payment order created.");
    }

    @PostMapping("/api/verify-payment")
    public ApiResponse<VerifyPaymentResponse> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request
    ) {
        VerifyPaymentResponse response = paymentService.verifyPayment(request);
        return ApiResponse.success(response, "Payment verified successfully.");
    }
}
