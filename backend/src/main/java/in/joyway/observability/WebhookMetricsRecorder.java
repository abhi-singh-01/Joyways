package in.joyway.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Low-cardinality Micrometer counters and timers for Razorpay webhook processing.
 * Never use event IDs, order IDs, payment IDs, or booking references as metric tags.
 */
@Component
public class WebhookMetricsRecorder {

    public enum Outcome {
        SUCCESS,
        DUPLICATE,
        RETRYABLE,
        FAILED,
        REJECTED
    }

    public enum HandlerBucket {
        PAYMENT_CAPTURED("payment_captured"),
        PAYMENT_FAILED("payment_failed"),
        ORDER_PAID("order_paid"),
        REFUND("refund"),
        UNKNOWN("unknown"),
        NONE("none");

        private final String tagValue;

        HandlerBucket(String tagValue) {
            this.tagValue = tagValue;
        }

        public String tagValue() {
            return tagValue;
        }
    }

    private final Counter received;
    private final Counter invalidSignature;
    private final Counter missingSignature;
    private final Counter missingEventId;
    private final Counter duplicate;
    private final Counter unknownEvent;
    private final Counter unknownOrder;
    private final Counter bookingNotFound;
    private final Counter success;
    private final Counter failure;
    private final Counter retryableFailure;
    private final MeterRegistry meterRegistry;

    public WebhookMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        received = Counter.builder("joyway.payment.webhook.received")
                .description("Webhook deliveries entering processWebhook")
                .register(meterRegistry);
        invalidSignature = Counter.builder("joyway.payment.webhook.invalid_signature")
                .description("Webhook signature verification failures")
                .register(meterRegistry);
        missingSignature = Counter.builder("joyway.payment.webhook.missing_signature")
                .description("Webhook rejections due to missing X-Razorpay-Signature header")
                .register(meterRegistry);
        missingEventId = Counter.builder("joyway.payment.webhook.missing_event_id")
                .description("Webhook rejections due to missing event ID")
                .register(meterRegistry);
        duplicate = Counter.builder("joyway.payment.webhook.duplicate")
                .description("Duplicate webhook event deliveries already processed")
                .register(meterRegistry);
        unknownEvent = Counter.builder("joyway.payment.webhook.unknown_event")
                .description("Unhandled Razorpay webhook event types")
                .register(meterRegistry);
        unknownOrder = Counter.builder("joyway.payment.webhook.unknown_order")
                .description("Captured payments referencing unknown Razorpay orders")
                .register(meterRegistry);
        bookingNotFound = Counter.builder("joyway.payment.webhook.booking_not_found")
                .description("Retryable webhook failures when booking row is missing")
                .register(meterRegistry);
        success = Counter.builder("joyway.payment.webhook.success")
                .description("Webhook events acknowledged and marked processed")
                .register(meterRegistry);
        failure = Counter.builder("joyway.payment.webhook.failure")
                .description("Non-retryable webhook processing failures")
                .register(meterRegistry);
        retryableFailure = Counter.builder("joyway.payment.webhook.retryable_failure")
                .description("Retryable webhook processing failures")
                .register(meterRegistry);
    }

    public void recordReceived() {
        received.increment();
    }

    public void recordInvalidSignature() {
        invalidSignature.increment();
    }

    public void recordMissingSignature() {
        missingSignature.increment();
    }

    public void recordMissingEventId() {
        missingEventId.increment();
    }

    public void recordDuplicate() {
        duplicate.increment();
    }

    public void recordUnknownEvent() {
        unknownEvent.increment();
    }

    public void recordUnknownOrder() {
        unknownOrder.increment();
    }

    public void recordBookingNotFound() {
        bookingNotFound.increment();
    }

    public void recordSuccess() {
        success.increment();
    }

    public void recordFailure() {
        failure.increment();
    }

    public void recordRetryableFailure() {
        retryableFailure.increment();
    }

    public void recordProcessingDuration(long startNanos, Outcome outcome) {
        meterRegistry.timer(
                        "joyway.payment.webhook.processing.duration",
                        "outcome",
                        outcomeTag(outcome)
                )
                .record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
    }

    private static String outcomeTag(Outcome outcome) {
        return switch (outcome) {
            case SUCCESS -> "success";
            case DUPLICATE -> "duplicate";
            case RETRYABLE -> "retryable";
            case FAILED -> "failed";
            case REJECTED -> "rejected";
        };
    }
}
