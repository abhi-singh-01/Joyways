package in.joyway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.joyway.config.RazorpayProperties;
import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.Payment;
import in.joyway.domain.entity.PaymentWebhookEvent;
import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.enums.BookingStatus;
import in.joyway.domain.enums.PaymentRecordStatus;
import in.joyway.domain.enums.PaymentStatus;
import in.joyway.domain.repository.BookingRepository;
import in.joyway.domain.repository.PaymentRepository;
import in.joyway.domain.repository.PaymentWebhookEventRepository;
import in.joyway.domain.repository.TravelPackageRepository;
import in.joyway.dto.admin.AdminRefundRequest;
import in.joyway.dto.admin.AdminRefundResponse;
import in.joyway.dto.booking.BookingReceiptDto;
import in.joyway.dto.payment.CreatePaymentOrderRequest;
import in.joyway.dto.payment.PaymentOrderResponse;
import in.joyway.dto.payment.VerifyPaymentRequest;
import in.joyway.dto.payment.VerifyPaymentResponse;
import in.joyway.exception.PaymentVerificationException;
import in.joyway.exception.ResourceNotFoundException;
import in.joyway.notification.BookingNotificationService;
import in.joyway.observability.WebhookMetricsRecorder;
import in.joyway.observability.WebhookMetricsRecorder.HandlerBucket;
import in.joyway.observability.WebhookMetricsRecorder.Outcome;
import in.joyway.service.audit.AuditLogService;
import in.joyway.service.operations.ConfirmedBookingTripAssignmentService;
import in.joyway.service.payment.RazorpayGateway;
import in.joyway.service.payment.RazorpayGatewayImpl;
import in.joyway.service.payment.RazorpayOrderResult;
import in.joyway.service.payment.RazorpayPaymentDetails;
import in.joyway.service.payment.RazorpayRefundResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final BookingQueryService bookingQueryService;
    private final RazorpayGateway razorpayGateway;
    private final RazorpayProperties razorpayProperties;
    private final ReceiptService receiptService;
    private final ObjectMapper objectMapper;
    private final BookingNotificationService bookingNotificationService;
    private final WebhookMetricsRecorder webhookMetricsRecorder;
    private final TravelPackageRepository packageRepository;
    private final ConfirmedBookingTripAssignmentService tripAssignmentService;
    private final AuditLogService auditLogService;

    @Transactional
    public PaymentOrderResponse createOrder(CreatePaymentOrderRequest request) {
        // Verify ownership before touching the row, then lock it so that concurrent
        // order creation for the same booking cannot produce two Razorpay orders.
        bookingQueryService.findBookingWithAccessCheck(
                request.getBookingReference(),
                request.getCustomerEmail()
        );

        Booking booking = bookingRepository.findByBookingReferenceForUpdate(request.getBookingReference())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));

        if (booking.getStatus() == BookingStatus.CONFIRMED
                || booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalArgumentException("This booking is already paid.");
        }

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalArgumentException("This booking is not eligible for payment.");
        }

        Payment existing = paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(booking.getId())
                .orElse(null);
        if (existing != null && existing.getStatus() == PaymentRecordStatus.CREATED
                && existing.getRazorpayOrderId() != null
                && existing.getAmount() != null
                && existing.getAmount().compareTo(booking.getTotalAmount()) == 0) {
            return PaymentOrderResponse.builder()
                    .razorpayOrderId(existing.getRazorpayOrderId())
                    .razorpayKeyId(razorpayProperties.getKeyId())
                    .amount(booking.getTotalAmount())
                    .currency(booking.getCurrency())
                    .bookingReference(booking.getBookingReference())
                    .bookingId(booking.getId())
                    .build();
        }

        supersedeCreatedOrdersForBooking(booking.getId());

        RazorpayOrderResult order = razorpayGateway.createOrder(
                booking.getBookingReference(),
                booking.getTotalAmount(),
                booking.getCurrency()
        );

        Payment payment = Payment.builder()
                .booking(booking)
                .razorpayOrderId(order.orderId())
                .amount(booking.getTotalAmount())
                .currency(booking.getCurrency())
                .status(PaymentRecordStatus.CREATED)
                .signatureVerified(false)
                .build();
        paymentRepository.save(payment);

        log.info("event=payment.order.created bookingReference={} orderId={}",
                booking.getBookingReference(), order.orderId());

        return PaymentOrderResponse.builder()
                .razorpayOrderId(order.orderId())
                .razorpayKeyId(razorpayProperties.getKeyId())
                .amount(booking.getTotalAmount())
                .currency(booking.getCurrency())
                .bookingReference(booking.getBookingReference())
                .bookingId(booking.getId())
                .build();
    }

    @Transactional
    public VerifyPaymentResponse verifyPayment(VerifyPaymentRequest request) {
        Booking booking = bookingRepository.findByBookingReferenceForUpdate(request.getBookingReference())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));

        bookingQueryService.findBookingWithAccessCheck(
                request.getBookingReference(),
                request.getCustomerEmail()
        );

        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new PaymentVerificationException("Payment order not found."));

        if (!payment.getBooking().getId().equals(booking.getId())) {
            throw new PaymentVerificationException("Payment does not belong to this booking.");
        }

        confirmPayment(booking, payment, request.getRazorpayPaymentId(), request.getRazorpaySignature());

        BookingReceiptDto receipt = receiptService.buildReceipt(booking, payment);
        return VerifyPaymentResponse.builder()
                .bookingReference(booking.getBookingReference())
                .status(booking.getStatus().name())
                .paymentStatus(booking.getPaymentStatus().name())
                .receipt(receipt)
                .build();
    }

    @Transactional
    public void processWebhook(String payload, String signature, String eventId) {
        long startNanos = System.nanoTime();
        Outcome timerOutcome = Outcome.FAILED;
        HandlerBucket handlerBucket = HandlerBucket.NONE;
        String eventType = "";

        log.info("event=payment.webhook.received eventId={}", eventId);
        webhookMetricsRecorder.recordReceived();

        try {
            if (eventId == null || eventId.isBlank()) {
                log.warn("event=payment.webhook.rejected reason=missing_event_id eventId={}", eventId);
                webhookMetricsRecorder.recordMissingEventId();
                timerOutcome = Outcome.REJECTED;
                throw new PaymentVerificationException("Webhook event id is required.");
            }

            if (!razorpayGateway.verifyWebhookSignature(payload, signature)) {
                log.warn("event=payment.webhook.failed reason=signature_invalid eventId={}", eventId);
                webhookMetricsRecorder.recordInvalidSignature();
                timerOutcome = Outcome.FAILED;
                throw new PaymentVerificationException("Invalid webhook signature.");
            }

            try {
                JsonNode root = objectMapper.readTree(payload);
                eventType = root.path("event").asText("");
                String payloadHash = sha256(payload);

                PaymentWebhookEvent webhookEvent = acquireWebhookEvent(eventId, eventType, payloadHash);

                if (Boolean.TRUE.equals(webhookEvent.getProcessed())) {
                    log.info("event=payment.webhook.duplicate eventId={}", eventId);
                    webhookMetricsRecorder.recordDuplicate();
                    timerOutcome = Outcome.DUPLICATE;
                    return;
                }

                handlerBucket = handlerBucketForEventType(eventType);

                WebhookHandlerOutcome outcome = switch (eventType) {
                    case "payment.captured" -> handlePaymentSuccess(root);
                    case "order.paid" -> {
                        handleOrderPaidInformational(root);
                        yield WebhookHandlerOutcome.ACKNOWLEDGED;
                    }
                    case "payment.failed" -> handlePaymentFailed(root);
                    case "refund.processed" -> handleRefundProcessed(root);
                    case "refund.failed" -> handleRefundFailed(root);
                    default -> {
                        log.info("event=payment.webhook.unhandled handler_bucket=unknown eventType={}", eventType);
                        webhookMetricsRecorder.recordUnknownEvent();
                        handlerBucket = HandlerBucket.UNKNOWN;
                        yield WebhookHandlerOutcome.ACKNOWLEDGED;
                    }
                };

                if (outcome == WebhookHandlerOutcome.RETRYABLE) {
                    log.warn("event=payment.webhook.retryable eventId={} eventType={}", eventId, eventType);
                    webhookMetricsRecorder.recordBookingNotFound();
                    webhookMetricsRecorder.recordRetryableFailure();
                    timerOutcome = Outcome.RETRYABLE;
                    throw new IllegalStateException("Webhook processing incomplete.");
                }

                webhookEvent.setProcessed(true);
                webhookEventRepository.save(webhookEvent);
                webhookMetricsRecorder.recordSuccess();
                timerOutcome = Outcome.SUCCESS;
                log.info(
                        "event=payment.webhook.processed outcome=success handler_bucket={} eventId={} eventType={} durationMs={}",
                        handlerBucket.tagValue(),
                        eventId,
                        eventType,
                        durationMillis(startNanos)
                );
            } catch (PaymentVerificationException ex) {
                webhookMetricsRecorder.recordFailure();
                timerOutcome = Outcome.FAILED;
                throw ex;
            } catch (IllegalStateException ex) {
                if ("Webhook processing incomplete.".equals(ex.getMessage())) {
                    throw ex;
                }
                log.error("event=payment.webhook.failed reason=processing_error eventId={} type={}",
                        eventId, ex.getClass().getSimpleName());
                webhookMetricsRecorder.recordFailure();
                timerOutcome = Outcome.FAILED;
                throw new IllegalStateException("Webhook processing failed.");
            } catch (Exception ex) {
                log.error("event=payment.webhook.failed reason=processing_error eventId={} type={}",
                        eventId, ex.getClass().getSimpleName());
                webhookMetricsRecorder.recordFailure();
                timerOutcome = Outcome.FAILED;
                throw new IllegalStateException("Webhook processing failed.");
            }
        } catch (PaymentVerificationException ex) {
            throw ex;
        } finally {
            webhookMetricsRecorder.recordProcessingDuration(startNanos, timerOutcome);
        }
    }

    private static HandlerBucket handlerBucketForEventType(String eventType) {
        return switch (eventType) {
            case "payment.captured" -> HandlerBucket.PAYMENT_CAPTURED;
            case "payment.failed" -> HandlerBucket.PAYMENT_FAILED;
            case "order.paid" -> HandlerBucket.ORDER_PAID;
            case "refund.processed", "refund.failed" -> HandlerBucket.REFUND;
            default -> HandlerBucket.UNKNOWN;
        };
    }

    private static long durationMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    /**
     * Locks or creates the webhook idempotency row. Retries reuse an existing {@code processed=false}
     * row; only {@code processed=true} deliveries are treated as duplicates.
     */
    private PaymentWebhookEvent acquireWebhookEvent(String eventId, String eventType, String payloadHash) {
        Optional<PaymentWebhookEvent> existing = webhookEventRepository.findByEventIdForUpdate(eventId);
        if (existing.isPresent()) {
            PaymentWebhookEvent event = existing.get();
            if (!payloadHash.equals(event.getPayloadHash())) {
                log.warn("event=payment.webhook.payload_mismatch eventId={}", eventId);
                throw new PaymentVerificationException("Webhook payload mismatch for event id.");
            }
            return event;
        }

        try {
            return webhookEventRepository.save(PaymentWebhookEvent.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .payloadHash(payloadHash)
                    .processed(false)
                    .build());
        } catch (DataIntegrityViolationException ex) {
            return webhookEventRepository.findByEventIdForUpdate(eventId)
                    .orElseThrow(() -> new IllegalStateException("Webhook event conflict without row."));
        }
    }

    private void handleOrderPaidInformational(JsonNode root) {
        JsonNode orderEntity = root.path("payload").path("order").path("entity");
        String orderId = orderEntity.path("id").asText(null);
        log.info(
                "event=payment.webhook.order_paid_informational orderId={} (booking confirmation uses payment.captured only)",
                orderId
        );
    }

    /**
     * Whether the webhook delivery can be acknowledged ({@code processed=true}) or must remain
     * retryable ({@code processed=false} with a non-2xx response to Razorpay).
     */
    private enum WebhookHandlerOutcome {
        /** Handler finished; same payload will not succeed on retry. */
        ACKNOWLEDGED,
        /** Transient failure — leave {@code processed=false} so Razorpay can retry. */
        RETRYABLE
    }

    private WebhookHandlerOutcome handlePaymentSuccess(JsonNode root) {
        JsonNode paymentNode = extractPaymentNode(root);
        if (paymentNode == null) {
            log.warn("event=payment.webhook.unrecoverable reason=missing_payment_payload");
            return WebhookHandlerOutcome.ACKNOWLEDGED;
        }

        String orderId = paymentNode.path("order_id").asText(null);
        String paymentId = paymentNode.path("id").asText(null);
        if (orderId == null || paymentId == null) {
            log.warn("event=payment.webhook.unrecoverable reason=missing_payment_identifiers");
            return WebhookHandlerOutcome.ACKNOWLEDGED;
        }

        if (paymentRepository.findByRazorpayPaymentId(paymentId).isPresent()) {
            log.info("Duplicate payment webhook ignored: paymentId={}", paymentId);
            return WebhookHandlerOutcome.ACKNOWLEDGED;
        }

        Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                .orElse(null);
        if (payment == null) {
            // Order rows are committed before the client receives a Razorpay order id, so an
            // unknown order id will not become known on retry with the same event payload.
            log.warn("event=payment.webhook.unrecoverable reason=unknown_order orderId={} paymentId={}",
                    orderId, paymentId);
            webhookMetricsRecorder.recordUnknownOrder();
            return WebhookHandlerOutcome.ACKNOWLEDGED;
        }

        Booking booking = bookingRepository.findByBookingReferenceForUpdate(
                payment.getBooking().getBookingReference()
        ).orElse(null);
        if (booking == null) {
            log.warn("event=payment.webhook.retryable reason=booking_not_found orderId={} paymentId={}",
                    orderId, paymentId);
            return WebhookHandlerOutcome.RETRYABLE;
        }

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            return WebhookHandlerOutcome.ACKNOWLEDGED;
        }

        // Money was captured by Razorpay, so always persist the payment record for
        // reconciliation. Only the booking transition is conditional.
        payment.setRazorpayPaymentId(paymentId);
        payment.setSignatureVerified(true);
        payment.setStatus(PaymentRecordStatus.CAPTURED);
        paymentRepository.save(payment);

        if (!capturedAmountMatchesBooking(booking, payment, paymentNode)) {
            log.warn("event=payment.amount_mismatch bookingReference={} paymentId={}",
                    booking.getBookingReference(), paymentId);
            notifyPaymentOnInactiveBookingSafely(booking);
            return WebhookHandlerOutcome.ACKNOWLEDGED;
        }

        if (!isPayable(booking.getStatus())) {
            log.warn("Captured payment for non-payable booking: reference={}, status={}, paymentId={}",
                    booking.getBookingReference(), booking.getStatus(), paymentId);
            notifyPaymentOnInactiveBookingSafely(booking);
            return WebhookHandlerOutcome.ACKNOWLEDGED;
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PAID);
        bookingRepository.save(booking);

        log.info("event=payment.webhook.confirmed bookingReference={} paymentId={}",
                booking.getBookingReference(), paymentId);

        notifyPaymentSuccessSafely(booking, payment);
        tryAssignTripSafely(booking);
        return WebhookHandlerOutcome.ACKNOWLEDGED;
    }

    /**
     * A booking may only transition to CONFIRMED while it still holds its seats.
     * Cancelled, expired, refunded and completed bookings have already released or
     * consumed their seat allocation.
     */
    private static boolean orderAmountMatchesBooking(Booking booking, Payment payment) {
        return payment.getAmount() != null
                && booking.getTotalAmount() != null
                && payment.getAmount().compareTo(booking.getTotalAmount()) == 0;
    }

    private static boolean capturedAmountMatchesBooking(
            Booking booking,
            Payment payment,
            JsonNode paymentNode
    ) {
        if (!orderAmountMatchesBooking(booking, payment)) {
            return false;
        }
        JsonNode amountNode = paymentNode.path("amount");
        if (amountNode.isMissingNode() || !amountNode.canConvertToLong()) {
            return false;
        }
        int expectedPaise = RazorpayGatewayImpl.toPaise(booking.getTotalAmount());
        return amountNode.asLong() == expectedPaise;
    }

    private static boolean isPayable(BookingStatus status) {
        return status == BookingStatus.PENDING_PAYMENT;
    }

    private WebhookHandlerOutcome handlePaymentFailed(JsonNode root) {
        JsonNode paymentNode = extractPaymentNode(root);
        if (paymentNode == null) {
            log.warn("event=payment.webhook.unrecoverable reason=missing_payment_payload eventType=payment.failed");
            return WebhookHandlerOutcome.ACKNOWLEDGED;
        }

        String orderId = paymentNode.path("order_id").asText(null);
        if (orderId == null) {
            log.warn("event=payment.webhook.unrecoverable reason=missing_order_id eventType=payment.failed");
            return WebhookHandlerOutcome.ACKNOWLEDGED;
        }

        paymentRepository.findByRazorpayOrderId(orderId).ifPresent(payment -> {
            payment.setStatus(PaymentRecordStatus.FAILED);
            paymentRepository.save(payment);

            Booking booking = payment.getBooking();
            if (booking.getPaymentStatus() == PaymentStatus.PENDING) {
                booking.setPaymentStatus(PaymentStatus.FAILED);
                bookingRepository.save(booking);
                notifyPaymentFailedSafely(booking);
            }
        });
        return WebhookHandlerOutcome.ACKNOWLEDGED;
    }

    private JsonNode extractPaymentNode(JsonNode root) {
        JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
        if (!paymentEntity.isMissingNode() && paymentEntity.has("order_id")) {
            return paymentEntity;
        }
        JsonNode orderPayments = root.path("payload").path("order").path("entity").path("payments");
        if (orderPayments.isArray() && !orderPayments.isEmpty()) {
            String paymentId = orderPayments.get(0).asText();
            JsonNode node = objectMapper.createObjectNode();
            ((com.fasterxml.jackson.databind.node.ObjectNode) node).put("id", paymentId);
            ((com.fasterxml.jackson.databind.node.ObjectNode) node).put(
                    "order_id",
                    root.path("payload").path("order").path("entity").path("id").asText()
            );
            return node;
        }
        return null;
    }

    private void confirmPayment(Booking booking, Payment payment, String paymentId, String signature) {
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            if (paymentId.equals(payment.getRazorpayPaymentId())) {
                return;
            }
            throw new PaymentVerificationException("This booking is already paid.");
        }

        // Seats are released back to the package when a booking is cancelled or expires.
        // Confirming such a booking here would resell those seats, so the payment must be
        // handled as a refund case by the team instead of silently confirming.
        if (!isPayable(booking.getStatus())) {
            log.warn("Payment verification attempted on non-payable booking: reference={}, status={}",
                    booking.getBookingReference(), booking.getStatus());
            throw new PaymentVerificationException(
                    "This booking is no longer active and cannot be paid. Please contact support.");
        }

        if (paymentRepository.findByRazorpayPaymentId(paymentId).isPresent()) {
            throw new PaymentVerificationException("This payment has already been processed.");
        }

        if (!orderAmountMatchesBooking(booking, payment)) {
            throw new PaymentVerificationException("Payment amount does not match this booking.");
        }

        boolean valid = razorpayGateway.verifyPaymentSignature(
                payment.getRazorpayOrderId(),
                paymentId,
                signature
        );
        if (!valid) {
            payment.setStatus(PaymentRecordStatus.FAILED);
            paymentRepository.save(payment);
            booking.setPaymentStatus(PaymentStatus.FAILED);
            bookingRepository.save(booking);
            notifyPaymentFailedSafely(booking);
            throw new PaymentVerificationException("Payment verification failed.");
        }

        RazorpayPaymentDetails razorpayPayment = razorpayGateway.fetchPayment(paymentId)
                .orElseThrow(() -> new PaymentVerificationException("Unable to verify payment with Razorpay."));
        if (razorpayPayment.orderId() == null
                || !payment.getRazorpayOrderId().equals(razorpayPayment.orderId())) {
            throw new PaymentVerificationException("Payment does not belong to this order.");
        }
        int expectedPaise = RazorpayGatewayImpl.toPaise(booking.getTotalAmount());
        if (razorpayPayment.amountPaise() != expectedPaise) {
            throw new PaymentVerificationException("Payment amount does not match this booking.");
        }

        payment.setRazorpayPaymentId(paymentId);
        payment.setRazorpaySignature(signature);
        payment.setSignatureVerified(true);
        payment.setStatus(PaymentRecordStatus.CAPTURED);
        paymentRepository.save(payment);

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PAID);
        bookingRepository.save(booking);

        log.info("event=payment.verified bookingReference={} paymentId={}",
                booking.getBookingReference(), paymentId);

        notifyPaymentSuccessSafely(booking, payment);
        tryAssignTripSafely(booking);
    }

    @Transactional
    public AdminRefundResponse refundBooking(Long bookingId, AdminRefundRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));
        booking = bookingRepository.findByBookingReferenceForUpdate(booking.getBookingReference())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));

        Payment payment = paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(booking.getId())
                .orElseThrow(() -> new IllegalArgumentException("No payment found for this booking."));

        if (payment.getRazorpayPaymentId() == null || payment.getRazorpayPaymentId().isBlank()) {
            throw new IllegalArgumentException("This booking has no captured Razorpay payment to refund.");
        }
        if (payment.getStatus() == PaymentRecordStatus.REFUNDED) {
            throw new IllegalArgumentException("This payment is already refunded.");
        }
        if (payment.getStatus() != PaymentRecordStatus.CAPTURED
                && payment.getStatus() != PaymentRecordStatus.PARTIALLY_REFUNDED) {
            throw new IllegalArgumentException("Only captured payments can be refunded.");
        }
        if (payment.getRazorpayRefundId() != null
                && payment.getStatus() == PaymentRecordStatus.CAPTURED
                && refundedAmountOrZero(payment).compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("A refund is already pending for this payment.");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Completed trips cannot be refunded from admin. Contact finance.");
        }

        BigDecimal remaining = payment.getAmount().subtract(refundedAmountOrZero(payment));
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("There is no remaining amount to refund.");
        }

        BigDecimal requested = request != null && request.getAmount() != null
                ? request.getAmount().setScale(2, RoundingMode.HALF_UP)
                : remaining;
        if (requested.compareTo(remaining) > 0) {
            throw new IllegalArgumentException("Refund amount exceeds the remaining paid amount.");
        }

        int amountPaise = RazorpayGatewayImpl.toPaise(requested);
        RazorpayRefundResult result = razorpayGateway.createRefund(
                payment.getRazorpayPaymentId(),
                amountPaise,
                booking.getBookingReference()
        );

        String refundState = result.status() == null ? "processed" : result.status().toLowerCase();
        payment.setRazorpayRefundId(result.refundId());

        if ("failed".equals(refundState)) {
            paymentRepository.save(payment);
            throw new IllegalArgumentException("Razorpay declined this refund.");
        }

        if ("pending".equals(refundState) || "created".equals(refundState)) {
            paymentRepository.save(payment);
            auditLogService.log("REFUND_PENDING", "BOOKING", booking.getId(),
                    "Refund " + result.refundId() + " pending for " + booking.getBookingReference() + ".");
            log.info("event=payment.refund.pending bookingReference={} refundId={}",
                    booking.getBookingReference(), result.refundId());
            return toRefundResponse(booking, payment, "PENDING");
        }

        applyRefundCapture(booking, payment, requested, result.refundId());
        auditLogService.log("REFUND_ISSUED", "BOOKING", booking.getId(),
                "Refund " + result.refundId() + " of " + requested + " " + booking.getCurrency()
                        + " for " + booking.getBookingReference() + ".");
        notifyRefundSafely(booking, payment, requested);
        return toRefundResponse(booking, payment, "PROCESSED");
    }

    private WebhookHandlerOutcome handleRefundProcessed(JsonNode root) {
        JsonNode refundNode = root.path("payload").path("refund").path("entity");
        if (refundNode.isMissingNode()) {
            log.warn("event=payment.webhook.unrecoverable reason=missing_refund_payload");
            return WebhookHandlerOutcome.ACKNOWLEDGED;
        }

        String refundId = refundNode.path("id").asText(null);
        String paymentId = refundNode.path("payment_id").asText(null);
        JsonNode amountNode = refundNode.path("amount");
        if (refundId == null || paymentId == null || amountNode.isMissingNode() || !amountNode.canConvertToLong()) {
            log.warn("event=payment.webhook.unrecoverable reason=missing_refund_identifiers");
            return WebhookHandlerOutcome.ACKNOWLEDGED;
        }

        Payment payment = paymentRepository.findByRazorpayPaymentId(paymentId).orElse(null);
        if (payment == null) {
            log.warn("event=payment.webhook.unrecoverable reason=unknown_payment_for_refund paymentId={}", paymentId);
            return WebhookHandlerOutcome.ACKNOWLEDGED;
        }

        Booking booking = bookingRepository.findByBookingReferenceForUpdate(
                payment.getBooking().getBookingReference()
        ).orElse(null);
        if (booking == null) {
            return WebhookHandlerOutcome.RETRYABLE;
        }

        if (payment.getStatus() == PaymentRecordStatus.REFUNDED
                && refundId.equals(payment.getRazorpayRefundId())) {
            return WebhookHandlerOutcome.ACKNOWLEDGED;
        }

        BigDecimal refundAmount = paiseToInr(amountNode.asLong());
        applyRefundCapture(booking, payment, refundAmount, refundId);
        notifyRefundSafely(booking, payment, refundAmount);
        log.info("event=payment.webhook.refunded bookingReference={} refundId={}",
                booking.getBookingReference(), refundId);
        return WebhookHandlerOutcome.ACKNOWLEDGED;
    }

    private WebhookHandlerOutcome handleRefundFailed(JsonNode root) {
        JsonNode refundNode = root.path("payload").path("refund").path("entity");
        String refundId = refundNode.path("id").asText(null);
        String paymentId = refundNode.path("payment_id").asText(null);
        if (paymentId == null) {
            return WebhookHandlerOutcome.ACKNOWLEDGED;
        }
        paymentRepository.findByRazorpayPaymentId(paymentId).ifPresent(payment -> {
            if (refundId != null && refundId.equals(payment.getRazorpayRefundId())
                    && payment.getStatus() == PaymentRecordStatus.CAPTURED) {
                payment.setRazorpayRefundId(null);
                paymentRepository.save(payment);
                log.warn("event=payment.refund.failed paymentId={} refundId={}", paymentId, refundId);
            }
        });
        return WebhookHandlerOutcome.ACKNOWLEDGED;
    }

    private void applyRefundCapture(Booking booking, Payment payment, BigDecimal refundAmount, String refundId) {
        BigDecimal already = refundedAmountOrZero(payment);
        BigDecimal next = already.add(refundAmount);
        if (next.compareTo(payment.getAmount()) > 0) {
            next = payment.getAmount();
        }
        payment.setRefundedAmount(next);
        payment.setRazorpayRefundId(refundId);
        boolean full = next.compareTo(payment.getAmount()) >= 0;
        payment.setStatus(full ? PaymentRecordStatus.REFUNDED : PaymentRecordStatus.PARTIALLY_REFUNDED);
        paymentRepository.save(payment);

        if (full) {
            restoreSeatsIfHeld(booking);
            booking.setStatus(BookingStatus.REFUNDED);
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
        } else {
            booking.setPaymentStatus(PaymentStatus.PARTIALLY_REFUNDED);
        }
        bookingRepository.save(booking);
    }

    private void restoreSeatsIfHeld(Booking booking) {
        if (booking.getStatus() != BookingStatus.CONFIRMED
                && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return;
        }
        TravelPackage pkg = packageRepository.findByIdForUpdate(booking.getTravelPackage().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found."));
        pkg.setAvailableSeats(pkg.getAvailableSeats() + booking.getNumberOfTravellers());
        packageRepository.save(pkg);
    }

    private static BigDecimal refundedAmountOrZero(Payment payment) {
        return payment.getRefundedAmount() == null ? BigDecimal.ZERO : payment.getRefundedAmount();
    }

    private static BigDecimal paiseToInr(long paise) {
        return BigDecimal.valueOf(paise).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private AdminRefundResponse toRefundResponse(Booking booking, Payment payment, String refundState) {
        return AdminRefundResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .bookingStatus(booking.getStatus().name())
                .paymentStatus(booking.getPaymentStatus().name())
                .paymentRecordStatus(payment.getStatus().name())
                .razorpayRefundId(payment.getRazorpayRefundId())
                .refundState(refundState)
                .refundedAmount(refundedAmountOrZero(payment))
                .totalAmount(booking.getTotalAmount())
                .build();
    }

    private void tryAssignTripSafely(Booking booking) {
        try {
            tripAssignmentService.assignIfSingleMatchingTrip(booking);
        } catch (Exception ex) {
            log.warn("Auto trip assignment skipped for reference={}: {}",
                    booking.getBookingReference(), ex.getClass().getSimpleName());
        }
    }

    private void notifyRefundSafely(Booking booking, Payment payment, BigDecimal refundAmount) {
        try {
            bookingRepository.findByIdWithDetails(booking.getId()).ifPresent(fullBooking ->
                    bookingNotificationService.onRefundProcessed(fullBooking, payment, refundAmount)
            );
        } catch (Exception ex) {
            log.warn("Refund notification failed for reference={}: {}",
                    booking.getBookingReference(), ex.getClass().getSimpleName());
        }
    }

    private void notifyPaymentSuccessSafely(Booking booking, Payment payment) {
        try {
            bookingRepository.findByIdWithDetails(booking.getId()).ifPresent(fullBooking ->
                    bookingNotificationService.onPaymentSuccess(fullBooking, payment)
            );
        } catch (Exception ex) {
            log.warn("Payment success notification failed for reference={}: {}",
                    booking.getBookingReference(), ex.getClass().getSimpleName());
        }
    }

    private void notifyPaymentOnInactiveBookingSafely(Booking booking) {
        try {
            bookingRepository.findByIdWithDetails(booking.getId()).ifPresent(
                    bookingNotificationService::onPaymentReceivedForInactiveBooking
            );
        } catch (Exception ex) {
            log.warn("Inactive-booking payment notification failed for reference={}: {}",
                    booking.getBookingReference(), ex.getClass().getSimpleName());
        }
    }

    private void notifyPaymentFailedSafely(Booking booking) {
        try {
            bookingRepository.findByIdWithDetails(booking.getId()).ifPresent(
                    bookingNotificationService::onPaymentFailed
            );
        } catch (Exception ex) {
            log.warn("Payment failed notification failed for reference={}: {}",
                    booking.getBookingReference(), ex.getClass().getSimpleName());
        }
    }

    /**
     * Marks stale CREATED payment rows as FAILED so only the latest order remains payable.
     */
    private void supersedeCreatedOrdersForBooking(Long bookingId) {
        List<Payment> created = paymentRepository.findByBookingIdAndStatus(bookingId, PaymentRecordStatus.CREATED);
        for (Payment stale : created) {
            stale.setStatus(PaymentRecordStatus.FAILED);
            paymentRepository.save(stale);
            log.info("event=payment.order.superseded bookingId={} orderId={}",
                    bookingId, stale.getRazorpayOrderId());
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
