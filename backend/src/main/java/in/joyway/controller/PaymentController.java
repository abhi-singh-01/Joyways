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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/orders")
    public ApiResponse<PaymentOrderResponse> createOrder(
            @Valid @RequestBody CreatePaymentOrderRequest request
    ) {
        PaymentOrderResponse response = paymentService.createOrder(request);
        return ApiResponse.success(response, "Payment order created.");
    }

    @PostMapping("/verify")
    public ApiResponse<VerifyPaymentResponse> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request
    ) {
        VerifyPaymentResponse response = paymentService.verifyPayment(request);
        return ApiResponse.success(response, "Payment verified successfully.");
    }
}
