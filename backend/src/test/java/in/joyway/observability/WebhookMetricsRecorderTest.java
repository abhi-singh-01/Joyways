package in.joyway.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookMetricsRecorderTest {

    private MeterRegistry meterRegistry;
    private WebhookMetricsRecorder recorder;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        recorder = new WebhookMetricsRecorder(meterRegistry);
    }

    @Test
    void countersIncrementCorrectly() {
        recorder.recordReceived();
        recorder.recordInvalidSignature();
        recorder.recordMissingSignature();
        recorder.recordMissingEventId();
        recorder.recordDuplicate();
        recorder.recordUnknownEvent();
        recorder.recordUnknownOrder();
        recorder.recordBookingNotFound();
        recorder.recordSuccess();
        recorder.recordFailure();
        recorder.recordRetryableFailure();

        assertCounterCount("joyway.payment.webhook.received", 1);
        assertCounterCount("joyway.payment.webhook.invalid_signature", 1);
        assertCounterCount("joyway.payment.webhook.missing_signature", 1);
        assertCounterCount("joyway.payment.webhook.missing_event_id", 1);
        assertCounterCount("joyway.payment.webhook.duplicate", 1);
        assertCounterCount("joyway.payment.webhook.unknown_event", 1);
        assertCounterCount("joyway.payment.webhook.unknown_order", 1);
        assertCounterCount("joyway.payment.webhook.booking_not_found", 1);
        assertCounterCount("joyway.payment.webhook.success", 1);
        assertCounterCount("joyway.payment.webhook.failure", 1);
        assertCounterCount("joyway.payment.webhook.retryable_failure", 1);
    }

    @Test
    void timerRecordsWithBoundedOutcomeTagOnly() {
        long startNanos = System.nanoTime() - 1_000_000L;
        recorder.recordProcessingDuration(startNanos, WebhookMetricsRecorder.Outcome.SUCCESS);

        Timer timer = meterRegistry.find("joyway.payment.webhook.processing.duration").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.NANOSECONDS)).isGreaterThan(0);

        Set<String> tagKeys = meterRegistry.getMeters().stream()
                .filter(m -> m.getId().getName().equals("joyway.payment.webhook.processing.duration"))
                .flatMap(m -> m.getId().getTags().stream())
                .map(tag -> tag.getKey() + "=" + tag.getValue())
                .collect(Collectors.toSet());

        assertThat(tagKeys)
                .containsExactly("outcome=success")
                .noneMatch(tag -> tag.startsWith("event_id="))
                .noneMatch(tag -> tag.startsWith("order_id="))
                .noneMatch(tag -> tag.startsWith("payment_id="));
    }

    @Test
    void outcomeTagValuesAreBounded() {
        for (WebhookMetricsRecorder.Outcome outcome : WebhookMetricsRecorder.Outcome.values()) {
            recorder.recordProcessingDuration(System.nanoTime(), outcome);
        }

        Set<String> outcomeValues = meterRegistry.find("joyway.payment.webhook.processing.duration")
                .timers().stream()
                .map(timer -> timer.getId().getTag("outcome"))
                .collect(Collectors.toSet());

        assertThat(outcomeValues).containsExactlyInAnyOrder(
                "success", "duplicate", "retryable", "failed", "rejected"
        );
    }

    private void assertCounterCount(String name, double expected) {
        Counter counter = meterRegistry.find(name).counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(expected);
    }
}
