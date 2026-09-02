package in.joyway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.joyway.config.RazorpayProperties;
import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.Customer;
import in.joyway.domain.entity.Payment;
import in.joyway.domain.entity.PaymentWebhookEvent;
import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.enums.BookingStatus;
import in.joyway.domain.enums.PaymentRecordStatus;
import in.joyway.domain.enums.PaymentStatus;
import in.joyway.domain.repository.BookingRepository;
import in.joyway.domain.repository.PaymentRepository;
import in.joyway.domain.repository.PaymentWebhookEventRepository;
import in.joyway.dto.payment.CreatePaymentOrderRequest;
import in.joyway.dto.payment.VerifyPaymentRequest;
import in.joyway.exception.AccessDeniedBookingException;
import in.joyway.exception.PaymentVerificationException;
import in.joyway.notification.BookingNotificationService;
import in.joyway.service.payment.RazorpayGateway;
import in.joyway.service.payment.RazorpayOrderResult;
import in.joyway.service.payment.RazorpayPaymentDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentWebhookEventRepository webhookEventRepository;
    @Mock
    private BookingQueryService bookingQueryService;
    @Mock
    private RazorpayGateway razorpayGateway;
    @Mock
    private RazorpayProperties razorpayProperties;
    @Mock
    private ReceiptService receiptService;
    @Mock
    private BookingNotificationService bookingNotificationService;

    @InjectMocks
    private PaymentService paymentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Booking booking;
    private Customer customer;
    private TravelPackage travelPackage;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                bookingRepository,
                paymentRepository,
                webhookEventRepository,
                bookingQueryService,
                razorpayGateway,
                razorpayProperties,
                receiptService,
                objectMapper,
                bookingNotificationService
        );

        customer = Customer.builder()
                .id(1L)
                .fullName("Test Customer")
                .email("customer@example.com")
                .mobile("9876543210")
                .build();

        travelPackage = TravelPackage.builder()
                .id(10L)
                .title("Nainital Escape")
                .destination("Nainital")
                .build();

        booking = Booking.builder()
                .id(100L)
                .bookingReference("JW-2026-00001")
                .customer(customer)
                .travelPackage(travelPackage)
                .numberOfTravellers(2)
                .totalAmount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(BookingStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
    }

    @Test
    void createOrder_createsRazorpayOrderAndPaymentRecord() {
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest();
        request.setBookingReference("JW-2026-00001");
        request.setCustomerEmail("customer@example.com");

        when(bookingQueryService.findBookingWithAccessCheck("JW-2026-00001", "customer@example.com"))
                .thenReturn(booking);
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByBookingIdAndStatus(100L, PaymentRecordStatus.CREATED))
                .thenReturn(Collections.emptyList());
        when(razorpayGateway.createOrder("JW-2026-00001", new BigDecimal("10000.00"), "INR"))
                .thenReturn(RazorpayOrderResult.builder()
                        .orderId("order_test123")
                        .amount(new BigDecimal("10000.00"))
                        .currency("INR")
                        .build());
        when(razorpayProperties.getKeyId()).thenReturn("rzp_test_key");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = paymentService.createOrder(request);

        assertThat(response.razorpayOrderId()).isEqualTo("order_test123");
        assertThat(response.razorpayKeyId()).isEqualTo("rzp_test_key");
        assertThat(response.amount()).isEqualByComparingTo("10000.00");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createOrder_rejectsAlreadyPaidBooking() {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PAID);

        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest();
        request.setBookingReference("JW-2026-00001");
        request.setCustomerEmail("customer@example.com");

        when(bookingQueryService.findBookingWithAccessCheck("JW-2026-00001", "customer@example.com"))
                .thenReturn(booking);
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    void createOrder_rejectsCancelledBooking() {
        booking.setStatus(BookingStatus.CANCELLED);

        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest();
        request.setBookingReference("JW-2026-00001");
        request.setCustomerEmail("customer@example.com");

        when(bookingQueryService.findBookingWithAccessCheck("JW-2026-00001", "customer@example.com"))
                .thenReturn(booking);
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not eligible");
        verify(razorpayGateway, never()).createOrder(any(), any(), any());
    }

    @Test
    void verifyPayment_rejectsCancelledBookingAndLeavesItCancelled() {
        booking.setStatus(BookingStatus.CANCELLED);

        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .razorpayOrderId("order_test123")
                .amount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CREATED)
                .build();

        VerifyPaymentRequest request = new VerifyPaymentRequest();
        request.setBookingReference("JW-2026-00001");
        request.setCustomerEmail("customer@example.com");
        request.setRazorpayOrderId("order_test123");
        request.setRazorpayPaymentId("pay_test456");
        request.setRazorpaySignature("valid_signature");

        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(bookingQueryService.findBookingWithAccessCheck("JW-2026-00001", "customer@example.com"))
                .thenReturn(booking);
        when(paymentRepository.findByRazorpayOrderId("order_test123"))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.verifyPayment(request))
                .isInstanceOf(PaymentVerificationException.class);

        // Seats were already released on cancellation, so the booking must not be resold.
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void webhook_capturesPaymentButDoesNotConfirmCancelledBooking() {
        booking.setStatus(BookingStatus.CANCELLED);

        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .razorpayOrderId("order_test123")
                .amount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CREATED)
                .build();

        String payload = """
                {"event":"payment.captured","payload":{"payment":{"entity":
                {"id":"pay_test456","order_id":"order_test123","amount":1000000}}}}""";

        when(razorpayGateway.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(webhookEventRepository.findByEventIdForUpdate("evt_2")).thenReturn(Optional.empty());
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByRazorpayPaymentId("pay_test456")).thenReturn(Optional.empty());
        when(paymentRepository.findByRazorpayOrderId("order_test123")).thenReturn(Optional.of(payment));
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processWebhook(payload, "sig", "evt_2");

        // Money was captured, so the payment record is retained for reconciliation...
        assertThat(payment.getStatus()).isEqualTo(PaymentRecordStatus.CAPTURED);
        assertThat(payment.getRazorpayPaymentId()).isEqualTo("pay_test456");
        // ...but the cancelled booking must not be revived.
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void webhook_confirmsPendingBooking() {
        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .razorpayOrderId("order_test123")
                .amount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CREATED)
                .build();

        String payload = """
                {"event":"payment.captured","payload":{"payment":{"entity":
                {"id":"pay_test456","order_id":"order_test123","amount":1000000}}}}""";

        when(razorpayGateway.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(webhookEventRepository.findByEventIdForUpdate("evt_3")).thenReturn(Optional.empty());
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByRazorpayPaymentId("pay_test456")).thenReturn(Optional.empty());
        when(paymentRepository.findByRazorpayOrderId("order_test123")).thenReturn(Optional.of(payment));
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processWebhook(payload, "sig", "evt_3");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void webhook_rejectsInvalidSignature() {
        when(razorpayGateway.verifyWebhookSignature("{}", "bad")).thenReturn(false);

        assertThatThrownBy(() -> paymentService.processWebhook("{}", "bad", "evt_4"))
                .isInstanceOf(PaymentVerificationException.class);

        verify(webhookEventRepository, never()).save(any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processWebhook_rejectsMissingEventId() {
        assertThatThrownBy(() -> paymentService.processWebhook("{}", "sig", " "))
                .isInstanceOf(PaymentVerificationException.class);
        verify(razorpayGateway, never()).verifyWebhookSignature(any(), any());
    }

    @Test
    void verifyPayment_confirmsBookingOnValidSignature() {
        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .razorpayOrderId("order_test123")
                .amount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CREATED)
                .build();

        VerifyPaymentRequest request = new VerifyPaymentRequest();
        request.setBookingReference("JW-2026-00001");
        request.setCustomerEmail("customer@example.com");
        request.setRazorpayOrderId("order_test123");
        request.setRazorpayPaymentId("pay_test456");
        request.setRazorpaySignature("valid_signature");

        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(bookingQueryService.findBookingWithAccessCheck("JW-2026-00001", "customer@example.com"))
                .thenReturn(booking);
        when(paymentRepository.findByRazorpayOrderId("order_test123"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.findByRazorpayPaymentId("pay_test456"))
                .thenReturn(Optional.empty());
        when(razorpayGateway.verifyPaymentSignature("order_test123", "pay_test456", "valid_signature"))
                .thenReturn(true);
        when(razorpayGateway.fetchPayment("pay_test456"))
                .thenReturn(Optional.of(new RazorpayPaymentDetails(
                        "pay_test456", "order_test123", 1_000_000L, "captured")));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(receiptService.buildReceipt(any(), any())).thenReturn(null);

        var response = paymentService.verifyPayment(request);

        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.paymentStatus()).isEqualTo("PAID");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void verifyPayment_rejectsInvalidSignature() {
        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .razorpayOrderId("order_test123")
                .amount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CREATED)
                .build();

        VerifyPaymentRequest request = new VerifyPaymentRequest();
        request.setBookingReference("JW-2026-00001");
        request.setCustomerEmail("customer@example.com");
        request.setRazorpayOrderId("order_test123");
        request.setRazorpayPaymentId("pay_test456");
        request.setRazorpaySignature("bad_signature");

        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(bookingQueryService.findBookingWithAccessCheck("JW-2026-00001", "customer@example.com"))
                .thenReturn(booking);
        when(paymentRepository.findByRazorpayOrderId("order_test123"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.findByRazorpayPaymentId("pay_test456"))
                .thenReturn(Optional.empty());
        when(razorpayGateway.verifyPaymentSignature("order_test123", "pay_test456", "bad_signature"))
                .thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> paymentService.verifyPayment(request))
                .isInstanceOf(PaymentVerificationException.class);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void processWebhook_skipsOnlyWhenAlreadyProcessed() {
        String payload = "{}";
        PaymentWebhookEvent processedEvent = PaymentWebhookEvent.builder()
                .eventId("evt_1")
                .eventType("payment.captured")
                .payloadHash(sha256(payload))
                .processed(true)
                .build();

        when(razorpayGateway.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(webhookEventRepository.findByEventIdForUpdate("evt_1"))
                .thenReturn(Optional.of(processedEvent));

        paymentService.processWebhook(payload, "sig", "evt_1");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(webhookEventRepository, never()).save(any());
    }

    @Test
    void processWebhook_retriesUnprocessedEvent() {
        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .razorpayOrderId("order_test123")
                .amount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CREATED)
                .build();

        String payload = """
                {"event":"payment.captured","payload":{"payment":{"entity":
                {"id":"pay_test456","order_id":"order_test123","amount":1000000}}}}""";

        PaymentWebhookEvent unprocessedEvent = PaymentWebhookEvent.builder()
                .eventId("evt_retry")
                .eventType("payment.captured")
                .payloadHash(sha256(payload))
                .processed(false)
                .build();

        when(razorpayGateway.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(webhookEventRepository.findByEventIdForUpdate("evt_retry"))
                .thenReturn(Optional.of(unprocessedEvent));
        when(paymentRepository.findByRazorpayPaymentId("pay_test456")).thenReturn(Optional.empty());
        when(paymentRepository.findByRazorpayOrderId("order_test123")).thenReturn(Optional.of(payment));
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processWebhook(payload, "sig", "evt_retry");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(unprocessedEvent.getProcessed()).isTrue();
        verify(webhookEventRepository).save(unprocessedEvent);
    }

    @Test
    void processWebhook_firstAttemptFailureDoesNotMarkProcessed() {
        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .razorpayOrderId("order_test123")
                .amount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CREATED)
                .build();

        String payload = """
                {"event":"payment.captured","payload":{"payment":{"entity":
                {"id":"pay_test456","order_id":"order_test123","amount":1000000}}}}""";

        when(razorpayGateway.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(webhookEventRepository.findByEventIdForUpdate("evt_fail")).thenReturn(Optional.empty());
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByRazorpayPaymentId("pay_test456")).thenReturn(Optional.empty());
        when(paymentRepository.findByRazorpayOrderId("order_test123")).thenReturn(Optional.of(payment));
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class)))
                .thenThrow(new IllegalStateException("simulated failure"));

        assertThatThrownBy(() -> paymentService.processWebhook(payload, "sig", "evt_fail"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Webhook processing failed");
    }

    @Test
    void processWebhook_duplicateSuccessfulDeliveryDoesNotReconfirmBooking() {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PAID);

        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .razorpayOrderId("order_test123")
                .razorpayPaymentId("pay_test456")
                .amount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CAPTURED)
                .build();

        String payload = """
                {"event":"payment.captured","payload":{"payment":{"entity":
                {"id":"pay_test456","order_id":"order_test123","amount":1000000}}}}""";

        PaymentWebhookEvent processedEvent = PaymentWebhookEvent.builder()
                .eventId("evt_dup")
                .eventType("payment.captured")
                .payloadHash(sha256(payload))
                .processed(true)
                .build();

        when(razorpayGateway.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(webhookEventRepository.findByEventIdForUpdate("evt_dup"))
                .thenReturn(Optional.of(processedEvent));

        paymentService.processWebhook(payload, "sig", "evt_dup");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(bookingNotificationService, never()).onPaymentSuccess(any(), any());
    }

    @Test
    void processWebhook_acknowledgesDuplicatePaymentId() {
        Payment existingPayment = Payment.builder()
                .id(99L)
                .booking(booking)
                .razorpayOrderId("order_other")
                .razorpayPaymentId("pay_known")
                .amount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CAPTURED)
                .build();

        String payload = """
                {"event":"payment.captured","payload":{"payment":{"entity":
                {"id":"pay_known","order_id":"order_test123","amount":1000000}}}}""";

        when(razorpayGateway.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(webhookEventRepository.findByEventIdForUpdate("evt_dup_payment")).thenReturn(Optional.empty());
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByRazorpayPaymentId("pay_known"))
                .thenReturn(Optional.of(existingPayment));

        paymentService.processWebhook(payload, "sig", "evt_dup_payment");

        verify(paymentRepository, never()).findByRazorpayOrderId(any());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(bookingRepository, never()).save(any(Booking.class));

        ArgumentCaptor<PaymentWebhookEvent> captor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
        verify(webhookEventRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().getLast().getProcessed()).isTrue();
    }

    @Test
    void processWebhook_alreadyPaidBookingDoesNotSendSuccessNotification() {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PAID);

        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .razorpayOrderId("order_test123")
                .amount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CREATED)
                .build();

        String payload = """
                {"event":"payment.captured","payload":{"payment":{"entity":
                {"id":"pay_test456","order_id":"order_test123","amount":1000000}}}}""";

        when(razorpayGateway.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(webhookEventRepository.findByEventIdForUpdate("evt_paid")).thenReturn(Optional.empty());
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByRazorpayPaymentId("pay_test456")).thenReturn(Optional.empty());
        when(paymentRepository.findByRazorpayOrderId("order_test123")).thenReturn(Optional.of(payment));
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processWebhook(payload, "sig", "evt_paid");

        verify(bookingRepository, never()).save(any(Booking.class));
        verify(bookingNotificationService, never()).onPaymentSuccess(any(), any());
    }

    @Test
    void processWebhook_rejectsCapturedAmountMismatch() {
        Booking otherBooking = Booking.builder()
                .id(200L)
                .bookingReference("JW-2026-99999")
                .customer(customer)
                .travelPackage(travelPackage)
                .totalAmount(new BigDecimal("5000.00"))
                .currency("INR")
                .status(BookingStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        Payment payment = Payment.builder()
                .id(1L)
                .booking(otherBooking)
                .razorpayOrderId("order_other")
                .amount(new BigDecimal("5000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CREATED)
                .build();

        String payload = """
                {"event":"payment.captured","payload":{"payment":{"entity":
                {"id":"pay_test456","order_id":"order_other","amount":1000000}}}}""";

        when(razorpayGateway.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(webhookEventRepository.findByEventIdForUpdate("evt_mismatch")).thenReturn(Optional.empty());
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByRazorpayPaymentId("pay_test456")).thenReturn(Optional.empty());
        when(paymentRepository.findByRazorpayOrderId("order_other")).thenReturn(Optional.of(payment));
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-99999"))
                .thenReturn(Optional.of(otherBooking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.findByIdWithDetails(200L)).thenReturn(Optional.of(otherBooking));

        paymentService.processWebhook(payload, "sig", "evt_mismatch");

        assertThat(otherBooking.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getStatus()).isEqualTo(PaymentRecordStatus.CAPTURED);
        verify(bookingNotificationService).onPaymentReceivedForInactiveBooking(otherBooking);
        verify(bookingNotificationService, never()).onPaymentSuccess(any(), any());
    }

    @Test
    void processWebhook_acknowledgesUnknownOrderWithoutConfirming() {
        String payload = """
                {"event":"payment.captured","payload":{"payment":{"entity":
                {"id":"pay_unknown","order_id":"order_missing","amount":1000000}}}}""";

        when(razorpayGateway.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(webhookEventRepository.findByEventIdForUpdate("evt_unknown")).thenReturn(Optional.empty());
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByRazorpayPaymentId("pay_unknown")).thenReturn(Optional.empty());
        when(paymentRepository.findByRazorpayOrderId("order_missing")).thenReturn(Optional.empty());

        paymentService.processWebhook(payload, "sig", "evt_unknown");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(bookingRepository, never()).save(any(Booking.class));

        ArgumentCaptor<PaymentWebhookEvent> captor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
        verify(webhookEventRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().getLast().getProcessed()).isTrue();
    }

    @Test
    void processWebhook_acknowledgesMissingPaymentPayload() {
        String payload = """
                {"event":"payment.captured","payload":{}}""";

        when(razorpayGateway.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(webhookEventRepository.findByEventIdForUpdate("evt_missing_payload")).thenReturn(Optional.empty());
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processWebhook(payload, "sig", "evt_missing_payload");

        verify(paymentRepository, never()).save(any(Payment.class));
        ArgumentCaptor<PaymentWebhookEvent> captor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
        verify(webhookEventRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().getLast().getProcessed()).isTrue();
    }

    @Test
    void processWebhook_acknowledgesMalformedPaymentCapturedPayload() {
        String payload = """
                {"event":"payment.captured","payload":{"payment":{"entity":
                {"id":"pay_malformed","amount":1000000}}}}""";

        when(razorpayGateway.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(webhookEventRepository.findByEventIdForUpdate("evt_malformed")).thenReturn(Optional.empty());
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processWebhook(payload, "sig", "evt_malformed");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentRepository, never()).findByRazorpayOrderId(any());
        ArgumentCaptor<PaymentWebhookEvent> captor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
        verify(webhookEventRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().getLast().getProcessed()).isTrue();
    }

    @Test
    void processWebhook_retriesWhenBookingNotFound() {
        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .razorpayOrderId("order_test123")
                .amount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CREATED)
                .build();

        String payload = """
                {"event":"payment.captured","payload":{"payment":{"entity":
                {"id":"pay_test456","order_id":"order_test123","amount":1000000}}}}""";

        PaymentWebhookEvent unprocessedEvent = PaymentWebhookEvent.builder()
                .eventId("evt_no_booking")
                .eventType("payment.captured")
                .payloadHash(sha256(payload))
                .processed(false)
                .build();

        when(razorpayGateway.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(webhookEventRepository.findByEventIdForUpdate("evt_no_booking")).thenReturn(Optional.empty());
        when(webhookEventRepository.save(any())).thenAnswer(inv -> unprocessedEvent);
        when(paymentRepository.findByRazorpayPaymentId("pay_test456")).thenReturn(Optional.empty());
        when(paymentRepository.findByRazorpayOrderId("order_test123")).thenReturn(Optional.of(payment));
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processWebhook(payload, "sig", "evt_no_booking"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Webhook processing incomplete");

        assertThat(unprocessedEvent.getProcessed()).isFalse();
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void processWebhook_concurrentDuplicateUsesProcessedFlag() {
        String payload = "{}";
        PaymentWebhookEvent processedEvent = PaymentWebhookEvent.builder()
                .eventId("evt_concurrent")
                .eventType("payment.captured")
                .payloadHash(sha256(payload))
                .processed(true)
                .build();

        when(razorpayGateway.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(webhookEventRepository.findByEventIdForUpdate("evt_concurrent"))
                .thenReturn(Optional.of(processedEvent));

        paymentService.processWebhook(payload, "sig", "evt_concurrent");
        paymentService.processWebhook(payload, "sig", "evt_concurrent");

        verify(webhookEventRepository, times(2)).findByEventIdForUpdate("evt_concurrent");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void createOrder_reusesValidExistingCreatedOrder() {
        Payment existing = Payment.builder()
                .id(5L)
                .booking(booking)
                .razorpayOrderId("order_existing")
                .amount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CREATED)
                .build();

        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest();
        request.setBookingReference("JW-2026-00001");
        request.setCustomerEmail("customer@example.com");

        when(bookingQueryService.findBookingWithAccessCheck("JW-2026-00001", "customer@example.com"))
                .thenReturn(booking);
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.of(existing));
        when(razorpayProperties.getKeyId()).thenReturn("rzp_test_key");

        var response = paymentService.createOrder(request);

        assertThat(response.razorpayOrderId()).isEqualTo("order_existing");
        verify(razorpayGateway, never()).createOrder(any(), any(), any());
        verify(paymentRepository, never()).findByBookingIdAndStatus(any(), any());
    }

    @Test
    void createOrder_supersedesStaleCreatedOrderBeforeCreatingNew() {
        Payment stale = Payment.builder()
                .id(5L)
                .booking(booking)
                .razorpayOrderId("order_stale")
                .amount(new BigDecimal("8000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CREATED)
                .build();

        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest();
        request.setBookingReference("JW-2026-00001");
        request.setCustomerEmail("customer@example.com");

        when(bookingQueryService.findBookingWithAccessCheck("JW-2026-00001", "customer@example.com"))
                .thenReturn(booking);
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.of(stale));
        when(paymentRepository.findByBookingIdAndStatus(100L, PaymentRecordStatus.CREATED))
                .thenReturn(List.of(stale));
        when(razorpayGateway.createOrder("JW-2026-00001", new BigDecimal("10000.00"), "INR"))
                .thenReturn(RazorpayOrderResult.builder()
                        .orderId("order_new")
                        .amount(new BigDecimal("10000.00"))
                        .currency("INR")
                        .build());
        when(razorpayProperties.getKeyId()).thenReturn("rzp_test_key");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.createOrder(request);

        assertThat(stale.getStatus()).isEqualTo(PaymentRecordStatus.FAILED);
        verify(razorpayGateway).createOrder("JW-2026-00001", new BigDecimal("10000.00"), "INR");
    }

    @Test
    void verifyPayment_rejectsRazorpayAmountMismatch() {
        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .razorpayOrderId("order_test123")
                .amount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CREATED)
                .build();

        VerifyPaymentRequest request = new VerifyPaymentRequest();
        request.setBookingReference("JW-2026-00001");
        request.setCustomerEmail("customer@example.com");
        request.setRazorpayOrderId("order_test123");
        request.setRazorpayPaymentId("pay_test456");
        request.setRazorpaySignature("valid_signature");

        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(bookingQueryService.findBookingWithAccessCheck("JW-2026-00001", "customer@example.com"))
                .thenReturn(booking);
        when(paymentRepository.findByRazorpayOrderId("order_test123"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.findByRazorpayPaymentId("pay_test456"))
                .thenReturn(Optional.empty());
        when(razorpayGateway.verifyPaymentSignature("order_test123", "pay_test456", "valid_signature"))
                .thenReturn(true);
        when(razorpayGateway.fetchPayment("pay_test456"))
                .thenReturn(Optional.of(new RazorpayPaymentDetails(
                        "pay_test456", "order_test123", 500_000L, "captured")));

        assertThatThrownBy(() -> paymentService.verifyPayment(request))
                .isInstanceOf(PaymentVerificationException.class)
                .hasMessageContaining("amount");
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void verifyPayment_rejectsWrongRazorpayOrderId() {
        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .razorpayOrderId("order_test123")
                .amount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CREATED)
                .build();

        VerifyPaymentRequest request = new VerifyPaymentRequest();
        request.setBookingReference("JW-2026-00001");
        request.setCustomerEmail("customer@example.com");
        request.setRazorpayOrderId("order_test123");
        request.setRazorpayPaymentId("pay_test456");
        request.setRazorpaySignature("valid_signature");

        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(bookingQueryService.findBookingWithAccessCheck("JW-2026-00001", "customer@example.com"))
                .thenReturn(booking);
        when(paymentRepository.findByRazorpayOrderId("order_test123"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.findByRazorpayPaymentId("pay_test456"))
                .thenReturn(Optional.empty());
        when(razorpayGateway.verifyPaymentSignature("order_test123", "pay_test456", "valid_signature"))
                .thenReturn(true);
        when(razorpayGateway.fetchPayment("pay_test456"))
                .thenReturn(Optional.of(new RazorpayPaymentDetails(
                        "pay_test456", "order_other", 1_000_000L, "captured")));

        assertThatThrownBy(() -> paymentService.verifyPayment(request))
                .isInstanceOf(PaymentVerificationException.class)
                .hasMessageContaining("order");
    }

    @Test
    void processWebhook_orderPaidIsInformationalOnly() {
        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .razorpayOrderId("order_test123")
                .amount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CREATED)
                .build();

        String payload = """
                {"event":"order.paid","payload":{"order":{"entity":
                {"id":"order_test123","amount":1000000,"payments":["pay_test456"]}}}}""";

        when(razorpayGateway.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(webhookEventRepository.findByEventIdForUpdate("evt_order_paid")).thenReturn(Optional.empty());
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processWebhook(payload, "sig", "evt_order_paid");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(bookingNotificationService, never()).onPaymentSuccess(any(), any());
    }

    @Test
    void processWebhook_orderPaidDuplicateDoesNotReconfirm() {
        String payload = """
                {"event":"order.paid","payload":{"order":{"entity":
                {"id":"order_test123","amount":1000000,"payments":["pay_test456"]}}}}""";

        PaymentWebhookEvent processedEvent = PaymentWebhookEvent.builder()
                .eventId("evt_order_dup")
                .eventType("order.paid")
                .payloadHash(sha256(payload))
                .processed(true)
                .build();

        when(razorpayGateway.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(webhookEventRepository.findByEventIdForUpdate("evt_order_dup"))
                .thenReturn(Optional.of(processedEvent));

        paymentService.processWebhook(payload, "sig", "evt_order_dup");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void verifyPayment_idempotentWhenAlreadyPaidWithSamePaymentId() {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PAID);

        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .razorpayOrderId("order_test123")
                .razorpayPaymentId("pay_test456")
                .amount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CAPTURED)
                .build();

        VerifyPaymentRequest request = new VerifyPaymentRequest();
        request.setBookingReference("JW-2026-00001");
        request.setCustomerEmail("customer@example.com");
        request.setRazorpayOrderId("order_test123");
        request.setRazorpayPaymentId("pay_test456");
        request.setRazorpaySignature("valid_signature");

        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(bookingQueryService.findBookingWithAccessCheck("JW-2026-00001", "customer@example.com"))
                .thenReturn(booking);
        when(paymentRepository.findByRazorpayOrderId("order_test123"))
                .thenReturn(Optional.of(payment));
        when(receiptService.buildReceipt(any(), any())).thenReturn(null);

        paymentService.verifyPayment(request);

        verify(razorpayGateway, never()).fetchPayment(any());
        verify(bookingNotificationService, never()).onPaymentSuccess(any(), any());
    }
}
