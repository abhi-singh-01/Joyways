package in.joyway.controller;

import in.joyway.observability.WebhookMetricsRecorder;
import in.joyway.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final PaymentService paymentService;
    private final WebhookMetricsRecorder webhookMetricsRecorder;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId
    ) {
        if (signature == null || signature.isBlank()) {
            log.warn("event=payment.webhook.rejected reason=missing_signature");
            webhookMetricsRecorder.recordMissingSignature();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (eventId == null || eventId.isBlank()) {
            log.warn("event=payment.webhook.rejected reason=missing_event_id");
            webhookMetricsRecorder.recordMissingEventId();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        paymentService.processWebhook(payload, signature, eventId);
        return ResponseEntity.ok().build();
    }
}
