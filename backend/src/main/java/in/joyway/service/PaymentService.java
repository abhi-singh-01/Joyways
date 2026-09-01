package in.joyway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.joyway.config.RazorpayProperties;
import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.Payment;
import in.joyway.domain.entity.PaymentWebhookEvent;
import in.joyway.domain.enums.BookingStatus;
import in.joyway.domain.enums.PaymentRecordStatus;
import in.joyway.domain.enums.PaymentStatus;
import in.joyway.domain.repository.BookingRepository;
import in.joyway.domain.repository.PaymentRepository;
import in.joyway.domain.repository.PaymentWebhookEventRepository;
import in.joyway.dto.booking.BookingReceiptDto;
import in.joyway.dto.payment.CreatePaymentOrderRequest;
import in.joyway.dto.payment.PaymentOrderResponse;
import in.joyway.dto.payment.VerifyPaymentRequest;
import in.joyway.dto.payment.VerifyPaymentResponse;
import in.joyway.exception.PaymentVerificationException;
import in.joyway.exception.ResourceNotFoundException;
import in.joyway.notification.BookingNotificationService;
import in.joyway.service.payment.RazorpayGateway;
import in.joyway.service.payment.RazorpayGatewayImpl;
import in.joyway.service.payment.RazorpayOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

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
        log.info("event=payment.webhook.received eventId={}", eventId);

        if (eventId == null || eventId.isBlank()) {
            throw new PaymentVerificationException("Webhook event id is required.");
        }

        if (!razorpayGateway.verifyWebhookSignature(payload, signature)) {
            log.warn("event=payment.webhook.failed reason=signature_invalid eventId={}", eventId);
            throw new PaymentVerificationException("Invalid webhook signature.");
        }

        if (webhookEventRepository.existsByEventId(eventId)) {
            log.info("event=payment.webhook.duplicate eventId={}", eventId);
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.path("event").asText("");
            String payloadHash = sha256(payload);

            if (webhookEventRepository.existsByPayloadHash(payloadHash)) {
                log.info("event=payment.webhook.duplicate payloadHash={}", payloadHash);
                return;
            }

            PaymentWebhookEvent webhookEvent = PaymentWebhookEvent.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .payloadHash(payloadHash)
                    .processed(false)
                    .build();
            webhookEventRepository.save(webhookEvent);

            switch (eventType) {
                case "payment.captured", "order.paid" -> handlePaymentSuccess(root);
                case "payment.failed" -> handlePaymentFailed(root);
                default -> log.info("Unhandled webhook event type: {}", eventType);
            }

            webhookEvent.setProcessed(true);
            webhookEventRepository.save(webhookEvent);
        } catch (PaymentVerificationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("event=payment.webhook.failed reason=processing_error eventId={} type={}",
                    eventId, ex.getClass().getSimpleName());
            throw new IllegalStateException("Webhook processing failed.");
        }
    }

    private void handlePaymentSuccess(JsonNode root) {
        JsonNode paymentNode = extractPaymentNode(root);
        if (paymentNode == null) {
            return;
        }

        String orderId = paymentNode.path("order_id").asText(null);
        String paymentId = paymentNode.path("id").asText(null);
        if (orderId == null || paymentId == null) {
            return;
        }

        if (paymentRepository.findByRazorpayPaymentId(paymentId).isPresent()) {
            log.info("Duplicate payment webhook ignored: paymentId={}", paymentId);
            return;
        }

        Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                .orElse(null);
        if (payment == null) {
            log.warn("Webhook payment success for unknown orderId={}", orderId);
            return;
        }

        Booking booking = bookingRepository.findByBookingReferenceForUpdate(
                payment.getBooking().getBookingReference()
        ).orElse(null);
        if (booking == null) {
            return;
        }

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            return;
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
            return;
        }

        if (!isPayable(booking.getStatus())) {
            log.warn("Captured payment for non-payable booking: reference={}, status={}, paymentId={}",
                    booking.getBookingReference(), booking.getStatus(), paymentId);
            notifyPaymentOnInactiveBookingSafely(booking);
            return;
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PAID);
        bookingRepository.save(booking);

        log.info("event=payment.webhook.confirmed bookingReference={} paymentId={}",
                booking.getBookingReference(), paymentId);

        notifyPaymentSuccessSafely(booking, payment);
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

    private void handlePaymentFailed(JsonNode root) {
        JsonNode paymentNode = extractPaymentNode(root);
        if (paymentNode == null) {
            return;
        }

        String orderId = paymentNode.path("order_id").asText(null);
        if (orderId == null) {
            return;
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
